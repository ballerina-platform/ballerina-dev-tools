/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.EnumSymbol;
import io.ballerina.compiler.api.symbols.ErrorTypeSymbol;
import io.ballerina.compiler.api.symbols.FutureTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.StreamTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TableTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.flowmodelgenerator.core.model.ModuleInfo;
import io.ballerina.flowmodelgenerator.core.model.TypeData;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.flowmodelgenerator.core.utils.SourceCodeGenerator;
import io.ballerina.flowmodelgenerator.core.utils.TypeTransformer;
import io.ballerina.flowmodelgenerator.core.utils.TypeUtils;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manage creation, retrieving and updating operations related to types.
 *
 * @since 2.0.0
 */
public class TypesManager {
    private static final Gson gson = new Gson();
    private final Module module;
    private final Document typeDocument;
    private static final List<SymbolKind> supportedSymbolKinds = List.of(SymbolKind.TYPE_DEFINITION, SymbolKind.ENUM,
            SymbolKind.CLASS, SymbolKind.TYPE);
    private static final List<SymbolKind> supportedGraphqlSymbolKinds = List.of(SymbolKind.TYPE_DEFINITION,
            SymbolKind.ENUM, SymbolKind.SERVICE_DECLARATION, SymbolKind.CLASS, SymbolKind.TYPE);

    public TypesManager(Document typeDocument) {
        this.typeDocument = typeDocument;
        this.module = typeDocument.module();
    }

    public JsonElement getAllTypes() {
        SemanticModel semanticModel = this.module.getCompilation().getSemanticModel();
        Map<String, Symbol> symbolMap = semanticModel.moduleSymbols().stream()
                .filter(s -> supportedSymbolKinds.contains(s.kind()))
                .collect(Collectors.toMap(symbol -> symbol.getName().orElse(""), symbol -> symbol));

        // Now we have all the defined types in the module scope
        // Now we need to get foreign types that we have defined members of the types
        // e.g: ballerina\time:UTC in Person record as a type of field `dateOfBirth`
        new HashMap<>(symbolMap).forEach((key, element) -> {
            if (element.kind() != SymbolKind.TYPE_DEFINITION) {
                return;
            }
            TypeSymbol typeSymbol = ((TypeDefinitionSymbol) element).typeDescriptor();
            addMemberTypes(typeSymbol, symbolMap);
        });

        List<Object> allTypes = symbolMap.values().stream().map(this::getTypeData).toList();

        return gson.toJsonTree(allTypes);
    }

    public JsonElement getType(Document document, LinePosition linePosition) {
        SemanticModel semanticModel = this.module.getCompilation().getSemanticModel();
        Optional<Symbol> symbol = semanticModel.symbol(document, linePosition);
        if (symbol.isEmpty() || !supportedGraphqlSymbolKinds.contains(symbol.get().kind())) {
            return null;
        }

        Object type = getTypeData(symbol.get());

        Map<String, Object> refs = new HashMap<>();
        if (symbol.get().kind() == SymbolKind.SERVICE_DECLARATION) {
            addDependencyTypes((ServiceDeclarationSymbol) symbol.get(), refs);
        } else {
            TypeSymbol typeDescriptor = getTypeDescriptor(symbol.get());
            if (typeDescriptor != null) {
                addDependencyTypes(typeDescriptor, refs);
            }
        }

        return gson.toJsonTree(new TypeDataWithRefs(type, refs.values().stream().toList()));
    }

    public TypeDataWithRefs getTypeDataWithRefs(TypeDefinitionSymbol typeDefSymbol) {
        Object type = getTypeData(typeDefSymbol);
        Map<String, Object> refs = new HashMap<>();
        TypeSymbol typeDescriptor = getTypeDescriptor(typeDefSymbol);
        if (typeDescriptor != null) {
            addDependencyTypes(typeDescriptor, refs);
        }
        return new TypeDataWithRefs(type, refs.values().stream().toList());
    }

    public JsonElement updateType(Path filePath, TypeData typeData) {
        List<TextEdit> textEdits = new ArrayList<>();
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        textEditsMap.put(filePath, textEdits);

        // Regenerate code snippet for the type
        SourceCodeGenerator sourceCodeGenerator = new SourceCodeGenerator();
        String codeSnippet = sourceCodeGenerator.generateCodeSnippetForType(typeData);

        SyntaxTree syntaxTree = this.typeDocument.syntaxTree();
        LineRange lineRange = typeData.codedata().lineRange();
        if (lineRange == null) {
            ModulePartNode modulePartNode = syntaxTree.rootNode();
            LinePosition startPos = LinePosition.from(modulePartNode.lineRange().endLine().line() + 1, 0);
            textEdits.add(new TextEdit(CommonUtils.toRange(startPos), codeSnippet));
        } else {
            NonTerminalNode node = CommonUtil.findNode(CommonUtils.toRange(lineRange), syntaxTree);
            textEdits.add(new TextEdit(CommonUtils.toRange(node.lineRange()), codeSnippet));
        }

        return gson.toJsonTree(textEditsMap);
    }

