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
            inferredFields.append(generateInferredGraphqlClassField(function));
        }

        // Build the "init" function parameters and body.
        // Infer the parameters and body from the functions.
        StringBuilder initParams = new StringBuilder();
        StringBuilder initBody = new StringBuilder();
        for (int i = 0; i < typeData.functions().size(); i++) {
            Function function = typeData.functions().get(i);
            // Append the return type and function name as a parameter.
            String paramTemplate = "%s %s";
            String param = paramTemplate.formatted(generateTypeDescriptor(function.returnType()), function.name());
            initParams.append(param);
            if (i < typeData.functions().size() - 1) {
                initParams.append(", ");
            }
            // Build the init function body: "self.<function-name> = <function-name>;"
            // TODO: Add do-on-fail block after fixing https://github.com/ballerina-platform/ballerina-lang/issues/43817
            String initBodyStatementTemplate = "%n\t\tself.%s = %s;";
            initBody.append(initBodyStatementTemplate.formatted(function.name(), function.name()));
        }

        // Build the resource functions.
        StringBuilder resourceFunctions = new StringBuilder();
        for (Function function : typeData.functions()) {
            resourceFunctions.append(generateResourceFunction(function));
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
        String typeDescriptor = generateTypeDescriptor(typeData);

        String template = "%stype %s %s;";

        return template.formatted(docs, typeData.name(), typeDescriptor);
    }

    private String generateTypeDescriptor(Object typeDescriptor) {
        if (typeDescriptor instanceof String) { // Type reference or in-line type as string
            return (String) typeDescriptor;
        }

        TypeData typeData;
        if (typeDescriptor instanceof Map) {
            String json = gson.toJson(typeDescriptor);
            typeData = gson.fromJson(json, TypeData.class);
        } else {
            typeData = (TypeData) typeDescriptor;
        }

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
        if (typeData.restMember() != null) {
            String typeDescriptor = generateTypeDescriptor(typeData.restMember().type());
            restField = LS + "\t" + typeDescriptor + " ...;";
        }

        // The template assumes that the dynamic parts already include their needed newlines and indentation.
        String template = "record {|%s%s%s%n|}";

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
        String rowType = generateTypeDescriptor(typeData.members().getFirst().type());

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
                            .append(generateTypeDescriptor(member.type()))
                            .append(")");
                } else {
                    stringBuilder.append(generateTypeDescriptor(member.type()));
                }
            } else {
                stringBuilder.append(generateTypeDescriptor(member.type()));
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
            joiner.add(generateTypeDescriptor(member.type()));
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
                            .append(generateTypeDescriptor(member.type()))
                            .append(")");
                } else {
                    stringBuilder.append(generateTypeDescriptor(member.type()));
                }
            } else {
                stringBuilder.append(generateTypeDescriptor(member.type()));
            }
            if (i < typeData.members().size() - 1) {
                stringBuilder.append("|");
            }
        }
        return stringBuilder.toString();
    }

    private String generateErrorTypeDescriptor(TypeData typeData) {
        if (typeData.members().size() == 1) {
            return "error<" + generateTypeDescriptor(typeData.members().getFirst().type()) + ">";
        }
        return "error";
    }

    private String generateTypedescTypeDescriptor(TypeData typeData) {
        if (typeData.members().size() == 1) {
            return "typedesc<" + generateTypeDescriptor(typeData.members().getFirst().type()) + ">";
        }
        return "typedesc<>";
    }

    private String generateFutureTypeDescriptor(TypeData typeData) {
        if (typeData.members().size() == 1) {
            return "future<" + generateTypeDescriptor(typeData.members().getFirst().type()) + ">";
        }
        return "future<>";
    }

    private String generateStreamTypeDescriptor(TypeData typeData) {
        if (typeData.members().size() == 1) {
            return "stream<" + generateTypeDescriptor(typeData.members().getFirst().type()) + ">";
        }
        return "stream<>";
    }

    private String generateMapTypeDescriptor(TypeData typeData) {
        if (typeData.members().size() == 1) {
            return "map<" + generateTypeDescriptor(typeData.members().getFirst().type()) + ">";
        }
        return "map<>";
    }

    private String generateArrayTypeDescriptor(TypeData typeData) {
        if (typeData.members().size() == 1) {
            String transformed = generateTypeDescriptor(typeData.members().getFirst().type());
            return transformed + "[]";
        }
        return "[]";
    }

    private String generateDocs(String docs, String indent) {
        return (docs != null && !docs.isEmpty())
                ? LS + indent + CommonUtils.convertToBalDocs(docs)
                : LS;
    }

    private String generateMember(Member member, boolean withDefaultValue) {
        String typeDescriptor = generateTypeDescriptor(member.type());

        String template = "%s %s%s"; // <type descriptor> <identifier> [= <default value>]

        return template.formatted(typeDescriptor,
                CommonUtil.escapeReservedKeyword(member.name()),
                (withDefaultValue && member.defaultValue() != null && !member.defaultValue().isEmpty()) ?
                        " = " + member.defaultValue() : "");
    }

    private String generateInferredGraphqlClassField(Function function) {
        String template = "%n\tprivate final %s %s;";
        return template.formatted(generateTypeDescriptor(function.returnType()),
                CommonUtil.escapeReservedKeyword(function.name()));
    }

    private String generateResourceFunction(Function function) {
        String docs = generateDocs(function.description(), "\t");

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

        return template.formatted(
                docs,
                function.accessor(),
                function.name(),
                paramJoiner.toString(),
                generateTypeDescriptor(function.returnType()),
                function.name()
        );
    }
}
