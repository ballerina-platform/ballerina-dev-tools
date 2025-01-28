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
import io.ballerina.compiler.api.symbols.AbsResourcePathAttachPoint;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.EnumSymbol;
import io.ballerina.compiler.api.symbols.ErrorTypeSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.FutureTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.LiteralAttachPoint;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ObjectFieldSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.ServiceAttachPoint;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.StreamTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.values.ConstantValue;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.flowmodelgenerator.core.model.Function;
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

/**
 * Transformer to transform Ballerina type symbols to type data.
 * @since 2.0.0
 */
public class TypeTransformer {
    private final Module module;
    private final ModuleInfo moduleInfo;
    private Map<String, RecordTypeDescriptorNode> recordTypeDescNodes;

    public TypeTransformer(Module module) {
        this.module = module;
        this.moduleInfo = ModuleInfo.from(module.descriptor());
    }

    public Object transform(ServiceDeclarationSymbol serviceDeclarationSymbol) {
        TypeData.TypeDataBuilder typeDataBuilder = new TypeData.TypeDataBuilder();
        String attachPoint = serviceDeclarationSymbol.attachPoint().map(this::getAttachPoint).orElse("");
        List<String> qualifiers = serviceDeclarationSymbol.qualifiers().stream().map(Qualifier::getValue).toList();
        typeDataBuilder
                .name(attachPoint)
                .editable()
                .metadata()
                    .label(attachPoint)
                    .description(getDocumentString(serviceDeclarationSymbol))
                    .stepOut()
                .codedata()
                    .node(NodeKind.SERVICE_DECLARATION)
                    .lineRange(serviceDeclarationSymbol.getLocation().get().lineRange())
                    .stepOut()
                .properties()
                    .name(attachPoint, false, true, false)
                    .qualifiers(qualifiers, true, true, true)
                    .isArray("false", true, true, true)
                    .arraySize("", false, false, false);

        // class fields
        Map<String, Member> fieldMembers = new HashMap<>();
        serviceDeclarationSymbol.fieldDescriptors().forEach((name, symbol) -> {
            fieldMembers.putIfAbsent(name, transformObjectFieldAsMember(name, symbol));
        });
        typeDataBuilder.members(fieldMembers);

        // methods
        List<Function> methods = transformMethodSymbols(serviceDeclarationSymbol.methods());
        typeDataBuilder.functions(methods);

        return typeDataBuilder.build();
    }

    public Object transform(ClassSymbol classSymbol) {
        TypeData.TypeDataBuilder typeDataBuilder = new TypeData.TypeDataBuilder();
        typeDataBuilder
                .codedata()
                    .node(NodeKind.CLASS)
                    .lineRange(classSymbol.getLocation().get().lineRange())
                    .stepOut()
                .properties()
                    .qualifiers(classSymbol.qualifiers().stream().map(Qualifier::getValue).toList(), true, true, true)
                    .isArray("false", true, true, true)
                    .arraySize("", false, false, false);

        // inclusions
        List<String> includes = new ArrayList<>();
        classSymbol.typeInclusions().forEach(typeInclusion -> {
            includes.add(CommonUtils.getTypeSignature(typeInclusion, this.moduleInfo));
        });
        typeDataBuilder.includes(includes);

        // class fields
        Map<String, Member> fieldMembers = new HashMap<>();
        classSymbol.fieldDescriptors().forEach((name, symbol) -> {
            fieldMembers.putIfAbsent(name, transformObjectFieldAsMember(name, symbol));
        });
        typeDataBuilder.members(fieldMembers);

        // methods
        List<Function> methods = transformMethodSymbols(classSymbol.methods());
        typeDataBuilder.functions(methods);

        return typeDataBuilder.build();
    }

    public Object transform(ObjectTypeSymbol objectTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        typeDataBuilder.codedata().node(NodeKind.OBJECT);

        // inclusions
        List<String> includes = new ArrayList<>();
        objectTypeSymbol.typeInclusions().forEach(typeInclusion -> {
            includes.add(CommonUtils.getTypeSignature(typeInclusion, this.moduleInfo));
        });
        typeDataBuilder.includes(includes);

        // object fields
        Map<String, Member> fieldMembers = new HashMap<>();
        objectTypeSymbol.fieldDescriptors().forEach((name, symbol) -> {
            fieldMembers.putIfAbsent(name, transformObjectFieldAsMember(name, symbol));
        });
        typeDataBuilder.members(fieldMembers);

        // methods
        List<Function> functions = transformMethodSymbols(objectTypeSymbol.methods());
        typeDataBuilder.functions(functions);

        return typeDataBuilder.build();
    }