    private void addMemberTypes(TypeSymbol typeSymbol, Map<String, Symbol> symbolMap) {
        // Record
        switch (typeSymbol.typeKind()) {
            case RECORD -> {
                RecordTypeSymbol recordTypeSymbol = (RecordTypeSymbol) typeSymbol;

                // Type inclusions
                List<TypeSymbol> inclusions = recordTypeSymbol.typeInclusions();
                inclusions.forEach(inc -> {
                    addToMapIfForeignAndNotAdded(symbolMap, inc);
                });

                // Rest field
                Optional<TypeSymbol> restTypeDescriptor = recordTypeSymbol.restTypeDescriptor();
                if (restTypeDescriptor.isPresent()) {
                    TypeSymbol restType = restTypeDescriptor.get();
                    addToMapIfForeignAndNotAdded(symbolMap, restType);
                }

                // Field members
                Map<String, RecordFieldSymbol> fieldSymbolMap = recordTypeSymbol.fieldDescriptors();
                fieldSymbolMap.forEach((key, field) -> {
                    TypeSymbol ts = field.typeDescriptor();
                    if (ts.typeKind() == TypeDescKind.ARRAY || ts.typeKind() == TypeDescKind.UNION) {
                        addMemberTypes(ts, symbolMap);
                    } else {
                        addToMapIfForeignAndNotAdded(symbolMap, ts);
                    }
                });
            }
            case UNION -> {
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
                List<TypeSymbol> unionMembers = unionTypeSymbol.memberTypeDescriptors();
                unionMembers.forEach(member -> {
                    if (member.typeKind() == TypeDescKind.ARRAY) {
                        addMemberTypes(member, symbolMap);
                    } else {
                        addToMapIfForeignAndNotAdded(symbolMap, member);
                    }
                });
            }
            case ARRAY -> {
                ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) typeSymbol;
                TypeSymbol arrMemberTypeDesc = arrayTypeSymbol.memberTypeDescriptor();
                if (arrMemberTypeDesc.typeKind() == TypeDescKind.ARRAY
                        || arrMemberTypeDesc.typeKind() == TypeDescKind.UNION) {
                    addMemberTypes(arrMemberTypeDesc, symbolMap);
                } else {
                    addToMapIfForeignAndNotAdded(symbolMap, arrMemberTypeDesc);
                }
            }
            default -> {
            }
        }
    }

    private Object getTypeData(Symbol symbol) {
        TypeTransformer typeTransformer = new TypeTransformer(this.module);
        return switch (symbol.kind()) {
            case TYPE_DEFINITION -> typeTransformer.transform((TypeDefinitionSymbol) symbol);
            case CLASS -> typeTransformer.transform((ClassSymbol) symbol);
            case ENUM -> typeTransformer.transform((EnumSymbol) symbol);
            case SERVICE_DECLARATION -> typeTransformer.transform((ServiceDeclarationSymbol) symbol);
            case TYPE -> getTypeData(((TypeReferenceTypeSymbol) symbol).definition());
            default ->  null;
        };
    }

    // Get type descriptor from the symbol
    private TypeSymbol getTypeDescriptor(Symbol symbol) {
        return switch (symbol.kind()) {
            case TYPE_DEFINITION -> ((TypeDefinitionSymbol) symbol).typeDescriptor();
            case CLASS -> ((ClassSymbol) symbol);
            default -> null;
        };
    }

    private void addToMapIfForeignAndNotAdded(Map<String, Symbol> foreignSymbols, TypeSymbol type) {
        if (type.typeKind() != TypeDescKind.TYPE_REFERENCE
                || type.getName().isEmpty()
                || type.getModule().isEmpty()) {
            return;
        }

        String name = type.getName().get();

        ModuleInfo moduleInfo = ModuleInfo.from(this.module.descriptor());
        if (CommonUtils.isWithinPackage(type, moduleInfo)) {
            return;
        }

        String typeName = TypeUtils.generateReferencedTypeId(type, moduleInfo);
        if (!foreignSymbols.containsKey(name)) {
            foreignSymbols.put(typeName, type);
        }
    }

    private void addDependencyTypes(ServiceDeclarationSymbol serviceDeclarationSymbol, Map<String, Object> references) {
        // attributes
        serviceDeclarationSymbol.fieldDescriptors().forEach((key, field) -> {
            addDependencyTypes(field.typeDescriptor(), references);
        });

        // methods
        serviceDeclarationSymbol.methods().forEach((key, method) -> {
            // params
            method.typeDescriptor().params().ifPresent(params -> params.forEach(param -> {
                addDependencyTypes(param.typeDescriptor(), references);
            }));

            // return type
            method.typeDescriptor().returnTypeDescriptor().ifPresent(returnType -> {
                addDependencyTypes(returnType, references);
            });

            // rest param
            method.typeDescriptor().restParam().ifPresent(restParam -> {
                addDependencyTypes(restParam.typeDescriptor(), references);
            });
        });
    }

    private void addDependencyTypes(TypeSymbol typeSymbol, Map<String, Object> references) {
        switch (typeSymbol.typeKind()) {
            case RECORD -> {
                RecordTypeSymbol recordTypeSymbol = (RecordTypeSymbol) typeSymbol;

                // type inclusions
                recordTypeSymbol.typeInclusions().forEach(includedType -> {
                    addDependencyTypes(includedType, references);
                });

                // members
                recordTypeSymbol.fieldDescriptors().forEach((key, field) -> {
                    addDependencyTypes(field.typeDescriptor(), references);
                });

                // rest member
                if (recordTypeSymbol.restTypeDescriptor().isPresent()) {
                    addDependencyTypes(recordTypeSymbol.restTypeDescriptor().get(), references);
                }
            }
            case ARRAY -> {
                addDependencyTypes(((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor(), references);
            }
            case UNION -> {
                ((UnionTypeSymbol) typeSymbol).memberTypeDescriptors().forEach(memberTypes -> {
                    addDependencyTypes(memberTypes, references);
                });
            }
            case ERROR -> {
                addDependencyTypes(((ErrorTypeSymbol) typeSymbol).detailTypeDescriptor(), references);
            }
            case FUTURE -> {
                Optional<TypeSymbol> typeParam = ((FutureTypeSymbol) typeSymbol).typeParameter();
                if (typeParam.isEmpty()) {
                    return;
                }
                addDependencyTypes(typeParam.get(), references);
            }
            case MAP -> {
                TypeSymbol typeParam = ((MapTypeSymbol) typeSymbol).typeParam();
                addDependencyTypes(typeParam, references);
            }
            case STREAM -> {
                TypeSymbol typeParam = ((StreamTypeSymbol) typeSymbol).typeParameter();
                addDependencyTypes(typeParam, references);
            }
            case INTERSECTION -> {
                ((IntersectionTypeSymbol) typeSymbol).memberTypeDescriptors().forEach(memberTypes -> {
                    addDependencyTypes(memberTypes, references);
                });
            }
            case TABLE -> {
                TableTypeSymbol tableTypeSymbol = (TableTypeSymbol) typeSymbol;
                addDependencyTypes(tableTypeSymbol.rowTypeParameter(), references);
                if (tableTypeSymbol.keyConstraintTypeParameter().isPresent()) {
                    addDependencyTypes(tableTypeSymbol.keyConstraintTypeParameter().get(), references);
                }
            }
            case OBJECT -> {
                ObjectTypeSymbol objectTypeSymbol = (ObjectTypeSymbol) typeSymbol;

                // inclusions
                objectTypeSymbol.typeInclusions().forEach(includedType -> {
                    addDependencyTypes(includedType, references);
                });

                // attributes
                objectTypeSymbol.fieldDescriptors().forEach((key, field) -> {
                    addDependencyTypes(field.typeDescriptor(), references);
                });

                // methods
                objectTypeSymbol.methods().forEach((key, method) -> {
                    // params
                    method.typeDescriptor().params().ifPresent(params -> params.forEach(param -> {
                                addDependencyTypes(param.typeDescriptor(), references);
                    }));

                    // return type
                    method.typeDescriptor().returnTypeDescriptor().ifPresent(returnType -> {
                        addDependencyTypes(returnType, references);
                    });

                    // rest param
                    method.typeDescriptor().restParam().ifPresent(restParam -> {
                        addDependencyTypes(restParam.typeDescriptor(), references);
                    });
                });
            }
            case FUNCTION, TUPLE -> {
                // TODO: Implement
            }
            case TYPE_REFERENCE -> {
                Symbol definition = ((TypeReferenceTypeSymbol) typeSymbol).definition();
                ModuleInfo moduleInfo = ModuleInfo.from(this.module.descriptor());
                String typeName = TypeUtils.generateReferencedTypeId(typeSymbol, moduleInfo);
                references.putIfAbsent(typeName, getTypeData(definition));
                if (CommonUtils.isWithinPackage(definition, moduleInfo)) {
                    addDependencyTypes(((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor(), references);
                }
            }
            default -> {
            }
        }
    }

    public record TypeDataWithRefs(Object type, List<Object> refs) {
    }
}
