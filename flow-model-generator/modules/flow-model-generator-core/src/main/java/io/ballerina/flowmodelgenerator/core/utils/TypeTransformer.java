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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.flowmodelgenerator.core.model.Member;
import io.ballerina.flowmodelgenerator.core.model.ModuleInfo;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.TypeData;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TypeTransformer {
    private final Module module;
    private final ModuleInfo moduleInfo;
    private Map<String, RecordTypeDescriptorNode> recordTypeDescNodes;

    public TypeTransformer(Module module) {
        this.module = module;
        this.moduleInfo = ModuleInfo.from(module.descriptor());
    }

    // Transform type definition symbol to a type-data object
    public Object transform(TypeDefinitionSymbol typeDef) {
        // TODO: Fix this. Support all types
        if (typeDef.typeDescriptor().typeKind() != TypeDescKind.RECORD) {
            return null;
        }

        TypeData.TypeDataBuilder typeDataBuilder = new TypeData.TypeDataBuilder();
        String typeName;
        if (CommonUtils.isWithinPackage(typeDef, this.moduleInfo)) {
            typeName = typeDef.getName().get();
        } else {
            ModuleID recTypeModuleId = typeDef.getModule().get().id();
            typeName = String.format("%s/%s:%s",
                    recTypeModuleId.orgName(), recTypeModuleId.packageName(), typeDef.getName().get());
        }
        typeDataBuilder
                .name(typeName)
                .editable()
                .metadata()
                    .label(typeName)
                    .stepOut()
                .codedata()
                    .lineRange(typeDef.getLocation().get().lineRange())
                    .stepOut()
                .properties()
                    .name(typeName, false, true, false);

        if (typeDef.documentation().isPresent()) {
            String doc = getDocumentString(typeDef);
            typeDataBuilder
                    .metadata().description(getDocumentString(typeDef)).stepOut()
                    .properties().description(doc, false, true, false);
        }

        // Need to set the node in codedata, properties and other type descriptor specific data
        return transform(typeDef.typeDescriptor(), typeDataBuilder);
    }

    public Object transform(RecordTypeSymbol recordTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        typeDataBuilder
                .codedata()
                    .node(NodeKind.RECORD)
                    .stepOut()
                .properties()
                    .isArray("false", true, true, true)
                    .arraySize("", true, true, true);

        // includes
        List<String> includes = new ArrayList<>();
        recordTypeSymbol.typeInclusions().forEach(typeInclusion -> {
            includes.add(CommonUtils.getTypeSignature(typeInclusion, this.moduleInfo));
        });
        typeDataBuilder.includes(includes);

        Member.MemberBuilder memberBuilder = new Member.MemberBuilder();

        // rest member
        Optional<TypeSymbol> restTypeSymbol = recordTypeSymbol.restTypeDescriptor();
        if (restTypeSymbol.isPresent()) {
            TypeData.TypeDataBuilder restTypeDataBuilder = new TypeData.TypeDataBuilder();
            Object transformedRestType = transform(restTypeSymbol.get(), restTypeDataBuilder);
            Member restMember = memberBuilder
                    .kind(Member.MemberKind.FIELD)
                    .type(transformedRestType)
                    .refs(transformedRestType instanceof String ?
                            TypeUtils.getTypeRefIds(restTypeSymbol.get(), moduleInfo) : List.of())
                    .build();
            typeDataBuilder.restMember(restMember);
        }

        // members
        Map<String, Member> fieldMembers = new HashMap<>();
        recordTypeSymbol.fieldDescriptors().forEach((fieldName, fieldSymbol) -> {
            TypeData.TypeDataBuilder memberTypeDataBuilder = new TypeData.TypeDataBuilder();
            Object transformedFieldType = transform(fieldSymbol.typeDescriptor(), memberTypeDataBuilder);
            Member member = memberBuilder
                    .name(fieldName)
                    .kind(Member.MemberKind.FIELD)
                    .type(transformedFieldType)
                    .refs(transformedFieldType instanceof String ?
                            TypeUtils.getTypeRefIds(fieldSymbol.typeDescriptor(), moduleInfo) : List.of())
                    .docs(getDocumentString(fieldSymbol))
                    .defaultValue(getDefaultValueOfField(typeDataBuilder.name(), fieldName).orElse(null))
                    .build();
            fieldMembers.put(fieldName, member);
        });
        typeDataBuilder.members(fieldMembers);

        return typeDataBuilder.build();
    }

    public Object transform(TypeSymbol typeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        return switch (typeSymbol.typeKind()) {
            case RECORD -> transform((RecordTypeSymbol) typeSymbol, typeDataBuilder);
            default -> CommonUtils.getTypeSignature(typeSymbol, this.moduleInfo);
        };
    }

    // Utils
    private Map<String, RecordTypeDescriptorNode> getRecordTypeDescNodes() {
        if (this.recordTypeDescNodes != null) {
            return this.recordTypeDescNodes;
        }
        TypeDefinitionNodeVisitor typeDefNodeVisitor = new TypeDefinitionNodeVisitor();
        this.module.documentIds().forEach(documentId -> {
            Document document = this.module.document(documentId);
            SyntaxTree syntaxTree = document.syntaxTree();
            syntaxTree.rootNode().accept(typeDefNodeVisitor);
        });
        this.recordTypeDescNodes = typeDefNodeVisitor.getRecordTypeDescNodes();
        return this.recordTypeDescNodes;
    }

    private Optional<String> getDefaultValueOfField(String typeName, String fieldName) {
        RecordTypeDescriptorNode recordTypeDescriptorNode = getRecordTypeDescNodes().get(typeName);
        if (recordTypeDescriptorNode == null) {
            return Optional.empty();
        }
        return recordTypeDescriptorNode.fields().stream()
                .filter(field ->
                        field.kind() == SyntaxKind.RECORD_FIELD_WITH_DEFAULT_VALUE &&
                                ((RecordFieldWithDefaultValueNode) field).fieldName().text().equals(fieldName))
                .findFirst()
                .map(node -> ((RecordFieldWithDefaultValueNode) node).expression().toString());
    }

    private String getDocumentString(Documentable documentable) {
        if (documentable.documentation().isPresent()) {
            return documentable.documentation().get().description().orElse("");
        }
        return null;
    }
}
