/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core.utils;

import com.google.gson.Gson;
import io.ballerina.flowmodelgenerator.core.model.Function;
import io.ballerina.flowmodelgenerator.core.model.Member;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.TypeData;
import io.ballerina.modelgenerator.commons.CommonUtils;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Code snippet generator.
 *
 * @since 2.0.0
 */
public class SourceCodeGenerator {

    private final Gson gson = new Gson();
    private final Map<String, String> imports = new HashMap<>();

    private static final String LS = System.lineSeparator();

    public String generateCodeSnippetForType(TypeData typeData) {
        NodeKind nodeKind = typeData.codedata().node();
        return switch (nodeKind) {
            case SERVICE_DECLARATION, CLASS -> "";  // TODO: Implement this
            case ENUM -> generateEnumCodeSnippet(typeData);
            default -> generateTypeDefCodeSnippet(typeData);
        };
    }

    public String generateGraphqlClassType(TypeData typeData) {
        NodeKind nodeKind = typeData.codedata().node();
        if (nodeKind != NodeKind.CLASS) {
            return "";
        }

        StringBuilder fieldBuilder = new StringBuilder();
        for (Member member: typeData.members()) {
            fieldBuilder
                    .append(generateDocs(member.docs(), "\t"))
                    .append(generateMember(member, true))
                    .append(";");
        }

        // Build the resource functions.
        StringBuilder resourceFunctions = new StringBuilder();
        if (typeData.functions() != null) {
            for (Function function : typeData.functions()) {
                resourceFunctions.append(generateResourceFunction(function));
            }
        }

        String template = "%nservice class %s {%s%n\tfunction init() {%n\t}%s%n}";

        return template.formatted(
                typeData.name(),
                fieldBuilder.toString(),
                resourceFunctions.toString()
        );
    }

    public Map<String, String> getImports () {
        return this.imports;
    }

    private String generateEnumCodeSnippet(TypeData typeData) {
        String docs = generateDocs(typeData.metadata().description(), "");

        // Build the enum values.
        StringBuilder enumValues = new StringBuilder();
        for (int i = 0; i < typeData.members().size(); i++) {
            Member member = typeData.members().get(i);
            enumValues.append(LS).append("\t").append(member.name());
            if (member.defaultValue() != null && !member.defaultValue().isEmpty()) {
                enumValues.append(" = ").append(member.defaultValue());
            }
            if (i < typeData.members().size() - 1) {
                enumValues.append(",");
            }
        }

        String template = "%senum %s {%s%n}%n";

        return template.formatted(docs, typeData.name(), enumValues.toString());
    }

    private String generateTypeDefCodeSnippet(TypeData typeData) {
        String docs = generateDocs(typeData.metadata().description(), "");
        String typeDescriptor = generateTypeDescriptor(typeData);

        String template = "%stype %s %s;";

        return template.formatted(docs, typeData.name(), typeDescriptor);
    }

    private String generateTypeDescriptor(Object typeDescriptor) {
        if (typeDescriptor instanceof String) { // Type reference or in-line type as string
            return (String) typeDescriptor;
        }

        TypeData typeData = toTypeData(typeDescriptor);

        return switch (typeData.codedata().node()) {
            case RECORD -> generateRecordTypeDescriptor(typeData);
            case ARRAY -> generateArrayTypeDescriptor(typeData);
            case MAP -> generateMapTypeDescriptor(typeData);
            case STREAM -> generateStreamTypeDescriptor(typeData);
            case FUTURE -> generateFutureTypeDescriptor(typeData);
            case TYPEDESC -> generateTypedescTypeDescriptor(typeData);
            case ERROR -> generateErrorTypeDescriptor(typeData);
            case UNION -> generateUnionTypeDescriptor(typeData);
            case INTERSECTION -> generateIntersectionTypeDescriptor(typeData);
            case OBJECT -> generateObjectTypeDescriptor(typeData);
            case TABLE -> generateTableTypeDescriptor(typeData);
            case TUPLE -> generateTupleTypeDescriptor(typeData);
            default -> throw new UnsupportedOperationException("Unsupported type descriptor: " + typeDescriptor);
        };
    }

    private String generateObjectTypeDescriptor(TypeData typeData) {
        StringBuilder fieldsBuilder = new StringBuilder();
        for (Member member : typeData.members()) {
            fieldsBuilder.append(generateFieldMember(member, false));
        }

        // TODO: Generate functions if needed.

        String objectTemplate = "object {%s%n}";

        return objectTemplate.formatted(fieldsBuilder.toString());
    }

