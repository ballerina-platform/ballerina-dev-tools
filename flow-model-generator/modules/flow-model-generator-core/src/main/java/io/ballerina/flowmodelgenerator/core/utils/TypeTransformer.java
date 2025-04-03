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
import io.ballerina.compiler.api.symbols.FunctionSymbol;
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
import io.ballerina.compiler.api.symbols.TableTypeSymbol;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
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
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.TypeData;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;

import java.util.ArrayList;
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

    public static final String BUILT_IN_ERROR = "error";
    public static final String BUILT_IN_ANYDATA = "anydata";

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
                    .name(attachPoint, false, false, false)
                    .qualifiers(qualifiers, true, true, true)
                    .isArray("false", true, true, true)
                    .arraySize("", false, false, false);

        // class fields
        List<Member> fieldMembers = new ArrayList<>();
        serviceDeclarationSymbol.fieldDescriptors().forEach((name, symbol) -> {
            fieldMembers.add(transformObjectFieldAsMember(name, symbol));
        });
        typeDataBuilder.members(fieldMembers);

        // methods
        List<Function> methods = transformMethodSymbols(serviceDeclarationSymbol.methods());
        typeDataBuilder.functions(methods);

        return typeDataBuilder.build();
    }

    public Object transform(ClassSymbol classSymbol) {
        TypeData.TypeDataBuilder typeDataBuilder = new TypeData.TypeDataBuilder();
        String typeName = getTypeName(classSymbol);
        List<Qualifier> qualifiers = classSymbol.qualifiers();
        String networkQualifier = qualifiers.contains(Qualifier.SERVICE) ?
                "service" : (qualifiers.contains(Qualifier.CLIENT) ? "client" : "");
        typeDataBuilder
                .name(typeName)
                .editable()
                .metadata()
                    .label(typeName)
                    .description(getDocumentString(classSymbol))
                    .stepOut()
                .codedata()
                    .node(NodeKind.CLASS)
                    .lineRange(classSymbol.getLocation().get().lineRange())
                    .stepOut()
                .properties()
                    .name(typeName, false, false, false)
                    .qualifiers(qualifiers.stream().map(Qualifier::getValue).toList(), true, true, true)
                    .isArray("false", true, true, true)
                    .arraySize("", false, false, false)
                    .isPublic(qualifiers.contains(Qualifier.PUBLIC), true, true, false)
                    .isDistinct(qualifiers.contains(Qualifier.DISTINCT), true, true, false)
                    .isIsolated(qualifiers.contains(Qualifier.ISOLATED), true, true, false)
                    .isReadOnly(qualifiers.contains(Qualifier.READONLY), true, true, false)
                    .networkQualifier(networkQualifier, true, true, false);

        // inclusions
        List<String> includes = new ArrayList<>();
        classSymbol.typeInclusions().forEach(typeInclusion -> {
            includes.add(CommonUtils.getTypeSignature(typeInclusion, this.moduleInfo));
        });
        typeDataBuilder.includes(includes);

        // class fields
        List<Member> fieldMembers = new ArrayList<>();
        classSymbol.fieldDescriptors().forEach((name, symbol) -> {
            fieldMembers.add(transformObjectFieldAsMember(name, symbol));
        });
        typeDataBuilder.members(fieldMembers);

        // methods
        List<Function> methods = transformMethodSymbols(classSymbol.methods());
        typeDataBuilder.functions(methods);

        return typeDataBuilder.build();
    }

    public Object transform(ObjectTypeSymbol objectTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        typeDataBuilder.codedata().node(NodeKind.OBJECT);

        List<Qualifier> qualifiers = objectTypeSymbol.qualifiers();
        String networkQualifier = qualifiers.contains(Qualifier.SERVICE) ?
                "service" : (qualifiers.contains(Qualifier.CLIENT) ? "client" : "");
        typeDataBuilder.properties()
                .isIsolated(qualifiers.contains(Qualifier.ISOLATED), true, true, false)
                .networkQualifier(networkQualifier, true, true, false);

        // inclusions
        List<String> includes = new ArrayList<>();
        objectTypeSymbol.typeInclusions().forEach(typeInclusion -> {
            includes.add(CommonUtils.getTypeSignature(typeInclusion, this.moduleInfo));
        });
        typeDataBuilder.includes(includes);

        // object fields
        List<Member> fieldMembers = new ArrayList<>();
        objectTypeSymbol.fieldDescriptors().forEach((name, symbol) -> {
            fieldMembers.add(transformObjectFieldAsMember(name, symbol));
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
                    .name(typeName, false, false, false)
                    .isPublic(typeDef.qualifiers().contains(Qualifier.PUBLIC), true, true, false);

        if (typeDef.documentation().isPresent()) {
            String doc = getDocumentString(typeDef);
            typeDataBuilder
                    .metadata().description(doc).stepOut()
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
                    .name(typeName, false, false, false)
                    .isArray("false", true, true, true)
                    .arraySize("", false, false, false);

        if (enumSymbol.documentation().isPresent()) {
            String doc = getDocumentString(enumSymbol);
            typeDataBuilder
                    .metadata().description(doc).stepOut()
                    .properties().description(doc, false, true, false);
        }

        List<Member> members = new ArrayList<>();
        Member.MemberBuilder memberBuilder = new Member.MemberBuilder();
        enumSymbol.members().reversed().forEach(enumMember -> { // reverse to maintain the order
            String name = enumMember.getName().get();
            ConstantValue constValue = (ConstantValue) enumMember.constValue();
            String constValueAsString = "\"" + constValue.value() + "\"";
            memberBuilder
                    .name(name)
                    .kind(Member.MemberKind.NAME)
                    .type(constValueAsString)
                    .refs(List.of());
            if (!constValue.value().equals(name)) {
                memberBuilder.defaultValue(constValueAsString);
            }
            Member member = memberBuilder.build();
            members.add(member);
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
            if (transformedRestType.equals(BUILT_IN_ANYDATA)) {
                typeDataBuilder.allowAdditionalFields(true);
            }
            Member restMember = memberBuilder
                    .kind(Member.MemberKind.FIELD)
                    .type(transformedRestType)
                    .refs(getTypeRefs(transformedRestType, restTypeSymbol.get()))
                    .build();
            typeDataBuilder.restMember(restMember);
        }

        // members
        List<Member> fieldMembers = new ArrayList<>();
        recordTypeSymbol.fieldDescriptors().forEach((fieldName, fieldSymbol) -> {
            TypeData.TypeDataBuilder memberTypeDataBuilder = new TypeData.TypeDataBuilder();
            Object transformedFieldType = transform(fieldSymbol.typeDescriptor(), memberTypeDataBuilder);
            Member member = memberBuilder
                    .name(fieldName)
                    .kind(Member.MemberKind.FIELD)
                    .type(transformedFieldType)
                    .optional(fieldSymbol.isOptional())
                    .refs(getTypeRefs(transformedFieldType, fieldSymbol.typeDescriptor()))
                    .docs(getDocumentString(fieldSymbol))
                    .defaultValue(getDefaultValueOfField(typeDataBuilder.name(), fieldName).orElse(null))
                    .build();
            fieldMembers.add(member);
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
        List<Member> memberTypes = new ArrayList<>();
        unionTypeSymbol.userSpecifiedMemberTypes().forEach(memberTypeSymbol -> {
            String name = CommonUtils.getTypeSignature(memberTypeSymbol, this.moduleInfo);
            Member member = transformTypeAsMember(name, memberTypeSymbol, memberBuilder);
            memberTypes.add(member);
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
        List<Member> memberTypes = new ArrayList<>();
        intersectionTypeSymbol.memberTypeDescriptors().forEach(memberTypeSymbol -> {
            String name = CommonUtils.getTypeSignature(memberTypeSymbol, this.moduleInfo);
            Member member = transformTypeAsMember(name, memberTypeSymbol, memberBuilder);
            memberTypes.add(member);
        });
        typeDataBuilder.members(memberTypes);

        return typeDataBuilder.build();
    }

    public Object transform(TableTypeSymbol tableTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        typeDataBuilder
                .codedata()
                    .node(NodeKind.TABLE)
                    .stepOut()
                .properties()
                    .isArray("false", true, true, true)
                    .arraySize("", false, false, false);

        Member.MemberBuilder memberBuilder = new Member.MemberBuilder();

        List<Member> memberTypes = new ArrayList<>();

        // row type
        TypeSymbol rowTypeSymbol = tableTypeSymbol.rowTypeParameter();
        Object transformedRowType = transform(rowTypeSymbol, new TypeData.TypeDataBuilder());
        Member rowTypeMember = memberBuilder
                .name("rowType")
                .kind(Member.MemberKind.TYPE)
                .type(transformedRowType)
                .refs(getTypeRefs(transformedRowType, rowTypeSymbol))
                .build();
        memberTypes.add(rowTypeMember);

        // key constraint type
        tableTypeSymbol.keyConstraintTypeParameter().ifPresent(typeSymbol -> {
            Object transformedKeyConstraintType = transform(typeSymbol, new TypeData.TypeDataBuilder());
            Member keyConstraintTypeMember = memberBuilder
                    .name("keyConstraintType")
                    .kind(Member.MemberKind.TYPE)
                    .type(transformedKeyConstraintType)
                    .refs(getTypeRefs(transformedRowType, typeSymbol))
                    .build();
            memberTypes.add(keyConstraintTypeMember);
        });
        typeDataBuilder.members(memberTypes);

        return typeDataBuilder.build();
    }

    public Object transform(ArrayTypeSymbol arrayTypeSymbol, TypeData.TypeDataBuilder typeDataBuilder) {
        typeDataBuilder.properties()
                .isArray("true", true, true, true)
                .arraySize(arrayTypeSymbol.size().isPresent() ? arrayTypeSymbol.size().get().toString() : "",
                        true, true, true);
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
        if (errorTypeSymbol.signature().equals(BUILT_IN_ERROR)) {
            return BUILT_IN_ERROR;
        }
        return transformTypesWithConstraintType(errorTypeSymbol, NodeKind.ERROR, typeDataBuilder);
    }

    public Object transform(TupleTypeSymbol tupleTypeSymbol, TypeData.TypeDataBuilder typeBuilder) {
        typeBuilder
                .codedata()
                    .node(NodeKind.TUPLE)
                    .stepOut()
                .properties()
                    .isArray("false", true, true, true)
                    .arraySize("", false, false, false);

        Member.MemberBuilder memberBuilder = new Member.MemberBuilder();
        List<Member> memberTypes = new ArrayList<>();
        tupleTypeSymbol.memberTypeDescriptors().forEach(memberTypeSymbol -> {
            String name = CommonUtils.getTypeSignature(memberTypeSymbol, this.moduleInfo);
            Member member = transformTypeAsMember(name, memberTypeSymbol, memberBuilder);
            memberTypes.add(member);
        });
        typeBuilder.members(memberTypes);

        return typeBuilder.build();
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
            case TABLE -> transform((TableTypeSymbol) typeSymbol, typeDataBuilder);
            case TUPLE -> transform((TupleTypeSymbol) typeSymbol, typeDataBuilder);
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
        typeDataBuilder.codedata().node(nodeKind);

        if (nodeKind != NodeKind.ARRAY) {
            typeDataBuilder
                    .properties()
                    .isArray("false", true, true, true)
                    .arraySize("", false, false, false);
        }

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
        typeDataBuilder.members(List.of(memberType));

        return typeDataBuilder.build();
    }

    private List<Function> transformMethodSymbols(Map<String, ? extends MethodSymbol> methods) {
        return methods.values().stream().map(this::transformFunction).toList();
    }

    private Function transformFunction(FunctionSymbol functionSymbol) {
        Function.FunctionBuilder functionBuilder = new Function.FunctionBuilder();
        Member.MemberBuilder memberBuilder = new Member.MemberBuilder();

        List<Qualifier> functionQuals = functionSymbol.qualifiers();

        functionBuilder.kind(Function.FunctionKind.FUNCTION);

        // qualifiers
        List<String> qualifiers = new ArrayList<>();
        functionQuals.forEach(q -> {
            qualifiers.add(q.name());
            if (q.equals(Qualifier.REMOTE)) {
                functionBuilder.kind(Function.FunctionKind.REMOTE);
            } else if (q.equals(Qualifier.RESOURCE)) {
                functionBuilder.kind(Function.FunctionKind.RESOURCE);
            }
        });
        functionBuilder.qualifiers(qualifiers);

        functionBuilder
                .docs(getDocumentString(functionSymbol))
                .name(functionSymbol.getName().orElse(""))
                .properties()
                    .isPrivate(functionQuals.contains(Qualifier.PRIVATE), true, true, false)
                    .isPublic(functionQuals.contains(Qualifier.PUBLIC), true, true, false)
                    .isIsolated(functionQuals.contains(Qualifier.ISOLATED), true, true, false);

        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();

        // return type
        functionTypeSymbol.returnTypeDescriptor().ifPresent(returnType -> {
            Object transformed = transform(returnType, new TypeData.TypeDataBuilder());
            functionBuilder
                    .returnType(transformed)
                    .refs(getTypeRefs(transformed, returnType));
        });

        // params
        functionTypeSymbol.params().ifPresent(params -> {
            List<Member> parameters = params.stream().map(param -> {
                Object transformedParamType = transform(param.typeDescriptor(), new TypeData.TypeDataBuilder());
                return memberBuilder
                        .name(param.getName().orElse(null))
                        .kind(Member.MemberKind.FIELD)
                        .type(transformedParamType)
                        .refs(getTypeRefs(transformedParamType, param.typeDescriptor()))
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
                    .refs(getTypeRefs(transformedRestParamType, restParam.typeDescriptor()))
                    .build();
            functionBuilder.restParameter(restParameter);
        });

        // resource path
        // TODO: Need a structured schema for resourcePath
        if (functionSymbol.kind().equals(SymbolKind.RESOURCE_METHOD)) {
            functionBuilder
                    .name(((ResourceMethodSymbol) functionSymbol).resourcePath().signature())
                    .accessor(functionSymbol.getName().orElse(""));
        }

        return functionBuilder.build();
    }

    private Member transformObjectFieldAsMember(String fieldName, ObjectFieldSymbol fieldSymbol) {
        TypeData.TypeDataBuilder attributeTypeDataBuilder = new TypeData.TypeDataBuilder();
        Object transformedAttributeType = transform(fieldSymbol.typeDescriptor(), attributeTypeDataBuilder);
        return (new Member.MemberBuilder())
                .name(fieldName)
                .kind(Member.MemberKind.FIELD)
                .type(transformedAttributeType)
                .refs(getTypeRefs(transformedAttributeType, fieldSymbol.typeDescriptor()))
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
                .refs(getTypeRefs(transformedMemberType, memberTypeDesc))
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

    private List<String> getTypeRefs(Object type, TypeSymbol typeDescriptor) {
        return type instanceof String ? TypeUtils.getTypeRefIds(typeDescriptor, moduleInfo) : List.of();
    }
}
