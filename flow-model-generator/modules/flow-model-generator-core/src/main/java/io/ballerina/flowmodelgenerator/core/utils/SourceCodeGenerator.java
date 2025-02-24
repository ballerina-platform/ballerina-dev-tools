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
import io.ballerina.flowmodelgenerator.core.model.TypeData;
import io.ballerina.modelgenerator.commons.CommonUtils;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Code snippet generator.
 *
 * @since 2.0.0
 */
public class SourceCodeGenerator {

    private final Gson gson = new Gson();

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

        // Build inferred fields (from functions)
        StringBuilder inferredFields = new StringBuilder();
        for (Function function : typeData.functions()) {
            generateInferredGraphqlClassField(function, inferredFields);
        }

        // Build the "init" function parameters and body.
        // Infer the parameters and body from the functions.
        StringBuilder initParams = new StringBuilder();
        StringBuilder initBody = new StringBuilder();
        for (int i = 0; i < typeData.functions().size(); i++) {
            Function function = typeData.functions().get(i);
            // Append the return type and function name as a parameter.
            generateTypeDescriptor(function.returnType(), initParams);
            initParams.append(" ").append(function.name());
            if (i < typeData.functions().size() - 1) {
                initParams.append(", ");
            }
            // Build the init function body: "self.<function-name> = <function-name>;"
            // TODO: Add do-on-fail block after fixing https://github.com/ballerina-platform/ballerina-lang/issues/43817
            initBody.append(LS).append("\t\tself.")
                    .append(function.name())
                    .append(" = ")
                    .append(function.name())
                    .append(";");
        }

        // Build the resource functions.
        StringBuilder resourceFunctions = new StringBuilder();
        for (Function function : typeData.functions()) {
            generateResourceFunction(function, resourceFunctions);
        }

        String template = "%nservice class %s {%s%n\tfunction init(%s) {%s%n\t}%s%n}";

        return template.formatted(
                typeData.name(),
                inferredFields.toString(),
                initParams.toString(),
                initBody.toString(),
                resourceFunctions.toString()
        );
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

        // Generate the type descriptor into a StringBuilder.
        StringBuilder typeDescriptorBuilder = new StringBuilder();
        generateTypeDescriptor(typeData, typeDescriptorBuilder);

        String template = "%stype %s %s;";

