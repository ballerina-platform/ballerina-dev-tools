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
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClauseNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QueryExpressionNode;
import io.ballerina.compiler.syntax.tree.SelectClauseNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.flowmodelgenerator.core.utils.DefaultValueGeneratorUtil;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.TypeInfo;
import org.ballerinalang.diagramutil.connector.models.connector.types.ArrayType;
import org.ballerinalang.diagramutil.connector.models.connector.types.PrimitiveType;
import org.ballerinalang.diagramutil.connector.models.connector.types.RecordType;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates types of the data mapper model.
 *
 * @since 2.0.0
 */
public class DataMapManager {

    private final WorkspaceManager workspaceManager;
    private final SemanticModel semanticModel;
    private final Document document;
    private final Gson gson;

    public DataMapManager(WorkspaceManager workspaceManager, SemanticModel semanticModel, Document document) {
        this.workspaceManager = workspaceManager;
        this.semanticModel = semanticModel;
        this.document = document;
        this.gson = new Gson();
    }

    public JsonElement getTypes(JsonElement node, String propertyKey) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        Codedata codedata = flowNode.codedata();
        NodeKind nodeKind = codedata.node();
        if (nodeKind == NodeKind.VARIABLE) {
            String dataType = flowNode.properties().get(Property.TYPE_KEY).toSourceCode();
            Optional<Symbol> varSymbol = getSymbol(semanticModel.moduleSymbols(), dataType);
            if (varSymbol.isEmpty()) {
                throw new IllegalStateException("Symbol cannot be found for : " + dataType);
            }
            Type t = Type.fromSemanticSymbol(varSymbol.get());
            if (t == null) {
                throw new IllegalStateException("Type cannot be found for : " + propertyKey);
            }
            return gson.toJsonTree(t);
        } else if (nodeKind == NodeKind.FUNCTION_CALL) {
            Optional<Symbol> varSymbol = getSymbol(semanticModel.moduleSymbols(), codedata.symbol());
            if (varSymbol.isEmpty() || varSymbol.get().kind() != SymbolKind.FUNCTION) {
                throw new IllegalStateException("Symbol cannot be found for : " + codedata.symbol());
            }
            Optional<List<ParameterSymbol>> optParams = ((FunctionSymbol) varSymbol.get()).typeDescriptor().params();
            if (optParams.isEmpty()) {
                return new JsonObject();
            }
            Optional<Type> type = optParams.flatMap(params -> params.parallelStream()
                    .filter(param -> param.nameEquals(propertyKey)).findAny()).map(Type::fromSemanticSymbol);
            if (type.isEmpty()) {
                throw new IllegalStateException("Type cannot be found for : " + propertyKey);
            }
            return gson.toJsonTree(type.get());
        }
        return new JsonObject();
    }

    private Optional<Symbol> getSymbol(List<Symbol> symbols, String name) {
        return symbols.parallelStream()
                .filter(symbol -> symbol.nameEquals(name))
                .findAny();
    }

    public JsonElement getMappings(JsonElement node, LinePosition position, String propertyKey, Path filePath,
                                   String targetField, Project project) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        SourceModification modification;
        if (flowNode.codedata().node() == NodeKind.NEW_CONNECTION) {
            modification = applyConnection(flowNode, project, filePath);
        } else {
            modification = applyNode(flowNode, project, filePath, position);
        }
        SemanticModel newSemanticModel = modification.semanticModel();
        List<MappingPort> inputPorts = getInputPorts(newSemanticModel, modification.document(), position);
        inputPorts.sort(Comparator.comparing(mt -> mt.id));

        TargetNode targetNode = getTargetNode(modification.stNode(), targetField, flowNode.codedata().node(),
                propertyKey, newSemanticModel);
        if (targetNode == null) {
            return null;
        }

        Type type = Type.fromSemanticSymbol(targetNode.typeSymbol());
        String name = targetNode.name();
        MappingPort outputPort = getMappingPort(name, name, type);
        List<Mapping> mappings = new ArrayList<>();
        ExpressionNode expressionNode = targetNode.expressionNode();
        if (expressionNode != null) {
            String typeKind = type.getTypeName();
            if (typeKind.equals("record")) {
                generateRecordVariableDataMapping(expressionNode, mappings, name, newSemanticModel);
            } else if (typeKind.equals("array")) {
                generateArrayVariableDataMapping(expressionNode, mappings, name, newSemanticModel);
            }
        }
        return gson.toJsonTree(new Model(inputPorts, outputPort, mappings));
    }

    private TargetNode getTargetNode(Node parentNode, String targetField, NodeKind nodeKind, String propertyKey,
                                     SemanticModel semanticModel) {
        SyntaxKind kind = parentNode.kind();
        Optional<ExpressionNode> optInitializer;
        TypedBindingPatternNode typedBindingPattern;
        if (kind == SyntaxKind.LOCAL_VAR_DECL) {
            VariableDeclarationNode varDeclNode = (VariableDeclarationNode) parentNode;
            optInitializer = varDeclNode.initializer();
            typedBindingPattern = varDeclNode.typedBindingPattern();
        } else if (kind == SyntaxKind.MODULE_VAR_DECL) {
            ModuleVariableDeclarationNode moduleVarDeclNode = (ModuleVariableDeclarationNode) parentNode;
            optInitializer = moduleVarDeclNode.initializer();
            typedBindingPattern = moduleVarDeclNode.typedBindingPattern();
        } else {
            return null;
        }

        Optional<Symbol> optSymbol = semanticModel.symbol(parentNode);
        if (optSymbol.isEmpty()) {
            return null;
        }
        Symbol symbol = optSymbol.get();
        if (symbol.kind() != SymbolKind.VARIABLE) {
            return null;
        }
        VariableSymbol variableSymbol = (VariableSymbol) symbol;

        TypeSymbol typeSymbol = variableSymbol.typeDescriptor();
        if (optInitializer.isEmpty()) {
            return new TargetNode(typeSymbol, variableSymbol.getName().get(), null);
        }
        ExpressionNode initializer = optInitializer.get();
        if (initializer.kind() == SyntaxKind.FUNCTION_CALL && nodeKind == NodeKind.FUNCTION_CALL) {
            FunctionCallExpressionNode funcCallExprNode = (FunctionCallExpressionNode) initializer;
            Optional<Symbol> optFunctionSymbol = semanticModel.symbol(funcCallExprNode);
            if (optFunctionSymbol.isEmpty() || optFunctionSymbol.get().kind() != SymbolKind.FUNCTION) {
                return null;
            }
            FunctionSymbol functionSymbol = (FunctionSymbol) optFunctionSymbol.get();
            Optional<List<ParameterSymbol>> optParams = functionSymbol.typeDescriptor().params();
            if (optParams.isEmpty()) {
                return null;
            }
            return getTargetNodeForFunctionParam(optParams.get(), propertyKey, funcCallExprNode.arguments());
        } else if (initializer.kind() == SyntaxKind.CHECK_EXPRESSION && nodeKind == NodeKind.NEW_CONNECTION) {
            ExpressionNode expressionNode = ((CheckExpressionNode) initializer).expression();
            if (expressionNode.kind() != SyntaxKind.IMPLICIT_NEW_EXPRESSION) {
                return null;
            }
            TypeSymbol rawType = CommonUtils.getRawType(typeSymbol);
            if (rawType.kind() != SymbolKind.CLASS) {
                return null;
            }
            ClassSymbol classSymbol = (ClassSymbol) rawType;
            Optional<MethodSymbol> optInitMethodSymbol = classSymbol.initMethod();
            if (optInitMethodSymbol.isEmpty()) {
                return null;
            }
            MethodSymbol initMethodSymbol = optInitMethodSymbol.get();
            Optional<List<ParameterSymbol>> optParams = initMethodSymbol.typeDescriptor().params();
            if (optParams.isEmpty()) {
                return null;
            }
            ImplicitNewExpressionNode implicitNewExprNode = (ImplicitNewExpressionNode) expressionNode;
            Optional<ParenthesizedArgList> optParenthesizedArgList = implicitNewExprNode.parenthesizedArgList();
            if (optParenthesizedArgList.isEmpty()) {
                return new TargetNode(optParams.get().getFirst().typeDescriptor(), propertyKey, null);
            }
            return getTargetNodeForFunctionParam(optParams.get(), propertyKey,
                    optParenthesizedArgList.get().arguments());
        }

        if (targetField == null) {
            return new TargetNode(typeSymbol, variableSymbol.getName().get(), initializer);
        }

        if (initializer.kind() == SyntaxKind.QUERY_EXPRESSION) {
            if (typeSymbol.typeKind() != TypeDescKind.ARRAY) {
                return null;
            }
            typeSymbol = ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor();
            initializer = ((SelectClauseNode) ((QueryExpressionNode) initializer).resultClause()).expression();
        }

        if (initializer.kind() != SyntaxKind.MAPPING_CONSTRUCTOR) {
            return null;
        }
        typeSymbol = CommonUtils.getRawType(typeSymbol);
        if (typeSymbol.typeKind() != TypeDescKind.RECORD) {
            return null;
        }

        RecordTypeSymbol recordTypeSymbol = (RecordTypeSymbol) typeSymbol;
        MappingConstructorExpressionNode mappingCtrExprNode = (MappingConstructorExpressionNode) initializer;

        String[] splits = targetField.split("\\.");
        if (!splits[0].equals(typedBindingPattern.bindingPattern().toSourceCode().trim())) {
            return null;
        }

        Map<String, MappingFieldNode> mappingFieldsMap = convertMappingFieldsToMap(mappingCtrExprNode.fields());
        Map<String, RecordFieldSymbol> fieldDescriptors = recordTypeSymbol.fieldDescriptors();
        for (int i = 1; i < splits.length; i++) {
            String split = splits[i];
            MappingFieldNode mappingFieldNode = mappingFieldsMap.get(split);
            if (mappingFieldNode != null) {
                RecordFieldSymbol recordFieldSymbol = fieldDescriptors.get(split);
                if (recordFieldSymbol != null) {
                    return new TargetNode(recordFieldSymbol.typeDescriptor(), split,
                            ((SpecificFieldNode) mappingFieldNode).valueExpr().get());
                }
                break;
            }
        }
        return null;
    }

    private record TargetNode(TypeSymbol typeSymbol, String name, ExpressionNode expressionNode) {
    }

    private TargetNode getTargetNodeForFunctionParam(List<ParameterSymbol> paramSymbols, String propertyKey,
                                                     SeparatedNodeList<FunctionArgumentNode> args) {
        for (int i = 0; i < paramSymbols.size(); i++) {
            ParameterSymbol paramSymbol = paramSymbols.get(i);
            if (paramSymbol.getName().get().equals(propertyKey)) {
                return new TargetNode(paramSymbol.typeDescriptor(), propertyKey, getArgForParam(propertyKey, i, args));
            }
        }
        throw new IllegalStateException("Parameter is not available for : " + propertyKey);
    }

    private ExpressionNode getArgForParam(String param, int index, SeparatedNodeList<FunctionArgumentNode> args) {
        for (int i = 0; i < args.size(); i++) {
            FunctionArgumentNode arg = args.get(i);
            SyntaxKind kind = arg.kind();
            if (kind == SyntaxKind.POSITIONAL_ARG) {
                if (index == i) {
                    return ((PositionalArgumentNode) arg).expression();
                }
            } else if (kind == SyntaxKind.NAMED_ARG) {
                NamedArgumentNode namedArgumentNode = (NamedArgumentNode) arg;
                if (namedArgumentNode.argumentName().toSourceCode().equals(param)) {
                    return namedArgumentNode.expression();
                }
            }
        }
        return null;
    }

    private Map<String, MappingFieldNode> convertMappingFieldsToMap(SeparatedNodeList<MappingFieldNode> mappingFields) {
        Map<String, MappingFieldNode> mappingFieldNodeMap = new HashMap<>();
        mappingFields.forEach(mappingFieldNode -> {
            if (mappingFieldNode.kind() == SyntaxKind.SPECIFIC_FIELD) {
                SpecificFieldNode specificFieldNode = (SpecificFieldNode) mappingFieldNode;
                mappingFieldNodeMap.put(specificFieldNode.fieldName().toSourceCode(), specificFieldNode);
            }
        });
        return mappingFieldNodeMap;
    }

    private void generateRecordVariableDataMapping(ExpressionNode expressionNode, List<Mapping> mappings,
                                                   String name, SemanticModel semanticModel) {
        SyntaxKind exprKind = expressionNode.kind();
        if (exprKind == SyntaxKind.MAPPING_CONSTRUCTOR) {
            genMapping((MappingConstructorExpressionNode) expressionNode, mappings, name, semanticModel);
        } else {
            List<String> inputs = new ArrayList<>();
            genInputs(expressionNode, inputs);
            Mapping mapping = new Mapping(name, inputs, expressionNode.toSourceCode(),
                    getDiagnostics(expressionNode.lineRange(), semanticModel), new ArrayList<>());
            mappings.add(mapping);
        }
    }

    private void generateArrayVariableDataMapping(ExpressionNode expressionNode, List<Mapping> mappings,
                                                  String name, SemanticModel semanticModel) {
        SyntaxKind exprKind = expressionNode.kind();
        if (exprKind == SyntaxKind.LIST_CONSTRUCTOR) {
            genMapping((ListConstructorExpressionNode) expressionNode, mappings, name, semanticModel);
        } else if (exprKind == SyntaxKind.QUERY_EXPRESSION) {
            genMapping((QueryExpressionNode) expressionNode, mappings, name, semanticModel);
        } else {
            genMapping(expressionNode, name, mappings, semanticModel);
        }
    }

    private void genMapping(MappingConstructorExpressionNode mappingCtrExpr, List<Mapping> mappings, String name,
                            SemanticModel semanticModel) {
        for (MappingFieldNode field : mappingCtrExpr.fields()) {
            if (field.kind() == SyntaxKind.SPECIFIC_FIELD) {
                SpecificFieldNode f = (SpecificFieldNode) field;
                Optional<ExpressionNode> optFieldExpr = f.valueExpr();
                if (optFieldExpr.isEmpty()) {
                    continue;
                }
                ExpressionNode fieldExpr = optFieldExpr.get();
                SyntaxKind kind = fieldExpr.kind();
                if (kind == SyntaxKind.MAPPING_CONSTRUCTOR) {
                    genMapping((MappingConstructorExpressionNode) fieldExpr, mappings,
                            name + "." + f.fieldName().toSourceCode().trim(), semanticModel);
                } else if (kind == SyntaxKind.LIST_CONSTRUCTOR) {
                    genMapping((ListConstructorExpressionNode) fieldExpr, mappings, name + "." +
                            f.fieldName().toSourceCode().trim(), semanticModel);
                } else {
                    genMapping(fieldExpr, name + "." + f.fieldName().toSourceCode().trim(), mappings, semanticModel);
                }
            }
        }
    }

    private void genMapping(ListConstructorExpressionNode listCtrExpr, List<Mapping> mappings, String name,
                            SemanticModel semanticModel) {
        SeparatedNodeList<Node> expressions = listCtrExpr.expressions();
        int size = expressions.size();
        List<MappingElements> mappingElements = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            List<Mapping> elements = new ArrayList<>();
            Node expr = expressions.get(i);
            if (expr.kind() == SyntaxKind.MAPPING_CONSTRUCTOR) {
                genMapping((MappingConstructorExpressionNode) expr, elements, name + "." + i, semanticModel);
            } else if (expr.kind() == SyntaxKind.LIST_CONSTRUCTOR) {
                genMapping((ListConstructorExpressionNode) expr, elements, name + "." + i, semanticModel);
            } else {
                genMapping(expr, name + "." + i, elements, semanticModel);
            }
            mappingElements.add(new MappingElements(elements));
        }
        Mapping mapping = new Mapping(name, new ArrayList<>(), listCtrExpr.toSourceCode(),
                getDiagnostics(listCtrExpr.lineRange(), semanticModel), mappingElements);
        mappings.add(mapping);
    }

    private void genMapping(Node expr, String name, List<Mapping> elements, SemanticModel semanticModel) {
        List<String> inputs = new ArrayList<>();
        genInputs(expr, inputs);
        Mapping mapping = new Mapping(name, inputs, expr.toSourceCode(),
                getDiagnostics(expr.lineRange(), semanticModel), new ArrayList<>());
        elements.add(mapping);
    }

    private void genMapping(QueryExpressionNode queryExpr, List<Mapping> mappings, String name,
                            SemanticModel semanticModel) {
        ClauseNode clauseNode = queryExpr.resultClause();
        if (clauseNode.kind() != SyntaxKind.SELECT_CLAUSE) {
            return;
        }
        SelectClauseNode selectClauseNode = (SelectClauseNode) clauseNode;
        ExpressionNode expr = selectClauseNode.expression();
        if (expr.kind() == SyntaxKind.MAPPING_CONSTRUCTOR) {
            genMapping((MappingConstructorExpressionNode) expr, mappings, name, semanticModel);
        }
    }

    private void genInputs(Node expr, List<String> inputs) {
        SyntaxKind kind = expr.kind();
        if (kind == SyntaxKind.FIELD_ACCESS) {
            String source = expr.toSourceCode().trim();
            String[] split = source.split("\\[");
            if (split.length > 1) {
                inputs.add(split[0]);
            } else {
                inputs.add(source);
            }
        } else if (kind == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            inputs.add(expr.toSourceCode().trim());
        } else if (kind == SyntaxKind.BINARY_EXPRESSION) {
            BinaryExpressionNode binaryExpr = (BinaryExpressionNode) expr;
            genInputs(binaryExpr.lhsExpr(), inputs);
            genInputs(binaryExpr.rhsExpr(), inputs);
        } else if (kind == SyntaxKind.METHOD_CALL) {
            MethodCallExpressionNode methodCallExpr = (MethodCallExpressionNode) expr;
            genInputs(methodCallExpr.expression(), inputs);
        } else if (kind == SyntaxKind.MAPPING_CONSTRUCTOR) {
            MappingConstructorExpressionNode mappingCtrExpr = (MappingConstructorExpressionNode) expr;
            for (MappingFieldNode field : mappingCtrExpr.fields()) {
                SyntaxKind fieldKind = field.kind();
                if (fieldKind == SyntaxKind.SPECIFIC_FIELD) {
                    Optional<ExpressionNode> optFieldExpr = ((SpecificFieldNode) field).valueExpr();
                    optFieldExpr.ifPresent(expressionNode -> genInputs(expressionNode, inputs));
                } else {
                    genInputs(field, inputs);
                }
            }
        } else if (kind == SyntaxKind.INDEXED_EXPRESSION) {
            String source = expr.toSourceCode().trim();
            inputs.add(source.replace("[", ".").substring(0, source.length() - 1));
        }
    }

    private List<String> getDiagnostics(LineRange lineRange, SemanticModel semanticModel) {
        List<String> diagnosticMsgs = new ArrayList<>();
        for (Diagnostic diagnostic : semanticModel.diagnostics(lineRange)) {
            diagnosticMsgs.add(diagnostic.message());
        }
        return diagnosticMsgs;
    }

    private List<MappingPort> getInputPorts(SemanticModel semanticModel, Document document, LinePosition position) {
        List<MappingPort> mappingPorts = new ArrayList<>();

        List<Symbol> symbols = semanticModel.visibleSymbols(document, position);
        for (Symbol symbol : symbols) {
            SymbolKind kind = symbol.kind();
            if (kind == SymbolKind.VARIABLE) {
                Optional<String> optName = symbol.getName();
                if (optName.isEmpty()) {
                    continue;
                }
                Type type = Type.fromSemanticSymbol(symbol);
                MappingPort mappingPort = getMappingPort(optName.get(), optName.get(), type);
                if (mappingPort == null) {
                    continue;
                }
                VariableSymbol varSymbol = (VariableSymbol) symbol;
                if (varSymbol.qualifiers().contains(Qualifier.CONFIGURABLE)) {
                    mappingPort.category = "configurable";
                } else {
                    mappingPort.category = "variable";
                }
                mappingPorts.add(mappingPort);
            } else if (kind == SymbolKind.PARAMETER) {
                Optional<String> optName = symbol.getName();
                if (optName.isEmpty()) {
                    continue;
                }
                Type type = Type.fromSemanticSymbol(symbol);
                MappingPort mappingPort = getMappingPort(optName.get(), optName.get(), type);
                if (mappingPort == null) {
                    continue;
                }
                mappingPort.category = "parameter";
                mappingPorts.add(mappingPort);
            } else if (kind == SymbolKind.CONSTANT) {
                Type type = Type.fromSemanticSymbol(symbol);
                MappingPort mappingPort = getMappingPort(type.getTypeName(), type.getTypeName(), type);
                if (mappingPort == null) {
                    continue;
                }
                mappingPort.category = "constant";
                mappingPorts.add(mappingPort);
            }
        }
        return mappingPorts;
    }

    private MappingPort getMappingPort(String id, String name, Type type) {
        if (type.getTypeName().equals("record")) {
            RecordType recordType = (RecordType) type;
            TypeInfo typeInfo = type.getTypeInfo();
            MappingRecordPort recordPort = new MappingRecordPort(id, name, typeInfo != null ?
                    typeInfo.name : type.getTypeName(), type.getTypeName());
            for (Type field : recordType.fields) {
                recordPort.fields.add(getMappingPort(id + "." + field.getName(), field.getName(), field));
            }
            return recordPort;
        } else if (type instanceof PrimitiveType) {
            return new MappingPort(id, type.getName(), type.getTypeName(), type.getTypeName());
        } else if (type.getTypeName().equals("array")) {
            ArrayType arrayType = (ArrayType) type;
            MappingPort memberPort = getMappingPort(id, null, arrayType.memberType);
            MappingArrayPort arrayPort = new MappingArrayPort(id, name, memberPort == null ? "record" :
                    memberPort.typeName + "[]", type.getTypeName());
            arrayPort.setMember(memberPort);
            return arrayPort;
        } else {
            return null;
        }
    }

    private static final java.lang.reflect.Type mt = new TypeToken<List<Mapping>>() {
    }.getType();

    private Object genSourceForMappings(List<Mapping> mappings, String prevOutput) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (mappings.size() == 1) {
            Mapping firstMapping = mappings.getFirst();
            if (firstMapping.output().equals(prevOutput)) {
                List<MappingElements> elements = firstMapping.elements();
                if (elements == null) {
                    return genExprFromMapping(firstMapping);
                }
                List<Object> rawMappings = new ArrayList<>();
                for (MappingElements element : elements) {
                    genSourceFromMappingThroughElements(element, firstMapping.output(), rawMappings);
                }
                return rawMappings;
            }
        }
        for (Mapping mapping : mappings) {
            String output = mapping.output();
            String substring = output.substring(prevOutput.length() + 1);
            String[] splits = substring.split("\\.");
            Map<String, Object> cm = m;
            int length = splits.length;
            for (int i = 0; i < length; i++) {
                String split = splits[i];
                Object o = cm.get(split);
                if (o == null) {
                    if (i == length - 1) {
                        cm.put(split, genExprFromMapping(mapping));
                    } else {
                        Map<String, Object> temp = new LinkedHashMap<>();
                        cm.put(split, temp);
                        cm = temp;
                    }
                } else if (o instanceof Map<?, ?>) {
                    cm = ((Map<String, Object>) o);
                }
            }
        }
        return m;
    }

    private Object genExprFromMapping(Mapping mapping) {
        List<MappingElements> elements = mapping.elements();
        if (elements == null || elements.isEmpty()) {
            return mapping.expression();
        } else {
            List<Object> rawMappings = new ArrayList<>();
            for (MappingElements element : elements) {
                genSourceFromMappingThroughElements(element, mapping.output(), rawMappings);
            }
            return rawMappings;
        }
    }

    private void genSourceFromMappingThroughElements(MappingElements mappingElements, String prevOutput,
                                                     List<Object> elements) {
        List<Mapping> mappings = mappingElements.mappings();
        Map<String, Object> m = new LinkedHashMap<>();
        for (Mapping mapping : mappings) {
            String output = mapping.output();
            String substring = output.substring(prevOutput.length() + 1);
            if (substring.isEmpty()) {
                continue;
            }
            String[] splits = substring.split("\\.");
            int length = splits.length;
            String lastSplit = splits[length - 1];
            if (length == 1 && lastSplit.matches("^-?\\d+$")) {
                elements.add(mapping.expression());
                return;
            }
            Map<String, Object> currentMapping = m;
            String key = splits[0];
            for (int i = 0; i < length; i++) {
                String split = splits[i];
                Object o = currentMapping.get(key);
                if (o == null) {
                    if (!split.matches("^-?\\d+$")) {
                        if (i == length - 1) {
                            Object o1 = genExprFromMapping(mapping);
                            currentMapping.put(split, o1);
                        } else {
                            Map<String, Object> t = new LinkedHashMap<>();
                            currentMapping.put(split, t);
                            currentMapping = t;
                            key = split;
                        }
                    }
                } else if (o instanceof Map<?, ?>) {
                    currentMapping = (Map<String, Object>) o;
                }
            }
        }
        elements.add(m);
    }

    public String getSource(JsonElement mp, JsonElement fNode, String targetField) {
        FlowNode flowNode = gson.fromJson(fNode, FlowNode.class);
        List<Mapping> fieldMapping = gson.fromJson(mp, mt);
        if (flowNode.codedata().node() != NodeKind.VARIABLE) {
            return "";
        }
        String mappingSource = genSource(genSourceForMappings(fieldMapping, getVariableName(flowNode)));
        Optional<Property> optProperty = flowNode.getProperty("expression");
        if (optProperty.isEmpty()) {
            return mappingSource;
        }
        Property property = optProperty.get();
        String source = property.toSourceCode();
        if (targetField == null) {
            if (source.matches("^from.*in.*select.*$")) {
                String[] split = source.split("select");
                return split[0] + " select " + mappingSource + ";";
            }
        } else {
            String fieldsPattern = getFieldsPattern(targetField);
            if (source.matches(".*" + fieldsPattern + "\\s*:\\s*from.*in.*select.*$")) {
                String[] split = source.split(fieldsPattern + "\\s*:\\s*from");
                String newSource = split[0] + fieldsPattern + ": from";
                String[] splitBySelect = split[1].split("select");
                return newSource + splitBySelect[0] + "select" + splitBySelect[1].replaceFirst("\\{.*?}",
                        mappingSource);
            }
        }
        return mappingSource;
    }

    private String getVariableName(FlowNode flowNode) {
        Optional<Property> optProperty = flowNode.getProperty("variable");
        if (optProperty.isEmpty()) {
            return "";
        }
        return optProperty.get().toSourceCode();
    }

    private String getFieldsPattern(String targetField) {
        String[] splits = targetField.split("\\.");
        StringBuilder pattern = new StringBuilder();
        int length = splits.length;
        for (int i = 1; i < length - 1; i++) {
            String split = splits[i];
            if (split.matches("^-?\\d+$")) {
                continue;
            }
            pattern.append(split).append(".*");
        }
        pattern.append(splits[length - 1]);
        return pattern.toString();
    }

    private String genSource(Object sourceObj) {
        if (sourceObj instanceof Map<?, ?>) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            Map<String, Object> mappings = (Map<String, Object>) sourceObj;
            int len = mappings.entrySet().size();
            int i = 0;
            for (Map.Entry<String, Object> stringObjectEntry : mappings.entrySet()) {
                sb.append(stringObjectEntry.getKey()).append(":");
                sb.append(genSource(stringObjectEntry.getValue()));

                if (i != len - 1) {
                    sb.append(",");
                }
                i = i + 1;
            }
            sb.append("}");
            return sb.toString();
        } else if (sourceObj instanceof List<?>) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            List<Object> objects = (List<Object>) sourceObj;
            int len = objects.size();
            int i = 0;
            for (Object object : objects) {
                sb.append(genSource(object));

                if (i != len - 1) {
                    sb.append(",");
                }
                i = i + 1;
            }
            sb.append("]");
            return sb.toString();
        } else {
            return sourceObj.toString();
        }
    }

    public String getQuery(JsonElement fNode, String targetField, Path filePath, LinePosition position,
                           Project project) {
        FlowNode flowNode = gson.fromJson(fNode, FlowNode.class);
        SourceModification modification = applyNode(flowNode, project, filePath, position);

        TargetNode targetNode = getTargetNode(modification.stNode(), targetField, flowNode.codedata().node(), null,
                modification.semanticModel());
        if (targetNode == null) {
            return "";
        }

        TypeSymbol targetTypeSymbol = CommonUtils.getRawType(targetNode.typeSymbol());
        if (targetTypeSymbol.typeKind() != TypeDescKind.ARRAY) {
            return "";
        }
        TypeSymbol typeSymbol = CommonUtils.getRawType(((ArrayTypeSymbol) targetTypeSymbol).memberTypeDescriptor());
        if (typeSymbol.typeKind() != TypeDescKind.RECORD) {
            return "";
        }

        String query = getQuerySource(targetNode.expressionNode(), (RecordTypeSymbol) typeSymbol);
        if (targetField == null) {
            return query;
        }
        if (flowNode.codedata().node() != NodeKind.VARIABLE) {
            return query;
        }
        Optional<Property> optProperty = flowNode.getProperty(Property.EXPRESSION_KEY);
        if (optProperty.isEmpty()) {
            return query;
        }
        Property property = optProperty.get();
        String expr = property.toSourceCode();
        return expr.replace(targetNode.expressionNode().toSourceCode(), query);
    }

    private SourceModification applyNode(FlowNode flowNode, Project project, Path filePath, LinePosition position) {
        SourceBuilder sourceBuilder = new SourceBuilder(flowNode, this.workspaceManager, filePath);
        String source = NodeBuilder.getNodeFromKind(flowNode.codedata().node())
                .toSource(sourceBuilder).entrySet().stream().iterator().next().getValue().get(0).getNewText();
        TextDocument textDocument = document.textDocument();
        int startTextPosition = textDocument.textPositionFrom(position);
        io.ballerina.tools.text.TextEdit textEdit =
                io.ballerina.tools.text.TextEdit.from(TextRange.from(startTextPosition,
                        0), source);
        io.ballerina.tools.text.TextEdit[] textEdits = {textEdit};
        TextDocument modifiedTextDoc = textDocument.apply(TextDocumentChange.from(textEdits));
        Document modifiedDoc =
                project.duplicate().currentPackage().module(document.module().moduleId())
                        .document(document.documentId()).modify().withContent(String.join(System.lineSeparator(),
                                modifiedTextDoc.textLines())).apply();

        SemanticModel newSemanticModel = modifiedDoc.module().packageInstance().getCompilation()
                .getSemanticModel(modifiedDoc.module().moduleId());
        LinePosition startLine = modifiedTextDoc.linePositionFrom(startTextPosition);
        LinePosition endLine = modifiedTextDoc.linePositionFrom(startTextPosition + source.length());
        Range range = new Range(new Position(startLine.line(), startLine.offset()),
                new Position(endLine.line(), endLine.offset()));
        NonTerminalNode stNode = CommonUtil.findNode(range, modifiedDoc.syntaxTree());

        return new SourceModification(source, modifiedDoc, newSemanticModel, stNode);
    }

    private SourceModification applyConnection(FlowNode flowNode, Project project, Path filePath) {
        SourceBuilder sourceBuilder = new SourceBuilder(flowNode, this.workspaceManager, filePath);
        Path connectionPath = workspaceManager.projectRoot(filePath).resolve("connections.bal");
        List<TextEdit> connectionTextEdits =
                NodeBuilder.getNodeFromKind(flowNode.codedata().node()).toSource(sourceBuilder).get(connectionPath);
        Document document = workspaceManager.document(connectionPath).orElseThrow();
        TextDocument textDocument = document.textDocument();
        io.ballerina.tools.text.TextEdit[] textEdits = new io.ballerina.tools.text.TextEdit[connectionTextEdits.size()];
        for (int i = 0; i < connectionTextEdits.size(); i++) {
            TextEdit connectionTextEdit = connectionTextEdits.get(i);
            Position start = connectionTextEdit.getRange().getStart();
            int startTextPosition = textDocument.textPositionFrom(LinePosition.from(start.getLine(),
                    start.getCharacter()));
            Position end = connectionTextEdit.getRange().getEnd();
            int endTextPosition = textDocument.textPositionFrom(LinePosition.from(end.getLine(), end.getCharacter()));
            io.ballerina.tools.text.TextEdit textEdit =
                    io.ballerina.tools.text.TextEdit.from(TextRange.from(startTextPosition,
                            endTextPosition - startTextPosition), connectionTextEdit.getNewText());
            textEdits[i] = textEdit;
        }
        TextDocument modifiedTextDoc = textDocument.apply(TextDocumentChange.from(textEdits));
        Document modifiedDoc =
                project.duplicate().currentPackage().module(document.module().moduleId())
                        .document(document.documentId()).modify().withContent(String.join(System.lineSeparator(),
                                modifiedTextDoc.textLines())).apply();

        Optional<Property> optVariable = flowNode.getProperty("variable");
        if (optVariable.isEmpty()) {
            throw new IllegalStateException("Variable cannot be found for the connection");
        }
        SemanticModel newSemanticModel = modifiedDoc.module().packageInstance().getCompilation()
                .getSemanticModel(modifiedDoc.module().moduleId());
        return new SourceModification("", modifiedDoc, newSemanticModel, connectionNode(modifiedDoc,
                optVariable.get().toSourceCode()));
    }

    private Node connectionNode(Document document, String connectionName) {
        ModulePartNode modulePartNode = document.syntaxTree().rootNode();
        NodeList<ModuleMemberDeclarationNode> members = modulePartNode.members();
        for (ModuleMemberDeclarationNode member : members) {
            if (member.kind() == SyntaxKind.MODULE_VAR_DECL) {
                ModuleVariableDeclarationNode varDecl = (ModuleVariableDeclarationNode) member;
                if (varDecl.typedBindingPattern().bindingPattern().toSourceCode().trim().equals(connectionName)) {
                    return varDecl;
                }
            }
        }
        return null;
    }

    private record SourceModification(String source, Document document, SemanticModel semanticModel, Node stNode) {
    }

    private String getQuerySource(NonTerminalNode inputExpr, RecordTypeSymbol recordTypeSymbol) {
        String name = "item";
        SyntaxKind kind = inputExpr.kind();
        if (kind == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            name = inputExpr.toSourceCode() + "Item";
        } else if (kind == SyntaxKind.FIELD_ACCESS) {
            FieldAccessExpressionNode fieldAccessExpr = (FieldAccessExpressionNode) inputExpr;
            name = fieldAccessExpr.fieldName().toSourceCode() + "Item";
        }

        StringBuilder sb = new StringBuilder("from var " + name + " in " + inputExpr.toSourceCode());
        sb.append(" ").append(SyntaxKind.SELECT_KEYWORD.stringValue()).append(" ");
        sb.append(SyntaxKind.OPEN_BRACE_TOKEN.stringValue());
        List<String> keys = new ArrayList<>(recordTypeSymbol.fieldDescriptors().keySet());
        int size = keys.size();
        for (int i = 0; i < size - 1; i++) {
            sb.append(keys.get(i)).append(": ").append(SyntaxKind.COMMA_TOKEN.stringValue());
        }
        sb.append(keys.get(size - 1)).append(": ");
        sb.append(SyntaxKind.CLOSE_BRACE_TOKEN.stringValue());
        return sb.toString();
    }

    public JsonElement getVisualizableProperties(JsonElement node, Project project, Path filePath,
                                                 LinePosition position) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        List<String> visualizableProperties = new ArrayList<>();
        NodeKind nodeKind = flowNode.codedata().node();
        if (nodeKind == NodeKind.VARIABLE) {
            SourceModification sourceModification = applyNode(flowNode, project, filePath, position);
            Node stNode = sourceModification.stNode();
            if (stNode.kind() != SyntaxKind.LOCAL_VAR_DECL) {
                throw new IllegalStateException("Node is not a variable declaration");
            }
            Optional<Symbol> optVarSymbol = sourceModification.semanticModel().symbol(stNode);
            if (optVarSymbol.isEmpty()) {
                throw new IllegalStateException("Symbol cannot be found for the variable declaration");
            }
            VariableSymbol variableSymbol = (VariableSymbol) optVarSymbol.get();
            if (isEffectiveRecordType(variableSymbol.typeDescriptor())) {
                visualizableProperties.add("expression");
            }
        } else if (nodeKind == NodeKind.NEW_CONNECTION) {
            SourceModification sourceModification = applyConnection(flowNode, project, filePath);
            Optional<Property> optVariable = flowNode.getProperty("variable");
            if (optVariable.isEmpty()) {
                throw new IllegalStateException("Variable cannot be found for the connection");
            }
            List<Symbol> symbols = sourceModification.semanticModel().moduleSymbols();
            String variableName = optVariable.get().toSourceCode();
            Optional<Symbol> optVariableSymbol = symbols.parallelStream()
                    .filter(symbol -> symbol.getName().isPresent() && symbol.getName().get().equals(variableName))
                    .findAny();
            if (optVariableSymbol.isEmpty()) {
                throw new IllegalStateException("Symbol cannot be found for the connection variable");
            }

            VariableSymbol variableSymbol = (VariableSymbol) optVariableSymbol.get();
            TypeSymbol typeSymbol = CommonUtils.getRawType(variableSymbol.typeDescriptor());
            if (typeSymbol.kind() != SymbolKind.CLASS) {
                throw new IllegalStateException("Connection symbol is not a class symbol");
            }
            ClassSymbol classSymbol = (ClassSymbol) typeSymbol;
            Optional<MethodSymbol> optInitMethodSymbol = classSymbol.initMethod();
            if (optInitMethodSymbol.isEmpty()) {
                throw new IllegalStateException("Init method cannot be found for the connection class");
            }
            MethodSymbol initMethodSymbol = optInitMethodSymbol.get();
            Optional<List<ParameterSymbol>> optParams = initMethodSymbol.typeDescriptor().params();
            if (optParams.isPresent()) {
                List<ParameterSymbol> params = optParams.get();
                for (ParameterSymbol param : params) {
                    if (isEffectiveRecordType(param.typeDescriptor())) {
                        visualizableProperties.add(param.getName().get());
                    }
                }
            }
        }
        return gson.toJsonTree(visualizableProperties);
    }

    private boolean isEffectiveRecordType(TypeSymbol typeSymbol) {
        TypeSymbol rawTypeSymbol = CommonUtils.getRawType(typeSymbol);
        TypeDescKind kind = rawTypeSymbol.typeKind();
        if (kind == TypeDescKind.ARRAY) {
            return isEffectiveRecordType(((ArrayTypeSymbol) rawTypeSymbol).memberTypeDescriptor());
        }
        return kind == TypeDescKind.RECORD;
    }

    public String addElement(JsonElement node, String propertyKey, Path filePath, String targetField, Project project,
                             LinePosition position) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        if (flowNode.codedata().node() != NodeKind.VARIABLE) {
            return "";
        }
        Optional<Property> optProperty = flowNode.getProperty(propertyKey);
        if (optProperty.isEmpty()) {
            return "";
        }
        Property property = optProperty.get();
        String source = property.toSourceCode();

        SourceModification sourceModification = applyNode(flowNode, project, filePath, position);
        Node stNode = sourceModification.stNode();
        if (stNode.kind() != SyntaxKind.LOCAL_VAR_DECL) {
            return "";
        }
        Optional<Symbol> symbol = sourceModification.semanticModel().symbol(stNode);
        if (symbol.isEmpty()) {
            return "";
        }
        TypeSymbol targetType = getTargetType(((VariableSymbol) symbol.get()).typeDescriptor(), targetField);
        if (targetType == null) {
            return "";
        }
        if (targetType.typeKind() == TypeDescKind.ARRAY) {
            targetType = ((ArrayTypeSymbol) targetType).memberTypeDescriptor();
        }
        String defaultVal = DefaultValueGeneratorUtil.getDefaultValueForType(targetType);
        if (source.equals("[]")) {
            return "[" + defaultVal + "]";
        }
        String fieldsPattern = getFieldsPattern(targetField);
        if (source.matches("(?s).*" + fieldsPattern + "\\s*:\\s*\\[.*$")) {
            String[] splits = source.split(".*" + fieldsPattern + "\\s*:\\s*\\[");
            String lastSplit = splits[1];
            if (!lastSplit.trim().startsWith("]")) {
                defaultVal = ", " + defaultVal;
            }
            String firstSplit = source.substring(0, source.length() - lastSplit.length());
            StringBuilder sb = new StringBuilder(firstSplit);
            int openBraceCount = 0;
            for (int i = 0; i < lastSplit.length(); i++) {
                char c = lastSplit.charAt(i);
                if (c == '[') {
                    openBraceCount = openBraceCount + 1;
                } else if (c == ']') {
                    if (openBraceCount == 0) {
                        sb.append(defaultVal);
                        sb.append(lastSplit.substring(i));
                        break;
                    } else {
                        openBraceCount = openBraceCount - 1;
                    }
                }
                sb.append(c);
            }
            return sb.toString();
        }
        return "";
    }

    private TypeSymbol getTargetType(TypeSymbol typeSymbol, String targetField) {
        if (targetField == null || targetField.isEmpty()) {
            return typeSymbol;
        }
        String[] splits = targetField.split("\\.");
        if (splits.length == 1) {
            return typeSymbol;
        }

        TypeSymbol targetType = typeSymbol;
        for (int i = 1; i < splits.length; i++) {
            targetType = CommonUtils.getRawType(targetType);
            String split = splits[i];
            if (split.matches("\\d+")) {
                if (targetType.typeKind() != TypeDescKind.ARRAY) {
                    return null;
                }
                targetType = ((ArrayTypeSymbol) targetType).memberTypeDescriptor();
            } else {
                if (targetType.typeKind() != TypeDescKind.RECORD) {
                    return null;
                }
                RecordFieldSymbol recordFieldSymbol = ((RecordTypeSymbol) targetType).fieldDescriptors().get(split);
                targetType = recordFieldSymbol.typeDescriptor();
            }
        }
        return targetType;
    }

    private record Model(List<MappingPort> inputs, MappingPort output, List<Mapping> mappings) {

    }

    private record Mapping(String output, List<String> inputs, String expression, List<String> diagnostics,
                           List<MappingElements> elements) {

    }

    private record MappingElements(List<Mapping> mappings) {

    }

    private static class MappingPort {
        String id;
        String variableName;
        String typeName;
        String kind;
        String category;

        MappingPort(String id, String variableName, String typeName, String kind) {
            this.id = id;
            this.variableName = variableName;
            this.typeName = typeName;
            this.kind = kind;
        }

        String getCategory() {
            return this.category;
        }

        String getKind() {
            return this.kind;
        }

        void setKind(String kind) {
            this.kind = kind;
        }

        String getVariableName() {
            return this.variableName;
        }

        void setVariableName(String variableName) {
            this.variableName = variableName;
        }
    }

    private static class MappingRecordPort extends MappingPort {
        List<MappingPort> fields = new ArrayList<>();

        MappingRecordPort(String id, String variableName, String typeName, String kind) {
            super(id, variableName, typeName, kind);
        }
    }

    private static class MappingArrayPort extends MappingPort {
        MappingPort member;

        MappingArrayPort(String id, String variableName, String typeName, String kind) {
            super(id, variableName, typeName, kind);
        }

        MappingPort getMember() {
            return this.member;
        }

        void setMember(MappingPort member) {
            this.member = member;
        }
    }
}
