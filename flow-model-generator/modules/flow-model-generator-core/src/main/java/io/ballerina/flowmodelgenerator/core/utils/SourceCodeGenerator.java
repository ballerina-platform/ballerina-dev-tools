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
            initBody.append(LS).append("\t\tself.")
                    .append(function.name())
                    .append(" = ")
                    .append(function.name())
                    .append(";");
        }

        // Build resource functions.
        StringBuilder resourceFunctions = new StringBuilder();
        for (Function function : typeData.functions()) {
            if (function.description() != null && !function.description().isEmpty()) {
                resourceFunctions.append(LS).append("\t")
                        .append(CommonUtils.convertToBalDocs(function.description()));
            } else {
                resourceFunctions.append(LS);
            }
            resourceFunctions.append("\tresource function ")
                    .append(function.accessor())
                    .append(" ")
                    .append(function.name())
                    .append("(");

            // Build the parameters for the resource function.
            for (int i = 0; i < function.parameters().size(); i++) {
                Member param = function.parameters().get(i);
                generateTypeDescriptor(param.type(), resourceFunctions);
                resourceFunctions.append(" ").append(param.name());
                if (param.defaultValue() != null && !param.defaultValue().isEmpty()) {
                    resourceFunctions.append(" = ").append(param.defaultValue());
                }
                if (i < function.parameters().size() - 1) {
                    resourceFunctions.append(", ");
                }
            }
            resourceFunctions.append(") returns ");
            generateTypeDescriptor(function.returnType(), resourceFunctions);
            resourceFunctions.append(" {")
                    .append(LS)
                    .append("\t\treturn self.")
                    .append(function.name())
                    .append(";")
                    .append(LS)
                    .append("\t}");
        }

        String template = "service class %s {%s%n\tfunction init(%s) {%s%n\t}%s%n}";

        return template.formatted(
                typeData.name(),
                inferredFields.toString(),
                initParams.toString(),
                initBody.toString(),
                resourceFunctions.toString()
        );
    }


    private String generateEnumCodeSnippet(TypeData typeData) {
        String docs = "";
        if (typeData.metadata().description() != null && !typeData.metadata().description().isEmpty()) {
            docs = CommonUtils.convertToBalDocs(typeData.metadata().description());
        }

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
        String docs = "";
        if (typeData.metadata().description() != null && !typeData.metadata().description().isEmpty()) {
            docs = CommonUtils.convertToBalDocs(typeData.metadata().description());
        }

        // Generate the type descriptor into a StringBuilder.
        StringBuilder typeDescriptorBuilder = new StringBuilder();
        generateTypeDescriptor(typeData, typeDescriptorBuilder);

        String template = "%stype %s %s;";

        return template.formatted(docs, typeData.name(), typeDescriptorBuilder.toString());
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
            generateMember(member, fieldsBuilder, false);
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
            generateMember(member, fieldsBuilder, true);
        }

        // Build the rest field (if present).
        StringBuilder restBuilder = new StringBuilder();
        Optional.ofNullable(typeData.restMember()).ifPresent(restMember -> {
            restBuilder.append(LS).append("\t");
            generateTypeDescriptor(restMember.type(), restBuilder);
            restBuilder.append(" ...;");
        });

        // The template assumes that the dynamic parts already include their needed newlines and indentation.
        String recordTemplate = "record {|%s%s%s%n|}";

        stringBuilder.append(recordTemplate.formatted(
                inclusionsBuilder.toString(),
                fieldsBuilder.toString(),
                restBuilder.toString()
        ));
    }


    private void generateMember(Member member, StringBuilder stringBuilder, boolean withDefaultValue) {
        // The documentation string.
        String docs = (member.docs() != null && !member.docs().isEmpty())
                ? LS + "\t" + CommonUtils.convertToBalDocs(member.docs())
                : LS;

        // The default value string (if exist).
        String defaultValue = "";
        if (withDefaultValue && member.defaultValue() != null && !member.defaultValue().isEmpty()) {
            defaultValue = " = " + member.defaultValue();
        }

        // Generate the type descriptor using a temporary StringBuilder.
        StringBuilder typeDescriptorBuilder = new StringBuilder();
        generateTypeDescriptor(member.type(), typeDescriptorBuilder);
        String typeDescriptor = typeDescriptorBuilder.toString();

        // The member template:
        // %s -> documentation (including leading newline and indent)
        // \t -> fixed indent for the member declaration
        // %s -> type descriptor
        // %s -> member name
        // %s -> default value (if any)
        // ; -> terminator
        String memberTemplate = "%s\t%s %s%s;";

        // Append the formatted member to the main string builder.
        stringBuilder.append(memberTemplate.formatted(docs, typeDescriptor, member.name(), defaultValue));
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

    private void generateInferredGraphqlClassField(Function function, StringBuilder stringBuilder) {
        stringBuilder.append(LS).append("\tprivate final ");
        generateTypeDescriptor(function.returnType(), stringBuilder);
        stringBuilder.append(" ").append(function.name());
        stringBuilder.append(";");
    }
}