        return template.formatted(docs, typeData.name(), typeDescriptorBuilder.toString());
    }

    private String generateTypeDescriptor(Object typeDescriptor) {
        StringBuilder stringBuilder = new StringBuilder();
        generateTypeDescriptor(typeDescriptor, stringBuilder);
        return stringBuilder.toString();
    }

    private void generateTypeDescriptor(Object typeDescriptor, StringBuilder stringBuilder) {
        if (typeDescriptor instanceof String) { // Type reference or in-line type as string
            stringBuilder.append((String) typeDescriptor);
            return;
        }

        TypeData typeData;
        if (typeDescriptor instanceof Map) {
            String json = gson.toJson(typeDescriptor);
            typeData = gson.fromJson(json, TypeData.class);
        } else {
            typeData = (TypeData) typeDescriptor;
        }

        switch (typeData.codedata().node()) {
            case RECORD -> generateRecordTypeDescriptor(typeData, stringBuilder);
            case ARRAY -> generateArrayTypeDescriptor(typeData, stringBuilder);
            case MAP -> generateMapTypeDescriptor(typeData, stringBuilder);
            case STREAM -> generateStreamTypeDescriptor(typeData, stringBuilder);
            case FUTURE -> generateFutureTypeDescriptor(typeData, stringBuilder);
            case TYPEDESC -> generateTypedescTypeDescriptor(typeData, stringBuilder);
            case ERROR -> generateErrorTypeDescriptor(typeData, stringBuilder);
            case UNION -> generateUnionTypeDescriptor(typeData, stringBuilder);
            case INTERSECTION -> generateIntersectionTypeDescriptor(typeData, stringBuilder);
            case OBJECT -> generateObjectTypeDescriptor(typeData, stringBuilder);
            case TABLE -> generateTableTypeDescriptor(typeData, stringBuilder);
            case TUPLE -> generateTupleTypeDescriptor(typeData, stringBuilder);
            default -> throw new UnsupportedOperationException("Unsupported type descriptor: " + typeDescriptor);
        }
    }

    private void generateObjectTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        StringBuilder fieldsBuilder = new StringBuilder();
        for (Member member : typeData.members()) {
            generateFieldMember(member, fieldsBuilder, false);
        }

        // TODO: Generate functions if needed.

        String objectTemplate = "object {%s%n}";

        stringBuilder.append(objectTemplate.formatted(fieldsBuilder.toString()));
    }

    private void generateRecordTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
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
            generateFieldMember(member, fieldsBuilder, true);
        }

        // Build the rest field (if present).
        StringBuilder restBuilder = new StringBuilder();
        Optional.ofNullable(typeData.restMember()).ifPresent(restMember -> {
            restBuilder.append(LS).append("\t");
            generateTypeDescriptor(restMember.type(), restBuilder);
            restBuilder.append(" ...;");
        });

        // The template assumes that the dynamic parts already include their needed newlines and indentation.
        String template = "record {|%s%s%s%n|}";

        stringBuilder.append(template.formatted(
                inclusionsBuilder.toString(),
                fieldsBuilder.toString(),
                restBuilder.toString()
        ));
    }


    private void generateFieldMember(Member member, StringBuilder stringBuilder, boolean withDefaultValue) {
        String docs = generateDocs(member.docs(), "\t");
        stringBuilder.append(docs).append("\t").append(generateMember(member, withDefaultValue)).append(";");
    }

    private void generateTableTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        if (typeData.members().isEmpty()) {
            return;
        }

        // Build the base table type descriptor.
        StringBuilder baseTypeBuilder = new StringBuilder();
        generateTypeDescriptor(typeData.members().getFirst().type(), baseTypeBuilder);
        String baseType = baseTypeBuilder.toString();

        // Build the key type constraint if available.
        String keyInformation = "";
        if (typeData.members().size() > 1) {
            StringBuilder keyTypeBuilder = new StringBuilder();
            generateTypeDescriptor(typeData.members().get(1).type(), keyTypeBuilder);
            keyInformation = " key<" + keyTypeBuilder + ">";
        }

        // TODO: key specifier is not yet supported

        String template = "table<%s>%s";
        stringBuilder.append(template.formatted(baseType, keyInformation));
    }

    private void generateIntersectionTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        if (typeData.members().size() <= 1) {
            return;
        }
        for (int i = 0; i < typeData.members().size(); i++) {
            Member member = typeData.members().get(i);
            if (member.type() instanceof TypeData) {
                if (((TypeData) member.type()).codedata().node() == NodeKind.INTERSECTION) {
                    stringBuilder.append("(");
                    generateTypeDescriptor(member.type(), stringBuilder);
                    stringBuilder.append(")");
                } else {
                    generateTypeDescriptor(member.type(), stringBuilder);
                }
            } else {
                generateTypeDescriptor(member.type(), stringBuilder);
            }
            if (i < typeData.members().size() - 1) {
                stringBuilder.append(" & ");
            }
        }
    }

    private void generateTupleTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        // If there are no members, output an empty tuple.
        if (typeData.members().isEmpty()) {
            stringBuilder.append("[]");
            return;
        }

        // Build the dynamic list of tuple elements.
        StringJoiner joiner = new StringJoiner(", ");
        for (Member member : typeData.members()) {
            StringBuilder memberDescriptor = new StringBuilder();
            generateTypeDescriptor(member.type(), memberDescriptor);
            joiner.add(memberDescriptor.toString());
        }

        String template = "[%s]";
        stringBuilder.append(template.formatted(joiner.toString()));
    }

    private void generateUnionTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        if (typeData.members().size() <= 1) {
            return;
        }
        for (int i = 0; i < typeData.members().size(); i++) {
            Member member = typeData.members().get(i);
            if (member.type() instanceof TypeData) {
                if (((TypeData) member.type()).codedata().node() == NodeKind.UNION) {
                    stringBuilder.append("(");
                    generateTypeDescriptor(member.type(), stringBuilder);
                    stringBuilder.append(")");
                } else {
                    generateTypeDescriptor(member.type(), stringBuilder);
                }
            } else {
                generateTypeDescriptor(member.type(), stringBuilder);
            }
            if (i < typeData.members().size() - 1) {
                stringBuilder.append("|");
            }
        }
    }

    private void generateErrorTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        if (typeData.members().size() == 1) {
            stringBuilder.append("error<");
            generateTypeDescriptor(typeData.members().getFirst().type(), stringBuilder);
            stringBuilder.append(">");
        }
    }

    private void generateTypedescTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        if (typeData.members().size() == 1) {
            stringBuilder.append("typedesc<");
            generateTypeDescriptor(typeData.members().getFirst().type(), stringBuilder);
            stringBuilder.append(">");
        }
    }

    private void generateFutureTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        if (typeData.members().size() == 1) {
            stringBuilder.append("future<");
            generateTypeDescriptor(typeData.members().getFirst().type(), stringBuilder);
            stringBuilder.append(">");
        }
    }

    private void generateStreamTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        if (typeData.members().size() == 1) {
            stringBuilder.append("stream<");
            generateTypeDescriptor(typeData.members().getFirst().type(), stringBuilder);
            stringBuilder.append(">");
        }
    }

    private void generateMapTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        if (typeData.members().size() == 1) {
            stringBuilder.append("map<");
            generateTypeDescriptor(typeData.members().getFirst().type(), stringBuilder);
            stringBuilder.append(">");
        }
    }

    private void generateArrayTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        if (typeData.members().size() == 1) {
            generateTypeDescriptor(typeData.members().getFirst().type(), stringBuilder);
        }
        stringBuilder.append("[]");
    }

    private String generateDocs(String docs, String indent) {
        return (docs != null && !docs.isEmpty())
                ? LS + indent + CommonUtils.convertToBalDocs(docs)
                : LS;
    }

    private String generateMember(Member member, boolean withDefaultValue) {
        StringBuilder typeDescriptorBuilder = new StringBuilder();
        generateTypeDescriptor(member.type(), typeDescriptorBuilder);

        String template = "%s %s%s"; // <type descriptor> <identifier> [= <default value>]

        return template.formatted(typeDescriptorBuilder.toString(),
                CommonUtil.escapeReservedKeyword(member.name()),
                (withDefaultValue && member.defaultValue() != null && !member.defaultValue().isEmpty()) ?
                        " = " + member.defaultValue() : "");
    }

    private void generateInferredGraphqlClassField(Function function, StringBuilder stringBuilder) {
        String template = "%n\tprivate final %s %s;";
        String classField = template.formatted(generateTypeDescriptor(function.returnType()),
                CommonUtil.escapeReservedKeyword(function.name()));
        stringBuilder.append(classField);
    }

    private void generateResourceFunction(Function function, StringBuilder stringBuilder) {
        String docs = (function.description() != null && !function.description().isEmpty())
                ? LS + "\t" + CommonUtils.convertToBalDocs(function.description())
                : LS;

        StringJoiner paramJoiner = new StringJoiner(", ");
        for (Member param : function.parameters()) {
            String genParam = generateMember(param, true);
            paramJoiner.add(genParam);
        }

        String template = "%s\tresource function %s %s(%s) returns %s {" +
                "%n\t\tdo {" +
                "%n\t\t\treturn self.%s;" +
                "%n\t\t} on fail error err {" +
                "%n\t\t\t//handle error" +
                "%n\t\t\tpanic err;" +
                "%n\t\t}" +
                "%n\t}";

        stringBuilder.append(template.formatted(
                docs,
                function.accessor(),
                function.name(),
                paramJoiner.toString(),
                generateTypeDescriptor(function.returnType()),
                function.name()
        ));
    }
}