    private String generateRecordTypeDescriptor(TypeData typeData) {
        // Build the inclusions.
        StringBuilder inclusionsBuilder = new StringBuilder();
        typeData.includes().forEach(include -> inclusionsBuilder
                .append(LS)
                .append("\t*")
                .append(include)
                .append(";"));

        // Build the fields.
        StringBuilder fieldsBuilder = new StringBuilder();
        for (Member member : typeData.members()) {
            fieldsBuilder.append(generateFieldMember(member, true));
        }

        // Build the rest field (if present).
        String restField = "";
        // TODO: Add support for rest field with closed records
//        if (typeData.restMember() != null) {
//            String typeDescriptor = generateTypeDescriptor(typeData.restMember().type());
//            restField = LS + "\t" + typeDescriptor + " ...;";
//        }

        // The template assumes that the dynamic parts already include their needed newlines and indentation.
        String template = "record {|%s%s%s%n|}";
        if (typeData.allowAdditionalFields()) {
            template = "record {%s%s%s%n}";
        }

        return template.formatted(
                inclusionsBuilder.toString(),
                fieldsBuilder.toString(),
                restField
        );
    }

    private String generateFieldMember(Member member, boolean withDefaultValue) {
        StringBuilder stringBuilder = new StringBuilder();
        String docs = generateDocs(member.docs(), "\t");
        stringBuilder
                .append(docs)
                .append("\t")
                .append(generateMember(member, withDefaultValue))
                .append(";");
        return stringBuilder.toString();
    }

    private String generateTableTypeDescriptor(TypeData typeData) {
        if (typeData.members().isEmpty()) {
            return "table";
        }

        // Build the base table type descriptor.
        String rowType = generateTypeFromMember(typeData.members().getFirst());

        // Build the key type constraint if available.
        String keyInformation = "";
        if (typeData.members().size() > 1) {
            keyInformation = " key<" + generateTypeDescriptor(typeData.members().get(1).type()) + ">";
        }

        // TODO: key specifier is not yet supported

        String template = "table<%s>%s";
        return template.formatted(rowType, keyInformation);
    }

    private String generateIntersectionTypeDescriptor(TypeData typeData) {
        StringBuilder stringBuilder = new StringBuilder();
        if (typeData.members().size() <= 1) {
            return "";
        }
        for (int i = 0; i < typeData.members().size(); i++) {
            Member member = typeData.members().get(i);
            if (member.type() instanceof TypeData) {
                if (((TypeData) member.type()).codedata().node() == NodeKind.INTERSECTION) {
                    stringBuilder
                            .append("(")
                            .append(generateTypeFromMember(member))
                            .append(")");
                } else {
                    stringBuilder.append(generateTypeFromMember(member));
                }
            } else {
                stringBuilder.append(generateTypeFromMember(member));
            }
            if (i < typeData.members().size() - 1) {
                stringBuilder.append(" & ");
            }
        }
        return stringBuilder.toString();
    }

    private String generateTupleTypeDescriptor(TypeData typeData) {
        StringBuilder stringBuilder = new StringBuilder();
        // If there are no members, output an empty tuple.
        if (typeData.members().isEmpty()) {
            stringBuilder.append("[]");
            return "";
        }

        // Build the dynamic list of tuple elements.
        StringJoiner joiner = new StringJoiner(", ");
        for (Member member : typeData.members()) {
            joiner.add(generateTypeFromMember(member));
        }

        String template = "[%s]";
        stringBuilder.append(template.formatted(joiner.toString()));
        return stringBuilder.toString();
    }

    private String generateUnionTypeDescriptor(TypeData typeData) {
        StringBuilder stringBuilder = new StringBuilder();
        if (typeData.members().size() <= 1) {
            return "";
        }
        for (int i = 0; i < typeData.members().size(); i++) {
            Member member = typeData.members().get(i);
            if (member.type() instanceof TypeData) {
                if (((TypeData) member.type()).codedata().node() == NodeKind.UNION) {
                    stringBuilder
                            .append("(")
                            .append(generateTypeFromMember(member))
                            .append(")");
                } else {
                    stringBuilder.append(generateTypeFromMember(member));
                }
            } else {
                stringBuilder.append(generateTypeFromMember(member));
            }
            if (i < typeData.members().size() - 1) {
                stringBuilder.append("|");
            }
        }
        return stringBuilder.toString();
    }