    public Object transform(TypeDefinitionSymbol typeDef) {
        TypeData.TypeDataBuilder typeDataBuilder = new TypeData.TypeDataBuilder();
        String typeName = getTypeName(typeDef);
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

        return transform(typeDef.typeDescriptor(), typeDataBuilder);
    }

    public Object transform(EnumSymbol enumSymbol) {
        TypeData.TypeDataBuilder typeDataBuilder = new TypeData.TypeDataBuilder();
        String typeName = getTypeName(enumSymbol);
        typeDataBuilder
                .name(typeName)
                .editable()
                .metadata()
                    .label(typeName)
                    .stepOut()
                .codedata()
                    .node(NodeKind.ENUM)
                    .lineRange(enumSymbol.getLocation().get().lineRange())
                    .stepOut()
                .properties()
                    .isArray("false", true, true, true)
                    .arraySize("", false, false, false);

        if (enumSymbol.documentation().isPresent()) {
            String doc = getDocumentString(enumSymbol);
            typeDataBuilder
                    .metadata().description(getDocumentString(enumSymbol)).stepOut()
                    .properties().description(doc, false, true, false);
        }

        Map<String, Member> members = new HashMap<>();
        Member.MemberBuilder memberBuilder = new Member.MemberBuilder();
        enumSymbol.members().forEach(enumMember -> {
            String name = enumMember.getName().get();
            Member member = memberBuilder
                    .name(name)
                    .kind(Member.MemberKind.NAME)
                    .type(((ConstantValue) enumMember.constValue()).value().toString())
                    .refs(List.of())
                    .build();
            members.putIfAbsent(name, member);
        });
        typeDataBuilder.members(members);

        return typeDataBuilder.build();
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

    public Object transform(UnionTypeSymbol unionTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        typeDataBuilder
                .codedata()
                    .node(NodeKind.UNION)
                    .stepOut()
                .properties()
                    .isArray("false", true, true, true)
                    .arraySize("", false, false, false);

        Member.MemberBuilder memberBuilder = new Member.MemberBuilder();
        Map<String, Member> memberTypes = new HashMap<>();
        unionTypeSymbol.userSpecifiedMemberTypes().forEach(memberTypeSymbol -> {
            String name = CommonUtils.getTypeSignature(memberTypeSymbol, this.moduleInfo);
            Member member = transformTypeAsMember(name, memberTypeSymbol, memberBuilder);
            memberTypes.putIfAbsent(name, member);
        });
        typeDataBuilder.members(memberTypes);

        return typeDataBuilder.build();
    }

    public Object transform(IntersectionTypeSymbol intersectionTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        typeDataBuilder
                .codedata()
                    .node(NodeKind.INTERSECTION)
                    .stepOut()
                .properties()
                    .isArray("false", true, true, true)
                    .arraySize("", false, false, false);

        Member.MemberBuilder memberBuilder = new Member.MemberBuilder();
        Map<String, Member> memberTypes = new HashMap<>();
        intersectionTypeSymbol.memberTypeDescriptors().forEach(memberTypeSymbol -> {
            String name = CommonUtils.getTypeSignature(memberTypeSymbol, this.moduleInfo);
            Member member = transformTypeAsMember(name, memberTypeSymbol, memberBuilder);
            memberTypes.putIfAbsent(name, member);
        });
        typeDataBuilder.members(memberTypes);

        return typeDataBuilder.build();
    }

    public Object transform(ArrayTypeSymbol arrayTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        return transformTypesWithConstraintType(arrayTypeSymbol, NodeKind.ARRAY, typeDataBuilder);
    }

    public Object transform(MapTypeSymbol mapTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        return transformTypesWithConstraintType(mapTypeSymbol, NodeKind.MAP, typeDataBuilder);
    }

    public Object transform(StreamTypeSymbol streamTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        return transformTypesWithConstraintType(streamTypeSymbol, NodeKind.STREAM, typeDataBuilder);
    }

    public Object transform(FutureTypeSymbol futureTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        return transformTypesWithConstraintType(futureTypeSymbol, NodeKind.FUTURE, typeDataBuilder);
    }

    public Object transform(TypeDescTypeSymbol typeDescTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        return transformTypesWithConstraintType(typeDescTypeSymbol, NodeKind.TYPEDESC, typeDataBuilder);
    }

    public Object transform(ErrorTypeSymbol errorTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        return transformTypesWithConstraintType(errorTypeSymbol, NodeKind.ERROR, typeDataBuilder);
    }

    public Object transform(TypeSymbol typeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        return switch (typeSymbol.typeKind()) {
            case RECORD -> transform((RecordTypeSymbol) typeSymbol, typeDataBuilder);
            case ARRAY -> transform((ArrayTypeSymbol) typeSymbol, typeDataBuilder);
            case MAP -> transform((MapTypeSymbol) typeSymbol, typeDataBuilder);
            case STREAM -> transform((StreamTypeSymbol) typeSymbol, typeDataBuilder);
            case FUTURE -> transform((FutureTypeSymbol) typeSymbol, typeDataBuilder);
            case TYPEDESC -> transform((TypeDescTypeSymbol) typeSymbol, typeDataBuilder);
            case ERROR -> transform((ErrorTypeSymbol) typeSymbol, typeDataBuilder);
            case UNION -> transform((UnionTypeSymbol) typeSymbol, typeDataBuilder);
            case INTERSECTION -> transform((IntersectionTypeSymbol) typeSymbol, typeDataBuilder);
            case OBJECT -> transform((ObjectTypeSymbol) typeSymbol, typeDataBuilder);
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

    private Object transformTypesWithConstraintType(TypeSymbol typeSymbol,
                                                    NodeKind nodeKind,
                                                    TypeData.TypeDataBuilder typeDataBuilder) {
        typeDataBuilder
                .codedata()
                    .node(nodeKind)
                    .stepOut()
                .properties()
                    .isArray(nodeKind == NodeKind.ARRAY ? "true" : "false", true, true, true)
                    .arraySize("", false, false, false);

        Member.MemberBuilder memberBuilder = new Member.MemberBuilder();
        TypeSymbol memberTypeDesc = switch (typeSymbol.typeKind()) {
            case ARRAY -> ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor();
            case MAP -> ((MapTypeSymbol) typeSymbol).typeParam();
            case FUTURE -> ((FutureTypeSymbol) typeSymbol).typeParameter().orElse(null);
            case STREAM -> ((StreamTypeSymbol) typeSymbol).typeParameter();
            case TYPEDESC -> ((TypeDescTypeSymbol) typeSymbol).typeParameter().orElse(null);
            case ERROR -> ((ErrorTypeSymbol) typeSymbol).detailTypeDescriptor();
            default -> null;
        };

        if (memberTypeDesc == null) {
            return typeDataBuilder.build();
        }

        String memberTypeName = CommonUtils.getTypeSignature(memberTypeDesc, moduleInfo);
        Member memberType = transformTypeAsMember(memberTypeName, memberTypeDesc, memberBuilder);
        typeDataBuilder.members(Map.of(memberTypeName, memberType));

        return typeDataBuilder.build();
    }

    private List<Function> transformMethodSymbols(Map<String, ? extends MethodSymbol> methods) {
        Member.MemberBuilder memberBuilder = new Member.MemberBuilder();
        List<Function> functions = new ArrayList<>();
        methods.forEach((name, methodSymbol) -> {
            Function.FunctionBuilder functionBuilder = new Function.FunctionBuilder();
            FunctionTypeSymbol functionTypeSymbol = methodSymbol.typeDescriptor();

            functionBuilder
                    .kind(Function.FunctionKind.FUNCTION)
                    .accessor(methodSymbol.getName().orElse(""));

            // return type
            functionTypeSymbol.returnTypeDescriptor().ifPresent(returnType -> {
                Object transformed = transform(functionTypeSymbol.returnTypeDescriptor().get(),
                        new TypeData.TypeDataBuilder());
                functionBuilder
                        .returnType(transformed)
                        .returnTypeRefs(transformed instanceof String
                                ? TypeUtils.getTypeRefIds(functionTypeSymbol.returnTypeDescriptor().get(), moduleInfo)
                                : List.of());
            });

            // params
            functionTypeSymbol.params().ifPresent(params -> {
                List<Member> parameters = params.stream().map(param -> {
                    Object transformedParamType = transform(param.typeDescriptor(), new TypeData.TypeDataBuilder());
                    return memberBuilder
                            .name(param.getName().orElse(null))
                            .kind(Member.MemberKind.FIELD)
                            .type(transformedParamType)
                            .refs(transformedParamType instanceof String ?
                                    TypeUtils.getTypeRefIds(param.typeDescriptor(), moduleInfo) : List.of())
                            .build();
                }).toList();
                functionBuilder.parameters(parameters);
            });

            // rest param
            functionTypeSymbol.restParam().ifPresent(restParam -> {
                Object transformedRestParamType = transform(restParam.typeDescriptor(), new TypeData.TypeDataBuilder());
                Member restParameter = memberBuilder
                        .name(restParam.getName().get())
                        .kind(Member.MemberKind.FIELD)
                        .type(transformedRestParamType)
                        .refs(transformedRestParamType instanceof String ?
                                TypeUtils.getTypeRefIds(restParam.typeDescriptor(), moduleInfo) : List.of())
                        .build();
                functionBuilder.restParameter(restParameter);
            });


            // qualifiers
            List<String> qualifiers = new ArrayList<>();
            methodSymbol.qualifiers().forEach(q -> {
                qualifiers.add(q.name());
                if (q.equals(Qualifier.REMOTE)) {
                    functionBuilder.kind(Function.FunctionKind.REMOTE);
                } else if (q.equals(Qualifier.RESOURCE)) {
                    functionBuilder.kind(Function.FunctionKind.RESOURCE);
                }
            });
            functionBuilder.qualifiers(qualifiers);

            // resource path
            // TODO: Need a structured schema for resourcePath
            if (methodSymbol.kind().equals(SymbolKind.RESOURCE_METHOD)) {
                functionBuilder.resourcePath(((ResourceMethodSymbol) methodSymbol).resourcePath().signature());
            }

            functions.add(functionBuilder.build());
        });
        return functions;
    }

    private Member transformObjectFieldAsMember(String fieldName, ObjectFieldSymbol fieldSymbol) {
        TypeData.TypeDataBuilder attributeTypeDataBuilder = new TypeData.TypeDataBuilder();
        Object transformedAttributeType = transform(fieldSymbol.typeDescriptor(), attributeTypeDataBuilder);
        return (new Member.MemberBuilder())
                .name(fieldName)
                .kind(Member.MemberKind.FIELD)
                .type(transformedAttributeType)
                .refs(transformedAttributeType instanceof String ?
                        TypeUtils.getTypeRefIds(fieldSymbol.typeDescriptor(), moduleInfo) : List.of())
                .docs(getDocumentString(fieldSymbol))
                .build();
    }

    private Member transformTypeAsMember(String typeName, TypeSymbol memberTypeDesc,
                                         Member.MemberBuilder memberBuilder) {
        TypeData.TypeDataBuilder memberTypeDataBuilder = new TypeData.TypeDataBuilder();
        Object transformedMemberType = transform(memberTypeDesc, memberTypeDataBuilder);
        return memberBuilder
                .name(typeName)
                .kind(Member.MemberKind.TYPE)
                .type(transformedMemberType)
                .refs(transformedMemberType instanceof String ?
                        TypeUtils.getTypeRefIds(memberTypeDesc, moduleInfo) : List.of())
                .build();
    }

    private String getAttachPoint(ServiceAttachPoint attachPoint) {
        return switch (attachPoint.kind()) {
            case ABSOLUTE_RESOURCE_PATH -> "/" +
                    String.join("/", ((AbsResourcePathAttachPoint) attachPoint).segments());
            case STRING_LITERAL -> ((LiteralAttachPoint) attachPoint).literal();
        };
    }

    private String getTypeName(Symbol symbol) {
        String typeName;
        if (CommonUtils.isWithinPackage(symbol, this.moduleInfo)) {
            typeName = symbol.getName().get();
        } else {
            ModuleID recTypeModuleId = symbol.getModule().get().id();
            typeName = String.format("%s/%s:%s",
                    recTypeModuleId.orgName(), recTypeModuleId.packageName(), symbol.getName().get());
        }
        return typeName;
    }
}
