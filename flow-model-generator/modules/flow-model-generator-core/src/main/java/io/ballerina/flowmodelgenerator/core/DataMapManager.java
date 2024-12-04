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
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ClauseNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.QueryExpressionNode;
import io.ballerina.compiler.syntax.tree.SelectClauseNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
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
import java.util.*;

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
        // TODO: add tests for enum
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        SourceModification modification = applyNode(flowNode, project, filePath, position);
        SemanticModel newSemanticModel = modification.semanticModel();
        List<MappingPort> inputPorts = getInputPorts(newSemanticModel, modification.document(), position);
        inputPorts.sort(Comparator.comparing(mt -> mt.id));

        TargetNode targetNode = getTargetNode(modification.stNode(), targetField, newSemanticModel);
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
        return gson.toJsonTree(new Model(inputPorts, outputPort, mappings, modification.source()));
    }

    private TargetNode getTargetNode(Node parentNode, String targetField, SemanticModel semanticModel) {
        if (parentNode.kind() != SyntaxKind.LOCAL_VAR_DECL) {
            return null;
        }

        VariableDeclarationNode varDeclNode = (VariableDeclarationNode) parentNode;

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
        Optional<ExpressionNode> optInitializer = varDeclNode.initializer();
        if (optInitializer.isEmpty()) {
            return new TargetNode(typeSymbol, variableSymbol.getName().get(), null);
        }
        ExpressionNode initializer = optInitializer.get();
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
        if (!splits[0].equals(varDeclNode.typedBindingPattern().bindingPattern().toSourceCode().trim())) {
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

    private Map<String, MappingFieldNode> convertMappingFieldsToMap(SeparatedNodeList<MappingFieldNode> mappingFields) {
        Map<String, MappingFieldNode> mappingFieldNodeMap = new HashMap<>();
        int size = mappingFields.size();
        for (int i = 0; i < size; i++) {
            SpecificFieldNode mappingFieldNode = (SpecificFieldNode) mappingFields.get(i);
            mappingFieldNodeMap.put(mappingFieldNode.fieldName().toSourceCode(), mappingFieldNode);
        }
        return mappingFieldNodeMap;
    }

    private void generateRecordVariableDataMapping(ExpressionNode expressionNode, List<Mapping> mappings,
                                                   String name, SemanticModel semanticModel) {
        SyntaxKind exprKind = expressionNode.kind();
        if (exprKind == SyntaxKind.MAPPING_CONSTRUCTOR) {
            genMapping((MappingConstructorExpressionNode) expressionNode, mappings, name, semanticModel);
        } else if (exprKind == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            genMapping((SimpleNameReferenceNode) expressionNode, mappings, name, semanticModel);
        }
    }

    private void generateArrayVariableDataMapping(ExpressionNode expressionNode, List<Mapping> mappings,
                                                  String name, SemanticModel semanticModel) {
        SyntaxKind exprKind = expressionNode.kind();
        if (exprKind == SyntaxKind.LIST_CONSTRUCTOR) {
            genMapping((ListConstructorExpressionNode) expressionNode, mappings, name, semanticModel);
        } else if (exprKind == SyntaxKind.QUERY_EXPRESSION) {
            genMapping((QueryExpressionNode) expressionNode, mappings, name, semanticModel);
        } else if (exprKind == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            genMapping((SimpleNameReferenceNode) expressionNode, mappings, name, semanticModel);
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
                    genMapping((ListConstructorExpressionNode) fieldExpr, mappings, name + "." + f.fieldName(),
                            semanticModel);
                } else {
                    List<String> inputs = new ArrayList<>();
                    genInputs(fieldExpr, inputs);
                    Mapping mapping = new Mapping(name + "." + f.fieldName().toSourceCode().trim(), inputs,
                            fieldExpr.toSourceCode(), getDiagnostics(fieldExpr.lineRange(), semanticModel), new ArrayList<>());
                    mappings.add(mapping);
                }
            }
        }
    }

    private void genMapping(SimpleNameReferenceNode varRef, List<Mapping> mappings, String name,
                            SemanticModel semanticModel) {
        List<String> inputs = new ArrayList<>();
        genInputs(varRef, inputs);
        Mapping mapping = new Mapping(name, inputs, varRef.toSourceCode(),
                getDiagnostics(varRef.lineRange(), semanticModel), new ArrayList<>());
        mappings.add(mapping);
    }

    private void genMapping(ListConstructorExpressionNode listCtrExpr, List<Mapping> mappings, String name,
                            SemanticModel semanticModel) {
        SeparatedNodeList<Node> expressions = listCtrExpr.expressions();
        int size = expressions.size();
        List<Mapping> mappingElements = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Node expr = expressions.get(i);
            if (expr.kind() == SyntaxKind.MAPPING_CONSTRUCTOR) {
                genMapping((MappingConstructorExpressionNode) expr, mappingElements, name + "." + i, semanticModel);
            }
            else if (expr.kind() == SyntaxKind.INDEXED_EXPRESSION) {
                genMapping((IndexedExpressionNode) expr, mappingElements, name + "." + i, semanticModel);
            }
        }
        List<String> inputs = new ArrayList<>();
        Mapping mapping = new Mapping(name, inputs, listCtrExpr.toSourceCode(), getDiagnostics(listCtrExpr.lineRange(), semanticModel), mappingElements);
        mappings.add(mapping);
    }

    private void genMapping(QueryExpressionNode queryExpr, List<Mapping> mappings, String name,
                            SemanticModel semanticModel) {
        // ((SelectClauseNode) expressionNode.resultClause()).expression()
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

    private void genMapping(IndexedExpressionNode indexedExprNode, List<Mapping> mappings, String name,
                            SemanticModel semanticModel) {
        String indexedExprSource = indexedExprNode.toSourceCode();
        List<String> inputs = new ArrayList<>();
        inputs.add(indexedExprSource.replace("[", ".").substring(0, indexedExprSource.length() - 1)); // TODO: Change this
        // TODO: check element here
        Mapping mapping = new Mapping(name, inputs, indexedExprSource, getDiagnostics(indexedExprNode.lineRange(), semanticModel), new ArrayList<>());
        mappings.add(mapping);
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
        }
    }

    private List<String> getDiagnostics(LineRange lineRange, SemanticModel semanticModel) {
        List<Diagnostic> diagnostics = semanticModel.diagnostics(lineRange);
        List<String> diagnosticMsgs = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics) {
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
                // TODO: Name of constant is set to type name, check that
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
            MappingPort memberPort = getMappingPort(id, id, arrayType.memberType);
            MappingArrayPort arrayPort = new MappingArrayPort(id, name, memberPort == null ? "record" :
                    memberPort.typeName + "[]", type.getTypeName());
            arrayPort.member = memberPort;
            return arrayPort;
        } else {
            return null;
        }
    }

    private static final java.lang.reflect.Type mt = new TypeToken<List<Mapping>>() {
    }.getType();

    public String getSource(JsonElement mp, JsonElement fNode, String targetField) {
        FlowNode flowNode = gson.fromJson(fNode, FlowNode.class);
        List<Mapping> fieldMapping = gson.fromJson(mp, mt);
        Map<String, Object> mappings = new LinkedHashMap<>();
        for (Mapping mapping : fieldMapping) {
            genSourceForMapping(mapping, mappings);
        }
        String mappingSource = genSource(mappings);
        if (flowNode.codedata().node() == NodeKind.VARIABLE) {
            Optional<Property> optProperty = flowNode.getProperty("expression");
            if (optProperty.isPresent()) {
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
            }
        }

        return mappingSource;
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

    // TODO: Check the logic
    private void genSourceForMapping(Mapping mapping, Map<String, Object> mappingSource) {
        String output = mapping.output;
        String[] splits = output.split("\\.");
        if (splits.length == 1) {
            return;
        }

        Map<String, Object> currentMapping = mappingSource;
        String key = splits[1];
        for (int i = 1; i < splits.length - 1; i++) {
            String split = splits[i];
            Object o = currentMapping.get(key);
            if (o == null) {
                if (splits[i + 1].matches("^-?\\d+$")) {
                    currentMapping.put(split, new ArrayList<>());
                } else {
                    Map<String, Object> newMapping = new HashMap<>();
                    currentMapping.put(split, newMapping);
                    currentMapping = newMapping;
                    key = split;
                }
            } else if (o instanceof Map<?, ?>) {
                currentMapping = (Map<String, Object>) o;
                key = split;
            } else if (o instanceof ArrayList<?>) {
                List list = (List) o;
                if (split.matches("^-?\\d+$")) {
                    int i1 = Integer.parseInt(split);
                    if (list.size() > i1 && list.get(i1) != null) {
                        currentMapping = (Map<String, Object>) list.get(i1);
                    } else {
                        Map<String, Object> newMapping = new HashMap<>();
                        list.add(i1, newMapping);
                        currentMapping = newMapping;
                    }
                }
            }
        }
        currentMapping.put(splits[splits.length - 1], mapping.expression);
    }

    public String getQuery(JsonElement fNode, String targetField, Path filePath, LinePosition position,
                           Project project) {
        FlowNode flowNode = gson.fromJson(fNode, FlowNode.class);
        SourceModification modification = applyNode(flowNode, project, filePath, position);

        TargetNode targetNode = getTargetNode(modification.stNode(), targetField, modification.semanticModel());
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
        Map<Path, List<TextEdit>> textEdits =
                NodeBuilder.getNodeFromKind(flowNode.codedata().node()).toSource(sourceBuilder);
        String source = textEdits.entrySet().stream().iterator().next().getValue().get(0).getNewText();
        TextDocument textDocument = document.textDocument();
        int startTextPosition = textDocument.textPositionFrom(position);
        io.ballerina.tools.text.TextEdit te = io.ballerina.tools.text.TextEdit.from(TextRange.from(startTextPosition,
                0), source);
        io.ballerina.tools.text.TextEdit[] tes = {te};
        TextDocument modifiedTextDoc = textDocument.apply(TextDocumentChange.from(tes));
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

    private record SourceModification(String source, Document document, SemanticModel semanticModel, Node stNode) {
    }

    private String getQuerySource(ExpressionNode inputExpr, RecordTypeSymbol recordTypeSymbol) {
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

    public JsonElement getVisualizableProperties(JsonElement node, LinePosition position) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);

        List<String> visualizableProperties = new ArrayList<>();
        if (flowNode.codedata().node() != NodeKind.VARIABLE) {
            return gson.toJsonTree(visualizableProperties);
        }

        Map<String, Property> properties = flowNode.properties();
        Property property = properties.get("type");
        Object value = property.value();
        if (!(value instanceof String typeName)) {
            return gson.toJsonTree(visualizableProperties);
        }
        if (typeName.matches(".*\\d*]")) {
            typeName = typeName.split("\\[")[0];
        }
        Map<String, Symbol> visibleVariables = visibleTypeSymbols(this.semanticModel.visibleSymbols(this.document,
                position));
        Symbol symbol = visibleVariables.get(typeName);
        if (symbol != null) {
            visualizableProperties.add("expression");
        }
        return gson.toJsonTree(visualizableProperties);
    }

    private Map<String, Symbol> visibleTypeSymbols(List<Symbol> symbols) {
        Map<String, Symbol> variableSymbols = new HashMap<>();
        for (Symbol symbol : symbols) {
            if (symbol.kind() == SymbolKind.TYPE_DEFINITION) {
                TypeSymbol rawType = CommonUtils.getRawType(((TypeDefinitionSymbol) symbol).typeDescriptor());
                TypeDescKind typeKind = rawType.typeKind();
                if (typeKind == TypeDescKind.RECORD || typeKind == TypeDescKind.ARRAY) {
                    variableSymbols.put(symbol.getName().get(), rawType);
                }
            }
        }
        return variableSymbols;
    }

    private record Model(List<MappingPort> inputs, MappingPort output, List<Mapping> mappings, String source) {

    }

    private record Mapping(String output, List<String> inputs, String expression, List<String> diagnostics, List<Mapping> elements) {

    }

    // TODO: Recheck the constructor generation
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
    }
}