    private String generateErrorTypeDescriptor(TypeData typeData) {
        if (typeData.members().size() == 1) {
            return "error<" + generateTypeFromMember(typeData.members().getFirst()) + ">";
        }
        return "error";
    }

    private String generateTypedescTypeDescriptor(TypeData typeData) {
        if (typeData.members().size() == 1) {
            return "typedesc<" + generateTypeFromMember(typeData.members().getFirst()) + ">";
        }
        return "typedesc<>";
    }

    private String generateFutureTypeDescriptor(TypeData typeData) {
        if (typeData.members().size() == 1) {
            return "future<" + generateTypeFromMember(typeData.members().getFirst()) + ">";
        }
        return "future<>";
    }

    private String generateStreamTypeDescriptor(TypeData typeData) {
        if (typeData.members().size() == 1) {
            return "stream<" + generateTypeFromMember(typeData.members().getFirst()) + ">";
        }
        return "stream<>";
    }

    private String generateMapTypeDescriptor(TypeData typeData) {
        if (typeData.members().size() == 1) {
            return "map<" + generateTypeFromMember(typeData.members().getFirst()) + ">";
        }
        return "map<>";
    }

    private String generateArrayTypeDescriptor(TypeData typeData) {
        Property arraySizeProperty = typeData.properties().get(Property.ARRAY_SIZE);
        String arraySize = "";
        if (arraySizeProperty != null) {
            arraySize =  arraySizeProperty.value().toString();
        }

        if (typeData.members().size() != 1) {
            return "[" + arraySize + "]";
        }

        Member typeMember = typeData.members().getFirst();
        Object type = typeMember.type();
        String transformed = generateTypeFromMember(typeMember);

        if (!(type instanceof String)) {
            NodeKind nodeKind = toTypeData(type).codedata().node();
            // Add parenthesis to union and intersection types
            if (nodeKind == NodeKind.UNION || nodeKind == NodeKind.INTERSECTION) {
                transformed = "(" + transformed + ")";
            }
        }

        return transformed + "[" + arraySize + "]";
    }

    private String generateTypeFromMember(Member member) {
        // Add the imports
        if (Objects.nonNull(member.imports())) {
            member.imports().forEach(this.imports::putIfAbsent);
        }

        // Generate the type descriptor
        return generateTypeDescriptor(member.type());
    }

    private String generateDocs(String docs, String indent) {
        return (docs != null && !docs.isEmpty())
                ? LS + indent + CommonUtils.convertToBalDocs(docs)
                : LS;
    }

    private String generateMember(Member member, boolean withDefaultValue) {
        String typeDescriptor = generateTypeFromMember(member);
        String template = "%s %s%s"; // <type descriptor> <identifier> [= <default value>]

        String fieldName = CommonUtil.escapeReservedKeyword(member.name());
        if (member.optional()) {
            fieldName = fieldName + "?";
        }

        return template.formatted(typeDescriptor,
                fieldName,
                (withDefaultValue && member.defaultValue() != null && !member.defaultValue().isEmpty()) ?
                        " = " + member.defaultValue() : "");
    }

    private String generateResourceFunction(Function function) {
        // Add the imports
        if (Objects.nonNull(function.imports())) {
            function.imports().forEach(this.imports::putIfAbsent);
        }

        String docs = generateDocs(function.description(), "\t");

        StringJoiner paramJoiner = new StringJoiner(", ");
        for (Member param : function.parameters()) {
            String genParam = generateMember(param, true);
            paramJoiner.add(genParam);
        }

        String template = "%s\tresource function %s %s(%s) returns %s {" +
                "%n\t\tdo {" +
                "%n\t\t\tpanic error(\"Unimplemented function\");" +
                "%n\t\t} on fail error err {" +
                "%n\t\t\t//handle error" +
                "%n\t\t\tpanic error(\"Unhandled error\");" +
                "%n\t\t}" +
                "%n\t}";

        return template.formatted(
                docs,
                function.accessor(),
                function.name(),
                paramJoiner.toString(),
                generateTypeDescriptor(function.returnType()),
                function.name()
        );
    }

    private TypeData toTypeData(Object typeDescAsObject) {
        TypeData typeData;
        if (typeDescAsObject instanceof Map) {
            String json = gson.toJson(typeDescAsObject);
            typeData = gson.fromJson(json, TypeData.class);
        } else {
            typeData = (TypeData) typeDescAsObject;
        }
        return typeData;
    }
}
