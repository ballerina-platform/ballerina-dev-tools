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
import io.ballerina.flowmodelgenerator.core.model.Member;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.TypeData;

import java.util.Map;
import java.util.Optional;

/**
 * Code snippet generator.
 *
 * @since 2.0.0
 */
public class SourceCodeGenerator {
    private final Gson gson = new Gson();

    public String generateCodeSnippetForType(TypeData typeData) {
        NodeKind nodeKind = typeData.codedata().node();
        return switch (nodeKind) {
            case SERVICE_DECLARATION, CLASS -> "";  // TODO: Implement this
            case ENUM -> generateEnumCodeSnippet(typeData);
            default -> generateTypeDefCodeSnippet(typeData);
        };
    }

    private String generateEnumCodeSnippet(TypeData typeData) {
        return null;
    }

    private String generateTypeDefCodeSnippet(TypeData typeData) {
        StringBuilder stringBuilder = new StringBuilder();

        // Add documentation if present
        if (typeData.metadata().description() != null && !typeData.metadata().description().isEmpty()) {
            stringBuilder.append(CommonUtils.convertToBalDocs(typeData.metadata().description()));
        }

        // Add type name
        stringBuilder.append("type ")
                .append(typeData.name())
                .append(" ");

        generateTypeDescriptor(typeData, stringBuilder);

        return stringBuilder.append(";\n").toString();
    }

    private void generateTypeDescriptor(Object typeDescriptor, StringBuilder stringBuilder) {
        if (typeDescriptor instanceof String) { // Type reference
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
            default -> throw new UnsupportedOperationException("Unsupported type descriptor: " + typeDescriptor);
        }
    }

    private void generateObjectTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        stringBuilder.append("object {");

        // Add fields
        for (Member member : typeData.members()) {
            generateMember(member, stringBuilder, false);
        }

        // TODO: Add functions
        stringBuilder.append("\n}");
    }

    private void generateRecordTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        // Assumption: a record is always a closed records
        stringBuilder.append("record {|");

        // Add inclusions
        typeData.includes().forEach(include -> stringBuilder.append("\n\t*").append(include).append(";"));

        // Add fields
        for (Member member : typeData.members()) {
            generateMember(member, stringBuilder, true);
        }

        // Add rest field
        Optional.ofNullable(typeData.restMember()).ifPresent(restMember -> {
            stringBuilder.append("\n\t");
            generateTypeDescriptor(restMember.type(), stringBuilder);
            stringBuilder.append(" ...;");
        });
        stringBuilder.append("\n|}");
    }

    private void generateMember(Member member, StringBuilder stringBuilder, boolean withDefaultValue) {
        if (member.docs() != null && !member.docs().isEmpty()) {
            stringBuilder
                    .append("\n\t")
                    .append(CommonUtils.convertToBalDocs(member.docs()));
        } else {
            stringBuilder.append("\n");
        }
        stringBuilder.append("\t");
        generateTypeDescriptor(member.type(), stringBuilder);
        stringBuilder.append(" ").append(member.name());

        if (withDefaultValue) {
            stringBuilder.append((member.defaultValue() != null && !member.defaultValue().isEmpty()) ?
                    " = " + member.defaultValue() : "");
        }
        stringBuilder.append(";");
    }

    private void generateTableTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        if (!typeData.members().isEmpty()) {
            stringBuilder.append("table<");
            generateTypeDescriptor(typeData.members().getFirst().type(), stringBuilder);
            stringBuilder.append(">");

            if (typeData.members().size() > 1) {
                stringBuilder.append(" key<");
                generateTypeDescriptor(typeData.members().get(1).type(), stringBuilder);
                stringBuilder.append(">");
            }

            // TODO: key specifier is not yet supported
        }
    }

    private void generateIntersectionTypeDescriptor(TypeData typeData, StringBuilder stringBuilder) {
        if (typeData.members().size() <= 1) {
            return;
        }
        for (Member member : typeData.members()) {
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
            stringBuilder.append(" & ");
        }
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
}
