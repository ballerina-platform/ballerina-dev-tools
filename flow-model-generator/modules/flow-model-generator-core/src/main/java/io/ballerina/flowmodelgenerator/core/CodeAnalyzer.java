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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BreakStatementNode;
import io.ballerina.compiler.syntax.tree.ByteArrayLiteralNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClientResourceAccessActionNode;
import io.ballerina.compiler.syntax.tree.CommentNode;
import io.ballerina.compiler.syntax.tree.CommitActionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ComputedResourceAccessSegmentNode;
import io.ballerina.compiler.syntax.tree.ContinueStatementNode;
import io.ballerina.compiler.syntax.tree.DoStatementNode;
import io.ballerina.compiler.syntax.tree.ElseBlockNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FailStatementNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.ForkStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.LocalTypeDefinitionStatementNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MatchClauseNode;
import io.ballerina.compiler.syntax.tree.MatchGuardNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.OnFailClauseNode;
import io.ballerina.compiler.syntax.tree.PanicStatementNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.RetryStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.RollbackStatementNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StartActionNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TransactionStatementNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.flowmodelgenerator.core.db.DatabaseManager;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.db.model.Parameter;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.ModuleInfo;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.node.AssignBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.BinaryBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.FailBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.IfBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.JsonPayloadBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnectionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.PanicBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ReturnBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.RollbackBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.StartBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.VariableBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.XmlPayloadBuilder;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Analyzes the source code and generates the flow model.
 *
 * @since 2.0.0
 */
class CodeAnalyzer extends NodeVisitor {

    // Readonly fields
    private final Project project;
    private final SemanticModel semanticModel;
    private final Map<String, LineRange> dataMappings;
    private final TextDocument textDocument;
    private final ModuleInfo moduleInfo;
    private final DiagnosticHandler diagnosticHandler;
    private final boolean forceAssign;
    private final String connectionScope;

    // State fields
    private NodeBuilder nodeBuilder;
    private final List<FlowNode> flowNodeList;
    private final Stack<NodeBuilder> flowNodeBuilderStack;
    private TypedBindingPatternNode typedBindingPatternNode;

    public CodeAnalyzer(Project project, SemanticModel semanticModel, String connectionScope,
                        Map<String, LineRange> dataMappings, TextDocument textDocument, ModuleInfo moduleInfo,
                        boolean forceAssign) {
        this.project = project;
        this.semanticModel = semanticModel;
        this.dataMappings = dataMappings;
        this.connectionScope = connectionScope;
        this.textDocument = textDocument;
        this.moduleInfo = moduleInfo;
        this.forceAssign = forceAssign;
        this.flowNodeList = new ArrayList<>();
        this.flowNodeBuilderStack = new Stack<>();
        this.diagnosticHandler = new DiagnosticHandler(semanticModel);
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(functionDefinitionNode);
        if (symbol.isEmpty()) {
            return;
        }

        startNode(NodeKind.EVENT_START, functionDefinitionNode).codedata()
                .lineRange(functionDefinitionNode.functionBody().lineRange())
                .sourceCode(functionDefinitionNode.toSourceCode().strip());
        endNode();
        super.visit(functionDefinitionNode);
    }

    @Override
    public void visit(ObjectFieldNode objectFieldNode) {
        objectFieldNode.expression().ifPresent(expressionNode -> expressionNode.accept(this));
        nodeBuilder.properties()
                .type(objectFieldNode.typeName(), true)
                .data(objectFieldNode.fieldName(), false, new HashSet<>());
        endNode(objectFieldNode);
    }

    @Override
    public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
        for (Node statementOrComment : functionBodyBlockNode.statementsWithComments()) {
            statementOrComment.accept(this);
        }
    }

    @Override
    public void visit(CommentNode commentNode) {
        Node node = commentNode.getCommentAttachedNode();
        LinePosition startPos = textDocument.linePositionFrom(node.textRangeWithMinutiae().startOffset());
        int offset = 0;
        if (!(node instanceof Token)) {
            offset = node.textRangeWithMinutiae().startOffset();
        }
        LinePosition endPos =
                textDocument.linePositionFrom(commentNode.getLastMinutiae().textRange().endOffset() + offset);
        LineRange commentRange = LineRange.from(node.lineRange().fileName(), startPos, endPos);
        CommentMetadata commentMetadata = new CommentMetadata(String.join(System.lineSeparator(),
                commentNode.getCommentLines()), commentRange);
        genCommentNode(commentMetadata);
    }

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        Optional<ExpressionNode> optExpr = returnStatementNode.expression();
        if (optExpr.isEmpty()) {
            startNode(NodeKind.STOP, returnStatementNode);
        } else {
            ExpressionNode expr = optExpr.get();
            expr.accept(this);
            if (isNodeUnidentified()) {
                startNode(NodeKind.RETURN, returnStatementNode)
                        .metadata().description(String.format(ReturnBuilder.DESCRIPTION, expr)).stepOut()
                        .properties().expression(expr, ReturnBuilder.RETURN_EXPRESSION_DOC);
            }
        }
        nodeBuilder.returning();
        endNode(returnStatementNode);
    }

    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        String methodName = remoteMethodCallActionNode.methodName().name().text();
        ExpressionNode expressionNode = remoteMethodCallActionNode.expression();
        SeparatedNodeList<FunctionArgumentNode> argumentNodes = remoteMethodCallActionNode.arguments();

        Optional<Symbol> symbol = semanticModel.symbol(remoteMethodCallActionNode);
        if (symbol.isEmpty() || (symbol.get().kind() != SymbolKind.METHOD)) {
            handleExpressionNode(remoteMethodCallActionNode);
            return;
        }

        MethodSymbol methodSymbol = (MethodSymbol) symbol.get();
        Optional<Documentation> documentation = methodSymbol.documentation();
        String description = documentation.flatMap(Documentation::description).orElse("");

        startNode(NodeKind.REMOTE_ACTION_CALL, expressionNode.parent())
                .symbolInfo(methodSymbol)
                .metadata()
                    .label(methodName)
                    .description(description)
                    .stepOut()
                .codedata()
                    .object("Client")
                    .symbol(methodName)
                    .stepOut()
                .properties().callExpression(expressionNode, Property.CONNECTION_KEY);

        DatabaseManager dbManager = DatabaseManager.getInstance();
        ModuleID id = symbol.get().getModule().get().id();
        Optional<FunctionResult> functionResult = dbManager.getAction(id.orgName(), id.moduleName(),
                symbol.get().getName().get(), null, DatabaseManager.FunctionKind.REMOTE);

        final Map<String, Node> namedArgValueMap = new HashMap<>();
        final Queue<Node> positionalArgs = new LinkedList<>();
        calculateFunctionArgs(namedArgValueMap, positionalArgs, argumentNodes);

        if (functionResult.isPresent()) { // function details are indexed
            analyzeAndHandleExprArgs(argumentNodes, dbManager, functionResult.get(),
                    methodSymbol, positionalArgs, namedArgValueMap);
        } else {
            handleFunctionCallActionCallsParams(argumentNodes, methodSymbol);
        }
        handleCheckFlag(remoteMethodCallActionNode, SyntaxKind.CHECK_ACTION, methodSymbol.typeDescriptor());

        nodeBuilder.codedata().nodeInfo(remoteMethodCallActionNode);
    }

    @Override
    public void visit(ClientResourceAccessActionNode clientResourceAccessActionNode) {
        String methodName = clientResourceAccessActionNode.methodName()
                .map(simpleNameReference -> simpleNameReference.name().text()).orElse("");
        ExpressionNode expressionNode = clientResourceAccessActionNode.expression();
        SeparatedNodeList<FunctionArgumentNode> argumentNodes =
                clientResourceAccessActionNode.arguments().map(ParenthesizedArgList::arguments).orElse(null);
        if (argumentNodes == null) { // cl->/path/to/'resource;
            methodName = "get";
        }

        Optional<Symbol> symbol = semanticModel.symbol(clientResourceAccessActionNode);
        if (symbol.isEmpty() || (symbol.get().kind() != SymbolKind.METHOD &&
                symbol.get().kind() != SymbolKind.RESOURCE_METHOD)) {
            handleExpressionNode(clientResourceAccessActionNode);
            return;
        }

        MethodSymbol methodSymbol = (MethodSymbol) symbol.get();
        Optional<Documentation> documentation = methodSymbol.documentation();
        String description = documentation.flatMap(Documentation::description).orElse("");
        SeparatedNodeList<Node> nodes = clientResourceAccessActionNode.resourceAccessPath();

        ParamUtils.ResourcePathTemplate resourcePathTemplate = ParamUtils.buildResourcePathTemplate(semanticModel,
                methodSymbol, semanticModel.types().ERROR);

        startNode(NodeKind.RESOURCE_ACTION_CALL, expressionNode.parent())
                .symbolInfo(methodSymbol)
                .metadata()
                    .label(methodName)
                    .description(description)
                    .stepOut()
                .codedata()
                .object("Client")
                .symbol(methodName)
                .resourcePath(resourcePathTemplate.resourcePathTemplate())
                .stepOut()
                .properties()
                .callExpression(expressionNode, Property.CONNECTION_KEY)
                .data(this.typedBindingPatternNode, false, new HashSet<>());

        if (CommonUtils.isHttpModule(methodSymbol)) {
            String resourcePath = nodes.stream().map(Node::toSourceCode).collect(Collectors.joining("/"));
            String fullPath = "/" + resourcePath;
            nodeBuilder.properties().resourcePath(fullPath, true);
        } else {
            nodeBuilder.properties().resourcePath(resourcePathTemplate.resourcePathTemplate(), false);

            int idx = 0;
            for (int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                if (nodes.size() <= idx) {
                    break;
                }
                if (node instanceof ComputedResourceAccessSegmentNode computedResourceAccessSegmentNode) {
                    ExpressionNode expr = computedResourceAccessSegmentNode.expression();
                    ParameterResult paramResult = resourcePathTemplate.pathParams().get(idx);
                    String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                    nodeBuilder.properties()
                            .custom()
                                .metadata()
                                .label(unescapedParamName)
                                .description(paramResult.description())
                                .stepOut()
                            .codedata()
                                .kind(paramResult.kind().name())
                                .originalName(paramResult.name())
                                .stepOut()
                            .value(expr.toSourceCode())
                            .typeConstraint(paramResult.type())
                            .type(Property.ValueType.EXPRESSION)
                            .editable()
                            .defaultable(paramResult.optional() == 1)
                            .stepOut()
                            .addProperty(unescapedParamName);
                    idx++;
                }
            }
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();
        ModuleID id = symbol.get().getModule().get().id();
        Optional<FunctionResult> functionResult = dbManager.getAction(id.orgName(), id.moduleName(),
                symbol.get().getName().get(), resourcePathTemplate.resourcePathTemplate(),
                DatabaseManager.FunctionKind.RESOURCE);

        final Map<String, Node> namedArgValueMap = new HashMap<>();
        final Queue<Node> positionalArgs = new LinkedList<>();
        calculateFunctionArgs(namedArgValueMap, positionalArgs, argumentNodes);

        if (functionResult.isPresent()) { // function details are indexed
            analyzeAndHandleExprArgs(argumentNodes, dbManager, functionResult.get(),
                    methodSymbol, positionalArgs, namedArgValueMap);
        } else {
            handleFunctionCallActionCallsParams(argumentNodes, methodSymbol);
        }
        handleCheckFlag(clientResourceAccessActionNode, SyntaxKind.CHECK_ACTION, methodSymbol.typeDescriptor());
        nodeBuilder.codedata().nodeInfo(clientResourceAccessActionNode);
    }

    private void analyzeAndHandleExprArgs(SeparatedNodeList<FunctionArgumentNode> argumentNodes,
                                          DatabaseManager dbManager,
                                          FunctionResult functionResult,
                                          FunctionSymbol methodSymbol,
                                          Queue<Node> positionalArgs,
                                          Map<String, Node> namedArgValueMap) {
        LinkedHashMap<String, ParameterResult> funcParamMap = dbManager
                .getFunctionParametersAsMap(functionResult.functionId());

        FunctionTypeSymbol functionTypeSymbol = methodSymbol.typeDescriptor();
        buildPropsFromFuncCallArgs(argumentNodes, functionTypeSymbol, funcParamMap, positionalArgs, namedArgValueMap);
    }

    private void addRemainingParamsToPropertyMap(LinkedHashMap<String, ParameterResult> funcParamMap,
                                                 boolean hasOnlyRestParams) {
        for (Map.Entry<String, ParameterResult> entry : funcParamMap.entrySet()) {
            ParameterResult paramResult = entry.getValue();
            if (paramResult.kind().equals(Parameter.Kind.PARAM_FOR_TYPE_INFER)
                    || paramResult.kind().equals(Parameter.Kind.INCLUDED_RECORD)) {
                continue;
            }

            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder = nodeBuilder.properties().custom();
            customPropBuilder
                    .metadata()
                        .label(unescapedParamName)
                        .description(paramResult.description())
                        .stepOut()
                    .codedata()
                        .kind(paramResult.kind().name())
                        .originalName(paramResult.name())
                        .importStatements(paramResult.importStatements())
                        .stepOut()
                    .placeholder(paramResult.defaultValue())
                    .typeConstraint(paramResult.type())
                    .editable()
                    .defaultable(paramResult.optional() == 1);

            if (paramResult.kind() == Parameter.Kind.INCLUDED_RECORD_REST) {
                if (hasOnlyRestParams) {
                    customPropBuilder.defaultable(false);
                }
                unescapedParamName = "additionalValues";
                customPropBuilder.type(Property.ValueType.MAPPING_EXPRESSION_SET);
            } else if (paramResult.kind() == Parameter.Kind.REST_PARAMETER) {
                if (hasOnlyRestParams) {
                    customPropBuilder.defaultable(false);
                }
                customPropBuilder.type(Property.ValueType.EXPRESSION_SET);
            } else if (paramResult.kind() == Parameter.Kind.REQUIRED) {
                customPropBuilder.type(Property.ValueType.EXPRESSION_SET).value(paramResult.defaultValue());
            } else {
                customPropBuilder.type(Property.ValueType.EXPRESSION);
            }
            customPropBuilder
                    .stepOut()
                    .addProperty(unescapedParamName);
        }
    }

    private void calculateFunctionArgs(Map<String, Node> namedArgValueMap,
                                       Queue<Node> positionalArgs,
                                       SeparatedNodeList<FunctionArgumentNode> argumentNodes) {
        if (argumentNodes != null) {
            for (FunctionArgumentNode argument : argumentNodes) {
                switch (argument.kind()) {
                    case NAMED_ARG -> {
                        NamedArgumentNode namedArgument = (NamedArgumentNode) argument;
                        namedArgValueMap.put(namedArgument.argumentName().name().text(),
                                namedArgument.expression());
                    }
                    case POSITIONAL_ARG -> positionalArgs.add(((PositionalArgumentNode) argument).expression());
                    default -> {
                        // Ignore the default case
                    }
                }
            }
        }
    }

    private void handleFunctionCallActionCallsParams(SeparatedNodeList<FunctionArgumentNode> argumentNodes,
                                                     FunctionSymbol functionSymbol) {
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
        final Map<String, Node> namedArgValueMap = new HashMap<>();
        final Queue<Node> positionalArgs = new LinkedList<>();
        calculateFunctionArgs(namedArgValueMap, positionalArgs, argumentNodes);
        LinkedHashMap<String, ParameterResult> funcParamMap = ParamUtils.buildFunctionParamResultMap(
                functionSymbol, semanticModel);
        buildPropsFromFuncCallArgs(argumentNodes, functionTypeSymbol, funcParamMap, positionalArgs, namedArgValueMap);
    }

    private void buildPropsFromFuncCallArgs(SeparatedNodeList<FunctionArgumentNode> argumentNodes,
                                            FunctionTypeSymbol functionTypeSymbol,
                                            LinkedHashMap<String, ParameterResult> funcParamMap,
                                            Queue<Node> positionalArgs, Map<String, Node> namedArgValueMap) {
        if (argumentNodes == null) { // cl->/path/to/'resource;
            List<ParameterResult> functionParameters = funcParamMap.values().stream().toList();
            boolean hasOnlyRestParams = functionParameters.size() == 1;
            for (ParameterResult paramResult : functionParameters) {
                Parameter.Kind paramKind = paramResult.kind();

                if (paramKind.equals(Parameter.Kind.PATH_PARAM) || paramKind.equals(Parameter.Kind.PATH_REST_PARAM)
                        || paramKind.equals(Parameter.Kind.PARAM_FOR_TYPE_INFER)
                        || paramKind.equals(Parameter.Kind.INCLUDED_RECORD)) {
                    continue;
                }

                String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder = nodeBuilder.properties().custom();
                customPropBuilder
                        .metadata()
                            .label(unescapedParamName)
                            .description(paramResult.description())
                            .stepOut()
                        .codedata()
                            .kind(paramKind.name())
                            .originalName(paramResult.name())
                            .importStatements(paramResult.importStatements())
                            .stepOut()
                        .placeholder(paramResult.defaultValue())
                        .typeConstraint(paramResult.type())
                        .editable()
                        .defaultable(paramResult.optional() == 1);

                if (paramKind == Parameter.Kind.INCLUDED_RECORD_REST) {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    customPropBuilder.type(Property.ValueType.MAPPING_EXPRESSION_SET);
                } else if (paramKind == Parameter.Kind.REST_PARAMETER) {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    customPropBuilder.type(Property.ValueType.EXPRESSION_SET);
                } else if (paramKind == Parameter.Kind.REQUIRED) {
                    customPropBuilder.type(Property.ValueType.EXPRESSION).value(paramResult.defaultValue());
                } else {
                    customPropBuilder.type(Property.ValueType.EXPRESSION);
                }
                customPropBuilder
                        .stepOut()
                        .addProperty(unescapedParamName);
            }
            return;
        }

        boolean hasOnlyRestParams = funcParamMap.size() == 1;
        if (functionTypeSymbol.restParam().isPresent()) {
            ParameterSymbol restParamSymbol = functionTypeSymbol.restParam().get();
            Optional<List<ParameterSymbol>> paramsOptional = functionTypeSymbol.params();

            if (paramsOptional.isPresent()) {
                List<ParameterSymbol> paramsList = paramsOptional.get();
                int paramCount = paramsList.size(); // param count without rest params
                int argCount = positionalArgs.size();

                List<String> restArgs = new ArrayList<>();
                for (int i = 0; i < paramsList.size(); i++) {
                    ParameterSymbol parameterSymbol = paramsList.get(i);
                    String escapedParamName = parameterSymbol.getName().get();
                    ParameterResult paramResult = funcParamMap.get(escapedParamName);
                    if (paramResult == null) {
                        escapedParamName = CommonUtil.escapeReservedKeyword(parameterSymbol.getName().get());
                    }
                    paramResult = funcParamMap.get(escapedParamName);
                    Node paramValue = i < argCount ? positionalArgs.poll()
                            : namedArgValueMap.get(paramResult.name());

                    funcParamMap.remove(parameterSymbol.getName().get());
                    Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                            nodeBuilder.properties().custom();
                    String value = paramValue != null ? paramValue.toSourceCode() : null;
                    String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                    customPropBuilder
                            .metadata()
                                .label(unescapedParamName)
                                .description(paramResult.description())
                                .stepOut()
                            .type(getPropertyTypeFromParamKind(paramResult.kind()))
                            .typeConstraint(paramResult.type())
                            .value(value)
                            .placeholder(paramResult.defaultValue())
                            .editable()
                            .defaultable(paramResult.optional() == 1)
                            .codedata()
                                .kind(paramResult.kind().name())
                                .originalName(paramResult.name())
                                .importStatements(paramResult.importStatements())
                                .stepOut()
                            .stepOut()
                            .addProperty(unescapedParamName);
                }

                for (int i = paramCount; i < argCount; i++) {
                    restArgs.add(Objects.requireNonNull(positionalArgs.poll()).toSourceCode());
                }
                Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                        nodeBuilder.properties().custom();
                String escapedParamName = CommonUtil.escapeReservedKeyword(restParamSymbol.getName().get());
                ParameterResult restParamResult = funcParamMap.get(escapedParamName);
                funcParamMap.remove(restParamSymbol.getName().get());
                String unescapedParamName = ParamUtils.removeLeadingSingleQuote(restParamResult.name());
                customPropBuilder
                        .metadata()
                            .label(unescapedParamName)
                            .description(restParamResult.description())
                            .stepOut()
                        .type(getPropertyTypeFromParamKind(restParamResult.kind()))
                        .typeConstraint(restParamResult.type())
                        .value(restArgs)
                        .placeholder(restParamResult.defaultValue())
                        .editable()
                        .defaultable(!hasOnlyRestParams)
                        .codedata()
                            .kind(restParamResult.kind().name())
                            .originalName(restParamResult.name())
                            .importStatements(restParamResult.importStatements())
                            .stepOut()
                        .stepOut()
                        .addProperty(unescapedParamName);
            }
            // iterate over functionParamMap
            addRemainingParamsToPropertyMap(funcParamMap, hasOnlyRestParams);
            return;
        }
        Optional<List<ParameterSymbol>> paramsOptional = functionTypeSymbol.params();
        if (paramsOptional.isPresent()) {
            List<ParameterSymbol> paramsList = paramsOptional.get();
            int argCount = positionalArgs.size();

            final List<LinkedHashMap<String, String>> includedRecordRestArgs = new ArrayList<>();
            for (int i = 0; i < paramsList.size(); i++) {
                ParameterSymbol parameterSymbol = paramsList.get(i);
                String escapedParamName = parameterSymbol.getName().get();
                ParameterResult paramResult = funcParamMap.get(escapedParamName);
                if (paramResult == null) {
                    escapedParamName = CommonUtil.escapeReservedKeyword(parameterSymbol.getName().get());
                }
                paramResult = funcParamMap.get(escapedParamName);
                Node paramValue;
                if (i < argCount) {
                    paramValue = positionalArgs.poll();
                } else {
                    paramValue = namedArgValueMap.get(paramResult.name());
                    namedArgValueMap.remove(paramResult.name());
                }
                if (paramResult.kind() == Parameter.Kind.INCLUDED_RECORD) {
                    if (argumentNodes.size() > i && argumentNodes.get(i).kind() == SyntaxKind.NAMED_ARG) {
                        FunctionArgumentNode argNode = argumentNodes.get(i);
                        funcParamMap.remove(escapedParamName);
                        NamedArgumentNode namedArgumentNode = (NamedArgumentNode) argNode;
                        String argName = namedArgumentNode.argumentName().name().text();
                        if (argName.equals(paramResult.name())) {  // foo("a", b = {})
                            paramResult = funcParamMap.get(escapedParamName);

                            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                                    nodeBuilder.properties().custom();
                            String value = paramValue != null ? paramValue.toSourceCode() : null;
                            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                            customPropBuilder
                                    .metadata()
                                        .label(unescapedParamName)
                                        .description(paramResult.description())
                                        .stepOut()
                                    .type(getPropertyTypeFromParamKind(paramResult.kind()))
                                    .typeConstraint(paramResult.type())
                                    .value(value)
                                    .placeholder(paramResult.defaultValue())
                                    .editable()
                                    .defaultable(paramResult.optional() == 1)
                                    .codedata()
                                        .kind(paramResult.kind().name())
                                        .originalName(paramResult.name())
                                        .importStatements(paramResult.importStatements())
                                        .stepOut()
                                    .stepOut()
                                    .addProperty(unescapedParamName, paramValue);
                        } else {
                            if (funcParamMap.containsKey(argName)) { // included record attribute
                                paramResult = funcParamMap.get(argName);
                                funcParamMap.remove(argName);
                                Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                                        nodeBuilder.properties().custom();
                                if (paramValue == null) {
                                    paramValue = namedArgValueMap.get(argName);
                                    namedArgValueMap.remove(argName);
                                }
                                String value = paramValue != null ? paramValue.toSourceCode() : null;
                                String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                                customPropBuilder
                                        .metadata()
                                            .label(unescapedParamName)
                                            .description(paramResult.description())
                                            .stepOut()
                                        .type(getPropertyTypeFromParamKind(paramResult.kind()))
                                        .typeConstraint(paramResult.type())
                                        .value(value)
                                        .placeholder(paramResult.defaultValue())
                                        .editable()
                                        .defaultable(paramResult.optional() == 1)
                                        .codedata()
                                            .kind(paramResult.kind().name())
                                            .originalName(paramResult.name())
                                            .importStatements(paramResult.importStatements())
                                            .stepOut()
                                        .stepOut()
                                        .addProperty(unescapedParamName, paramValue);

                            }
                        }

                    } else { // positional arg
                        if (paramValue != null) {
                            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                                    nodeBuilder.properties().custom();

                            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                            funcParamMap.remove(escapedParamName);
                            String value = paramValue.toSourceCode();
                            customPropBuilder
                                    .metadata()
                                        .label(unescapedParamName)
                                        .description(paramResult.description())
                                        .stepOut()
                                    .type(getPropertyTypeFromParamKind(paramResult.kind()))
                                    .typeConstraint(paramResult.type())
                                    .value(value)
                                    .placeholder(paramResult.defaultValue())
                                    .editable()
                                    .defaultable(paramResult.optional() == 1)
                                    .codedata()
                                        .kind(paramResult.kind().name())
                                        .originalName(paramResult.name())
                                        .importStatements(paramResult.importStatements())
                                        .stepOut()
                                    .stepOut()
                                    .addProperty(unescapedParamName, paramValue);
                            return;
                        }
                    }
                }

                if (paramValue == null && paramResult.kind() == Parameter.Kind.INCLUDED_RECORD) {
                    funcParamMap.remove(escapedParamName);
                    continue;
                }
                Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                        nodeBuilder.properties().custom();
                funcParamMap.remove(escapedParamName);
                String value = paramValue != null ? paramValue.toSourceCode() : null;
                String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
                customPropBuilder
                        .metadata()
                            .label(unescapedParamName)
                            .description(paramResult.description())
                            .stepOut()
                        .type(getPropertyTypeFromParamKind(paramResult.kind()))
                        .typeConstraint(paramResult.type())
                        .value(value)
                        .placeholder(paramResult.defaultValue())
                        .editable()
                        .defaultable(paramResult.optional() == 1)
                        .codedata()
                            .kind(paramResult.kind().name())
                            .originalName(paramResult.name())
                            .importStatements(paramResult.importStatements())
                            .stepOut()
                        .stepOut()
                        .addProperty(unescapedParamName, paramValue);
            }
            for (Map.Entry<String, Node> entry : namedArgValueMap.entrySet()) {
                LinkedHashMap<String, String> map = new LinkedHashMap<>();
                map.put(entry.getKey(), entry.getValue().toSourceCode());
                includedRecordRestArgs.add(map);
            }
            ParameterResult includedRecordRest = funcParamMap.get("Additional Values");
            if (includedRecordRest != null) {
                funcParamMap.remove("Additional Values");
                Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder =
                        nodeBuilder.properties().custom();
                String unescapedParamName = ParamUtils.removeLeadingSingleQuote(includedRecordRest.name());
                customPropBuilder
                        .metadata()
                            .label(unescapedParamName)
                            .description(includedRecordRest.description())
                            .stepOut()
                        .type(getPropertyTypeFromParamKind(includedRecordRest.kind()))
                        .typeConstraint(includedRecordRest.type())
                        .value(includedRecordRestArgs)
                        .placeholder(includedRecordRest.defaultValue())
                        .editable()
                        .defaultable(includedRecordRest.optional() == 1)
                        .codedata()
                            .kind(includedRecordRest.kind().name())
                            .originalName(includedRecordRest.name())
                            .importStatements(includedRecordRest.importStatements())
                            .stepOut()
                        .stepOut()
                        .addProperty("additionalValues");
            }
            addRemainingParamsToPropertyMap(funcParamMap, hasOnlyRestParams);
        }
    }

    private void handleCheckFlag(NonTerminalNode node, SyntaxKind check, FunctionTypeSymbol functionTypeSymbol) {
        if (node.parent().kind() == check) {
            nodeBuilder.properties().checkError(true);
        } else {
            functionTypeSymbol.returnTypeDescriptor()
                    .ifPresent(typeSymbol -> {
                        if (CommonUtils.subTypeOf(typeSymbol, semanticModel.types().ERROR)
                                && CommonUtils.withinDoClause(node)) {
                            nodeBuilder.properties().checkError(false);
                        }
                    });
        }
    }

    private Property.ValueType getPropertyTypeFromParamKind(Parameter.Kind kind) {
        if (kind == Parameter.Kind.REST_PARAMETER) {
            return Property.ValueType.EXPRESSION_SET;
        } else if (kind == Parameter.Kind.INCLUDED_RECORD_REST) {
            return Property.ValueType.MAPPING_EXPRESSION_SET;
        }
        return Property.ValueType.EXPRESSION;
    }

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
        startNode(NodeKind.IF, ifElseStatementNode);
        addConditionalBranch(ifElseStatementNode.condition(), ifElseStatementNode.ifBody(), IfBuilder.IF_THEN_LABEL);
        ifElseStatementNode.elseBody().ifPresent(this::analyzeElseBody);
        endNode(ifElseStatementNode);
    }

    private void addConditionalBranch(ExpressionNode condition, BlockStatementNode body, String label) {
        Branch.Builder branchBuilder = startBranch(label, NodeKind.CONDITIONAL, Branch.BranchKind.BLOCK,
                Branch.Repeatable.ONE_OR_MORE).properties().condition(condition).stepOut();
        analyzeBlock(body, branchBuilder);
        endBranch(branchBuilder, body);
    }

    private void analyzeElseBody(Node elseBody) {
        switch (elseBody.kind()) {
            case ELSE_BLOCK -> analyzeElseBody(((ElseBlockNode) elseBody).elseBody());
            case BLOCK_STATEMENT -> {
                Branch.Builder branchBuilder =
                        startBranch(IfBuilder.IF_ELSE_LABEL, NodeKind.ELSE, Branch.BranchKind.BLOCK,
                                Branch.Repeatable.ZERO_OR_ONE);
                analyzeBlock((BlockStatementNode) elseBody, branchBuilder);
                endBranch(branchBuilder, elseBody);
            }
            case IF_ELSE_STATEMENT -> {
                IfElseStatementNode ifElseNode = (IfElseStatementNode) elseBody;
                addConditionalBranch(ifElseNode.condition(), ifElseNode.ifBody(),
                        ifElseNode.condition().toSourceCode().strip());
                ifElseNode.elseBody().ifPresent(this::analyzeElseBody);
            }
            default -> throw new IllegalStateException("Unexpected else body kind: " + elseBody.kind());
        }
    }

    @Override
    public void visit(ImplicitNewExpressionNode implicitNewExpressionNode) {
        SeparatedNodeList<FunctionArgumentNode> argumentNodes =
                implicitNewExpressionNode.parenthesizedArgList()
                        .map(ParenthesizedArgList::arguments)
                        .orElse(null);
        checkForNewConnection(implicitNewExpressionNode, argumentNodes);
        super.visit(implicitNewExpressionNode);
    }

    @Override
    public void visit(ExplicitNewExpressionNode explicitNewExpressionNode) {
        SeparatedNodeList<FunctionArgumentNode> argumentNodes =
                explicitNewExpressionNode.parenthesizedArgList().arguments();
        checkForNewConnection(explicitNewExpressionNode, argumentNodes);
        super.visit(explicitNewExpressionNode);
    }

    private void checkForNewConnection(NewExpressionNode newExpressionNode,
                                       SeparatedNodeList<FunctionArgumentNode> argumentNodes) {
        Optional<TypeSymbol> typeSymbol =
                CommonUtils.getTypeSymbol(semanticModel, newExpressionNode).flatMap(symbol -> {
                    if (symbol.typeKind() == TypeDescKind.UNION) {
                        return ((UnionTypeSymbol) symbol).memberTypeDescriptors().stream()
                                .filter(tSymbol -> !tSymbol.subtypeOf(semanticModel.types().ERROR))
                                .findFirst();
                    }
                    return Optional.of(symbol);
                });
        if (typeSymbol.isEmpty()) {
            return;
        }

        String moduleName = CommonUtils.getModuleName(typeSymbol.get());

        if (typeSymbol.get().typeKind() != TypeDescKind.TYPE_REFERENCE) {
            return;
        }
        Symbol defintionSymbol = ((TypeReferenceTypeSymbol) typeSymbol.get()).definition();
        if (defintionSymbol.kind() != SymbolKind.CLASS) {
            return;
        }
        ClassSymbol classSymbol = (ClassSymbol) defintionSymbol;
        String description = classSymbol.documentation().flatMap(Documentation::description).orElse("");

        Optional<MethodSymbol> initMethodSymbol = classSymbol.initMethod();
        if (initMethodSymbol.isEmpty()) {
            return;
        }
        Map<String, String> documentationMap =
                initMethodSymbol.get().documentation().map(Documentation::parameterMap).orElse(Map.of());

        startNode(NodeKind.NEW_CONNECTION, newExpressionNode)
                .symbolInfo(initMethodSymbol.get())
                .metadata()
                    .label(moduleName)
                    .description(description)
                    .stepOut()
                .codedata()
                .object(NewConnectionBuilder.CLIENT_SYMBOL)
                .symbol(NewConnectionBuilder.INIT_SYMBOL)
                .stepOut()
                .properties()
                .scope(connectionScope)
                .checkError(true, NewConnectionBuilder.CHECK_ERROR_DOC, false);
        try {
            MethodSymbol methodSymbol =
                    ((ClassSymbol) ((TypeReferenceTypeSymbol) typeSymbol.get()).definition()).initMethod()
                            .orElseThrow();

            DatabaseManager dbManager = DatabaseManager.getInstance();
            ModuleID id = methodSymbol.getModule().get().id();
            Optional<FunctionResult> functionResult = dbManager.getAction(id.orgName(), id.moduleName(),
                    methodSymbol.getName().get(), null, DatabaseManager.FunctionKind.CONNECTOR);

            final Map<String, Node> namedArgValueMap = new HashMap<>();
            final Queue<Node> positionalArgs = new LinkedList<>();
            calculateFunctionArgs(namedArgValueMap, positionalArgs, argumentNodes);

            if (functionResult.isPresent()) { // function details are indexed
                analyzeAndHandleExprArgs(argumentNodes, dbManager, functionResult.get(),
                        methodSymbol, positionalArgs, namedArgValueMap);
                return;
            }
            methodSymbol.typeDescriptor().params().ifPresent(params -> nodeBuilder.properties().functionArguments(
                    argumentNodes, params, documentationMap, methodSymbol.external()));
        } catch (RuntimeException ignored) {

        }
    }

    @Override
    public void visit(TemplateExpressionNode templateExpressionNode) {
        if (forceAssign) {
            return;
        }
        if (templateExpressionNode.kind() == SyntaxKind.XML_TEMPLATE_EXPRESSION) {
            startNode(NodeKind.XML_PAYLOAD, templateExpressionNode)
                    .metadata()
                    .description(XmlPayloadBuilder.DESCRIPTION)
                    .stepOut()
                    .properties().expression(templateExpressionNode);
        }
    }

    @Override
    public void visit(ByteArrayLiteralNode byteArrayLiteralNode) {
        if (forceAssign) {
            return;
        }
        startNode(NodeKind.BINARY_DATA, byteArrayLiteralNode)
                .metadata()
                .stepOut()
                .properties().expression(byteArrayLiteralNode);
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        handleVariableNode(variableDeclarationNode);
    }

    private void handleVariableNode(NonTerminalNode variableDeclarationNode) {
        Optional<ExpressionNode> initializer;
        Optional<Token> finalKeyword;
        switch (variableDeclarationNode.kind()) {
            case LOCAL_VAR_DECL -> {
                VariableDeclarationNode localVariableDeclarationNode =
                        (VariableDeclarationNode) variableDeclarationNode;
                initializer = localVariableDeclarationNode.initializer();
                this.typedBindingPatternNode = localVariableDeclarationNode.typedBindingPattern();
                finalKeyword = localVariableDeclarationNode.finalKeyword();
            }
            case MODULE_VAR_DECL -> {
                ModuleVariableDeclarationNode moduleVariableDeclarationNode =
                        (ModuleVariableDeclarationNode) variableDeclarationNode;
                initializer = moduleVariableDeclarationNode.initializer();
                this.typedBindingPatternNode = moduleVariableDeclarationNode.typedBindingPattern();
                finalKeyword = Optional.empty();
            }
            default -> throw new IllegalStateException("Unexpected variable declaration kind: " +
                    variableDeclarationNode.kind());
        }
        boolean implicit = false;
        if (initializer.isEmpty()) {
            implicit = true;
            startNode(NodeKind.VARIABLE, variableDeclarationNode)
                    .metadata()
                    .description(AssignBuilder.DESCRIPTION)
                    .stepOut()
                    .properties().expression((ExpressionNode) null, VariableBuilder.EXPRESSION_DOC, true);
        } else {
            ExpressionNode initializerNode = initializer.get();
            initializerNode.accept(this);

            // Generate the default expression node if a node is not built
            if (isNodeUnidentified()) {
                implicit = true;
                startNode(NodeKind.VARIABLE, variableDeclarationNode)
                        .metadata()
                        .description(AssignBuilder.DESCRIPTION)
                        .stepOut()
                        .properties().expression(initializerNode, VariableBuilder.EXPRESSION_DOC, true);
            }
        }

        // TODO: Find a better way on how we can achieve this
        if (nodeBuilder instanceof DataMapperBuilder) {
            nodeBuilder.properties().data(this.typedBindingPatternNode, new HashSet<>());
        } else if (nodeBuilder instanceof XmlPayloadBuilder) {
            nodeBuilder.properties().payload(this.typedBindingPatternNode, "xml");
        } else if (nodeBuilder instanceof JsonPayloadBuilder) {
            nodeBuilder.properties().payload(this.typedBindingPatternNode, "json");
        } else if (nodeBuilder instanceof BinaryBuilder) {
            nodeBuilder.properties().payload(this.typedBindingPatternNode, "byte[]");
        } else if (nodeBuilder instanceof NewConnectionBuilder) {
            nodeBuilder.properties()
                    .dataVariable(this.typedBindingPatternNode, NewConnectionBuilder.CONNECTION_NAME_LABEL,
                            NewConnectionBuilder.CONNECTION_TYPE_LABEL, false, new HashSet<>());
        } else {
            nodeBuilder.properties().dataVariable(this.typedBindingPatternNode, implicit, new HashSet<>());
        }
        finalKeyword.ifPresent(token -> nodeBuilder.flag(FlowNode.NODE_FLAG_FINAL));
        endNode(variableDeclarationNode);
        this.typedBindingPatternNode = null;
    }

    @Override
    public void visit(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        handleVariableNode(moduleVariableDeclarationNode);
    }

    @Override
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        ExpressionNode expression = assignmentStatementNode.expression();
        expression.accept(this);

        if (isNodeUnidentified()) {
            startNode(NodeKind.ASSIGN, assignmentStatementNode)
                    .metadata()
                    .description(AssignBuilder.DESCRIPTION)
                    .stepOut()
                    .properties()
                    .expression(expression, AssignBuilder.EXPRESSION_DOC, false)
                    .data(assignmentStatementNode.varRef(), true, new HashSet<>());
        }

        if (nodeBuilder instanceof XmlPayloadBuilder || nodeBuilder instanceof JsonPayloadBuilder
                || nodeBuilder instanceof BinaryBuilder) {
            nodeBuilder.properties().data(assignmentStatementNode.varRef(), false, new HashSet<>());
        }
        endNode(assignmentStatementNode);
    }

    @Override
    public void visit(CompoundAssignmentStatementNode compoundAssignmentStatementNode) {
        handleDefaultStatementNode(compoundAssignmentStatementNode, () -> super.visit(compoundAssignmentStatementNode));
    }

    @Override
    public void visit(BlockStatementNode blockStatementNode) {
        handleDefaultNodeWithBlock(blockStatementNode);
    }

    @Override
    public void visit(BreakStatementNode breakStatementNode) {
        startNode(NodeKind.BREAK, breakStatementNode);
        endNode(breakStatementNode);
    }

    @Override
    public void visit(FailStatementNode failStatementNode) {
        startNode(NodeKind.FAIL, failStatementNode)
                .properties().expression(failStatementNode.expression(), FailBuilder.FAIL_EXPRESSION_DOC);
        endNode(failStatementNode);
    }

    @Override
    public void visit(ExpressionStatementNode expressionStatementNode) {
        super.visit(expressionStatementNode);
        if (isNodeUnidentified()) {
            handleExpressionNode(expressionStatementNode);
        }
        endNode(expressionStatementNode);
    }

    @Override
    public void visit(ContinueStatementNode continueStatementNode) {
        startNode(NodeKind.CONTINUE, continueStatementNode);
        endNode(continueStatementNode);
    }

    @Override
    public void visit(MethodCallExpressionNode methodCallExpressionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(methodCallExpressionNode);
        if (symbol.isEmpty() || !(symbol.get() instanceof FunctionSymbol functionSymbol)) {
            handleExpressionNode(methodCallExpressionNode);
            return;
        }

        ExpressionNode expressionNode = methodCallExpressionNode.expression();
        NameReferenceNode nameReferenceNode = methodCallExpressionNode.methodName();

        Optional<Documentation> documentation = functionSymbol.documentation();
        String description = documentation.flatMap(Documentation::description).orElse("");

        String functionName = getIdentifierName(nameReferenceNode);

        startNode(NodeKind.METHOD_CALL, methodCallExpressionNode.parent());
        if (CommonUtils.isDefaultPackage(functionSymbol, moduleInfo)) {
            functionSymbol.getLocation()
                    .flatMap(location -> CommonUtil.findNode(functionSymbol,
                            CommonUtils.getDocument(project, location).syntaxTree()))
                    .ifPresent(node -> nodeBuilder.properties().view(node.lineRange()));
        }
        nodeBuilder
                .symbolInfo(functionSymbol)
                    .metadata()
                    .label(functionName)
                    .description(description)
                    .stepOut()
                .codedata()
                    .symbol(functionName)
                    .stepOut()
                .properties()
                .callExpression(expressionNode, Property.CONNECTION_KEY);

        DatabaseManager dbManager = DatabaseManager.getInstance();
        ModuleID id = functionSymbol.getModule().get().id();
        Optional<FunctionResult> functionResult = dbManager.getAction(id.orgName(), id.moduleName(),
                functionSymbol.getName().get(), null, DatabaseManager.FunctionKind.FUNCTION);

        final Map<String, Node> namedArgValueMap = new HashMap<>();
        final Queue<Node> positionalArgs = new LinkedList<>();

        if (!CommonUtils.isValueLangLibFunction(functionSymbol)) {
            SeparatedNodeList<FunctionArgumentNode> arguments = methodCallExpressionNode.arguments();
            calculateFunctionArgs(namedArgValueMap, positionalArgs, arguments);

            if (functionResult.isPresent()) { // function details are indexed
                analyzeAndHandleExprArgs(arguments, dbManager, functionResult.get(),
                        functionSymbol, positionalArgs, namedArgValueMap);
            } else {
                handleFunctionCallActionCallsParams(arguments, functionSymbol);
            }
        }
        handleCheckFlag(methodCallExpressionNode, SyntaxKind.CHECK_EXPRESSION, functionSymbol.typeDescriptor());

        nodeBuilder
                .symbolInfo(functionSymbol)
                .metadata()
                .label(functionName)
                .description(description)
                .stepOut()
                .codedata().symbol(functionName);
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(functionCallExpressionNode);
        if (symbol.isEmpty() || symbol.get().kind() != SymbolKind.FUNCTION) {
            handleExpressionNode(functionCallExpressionNode);
            return;
        }

        FunctionSymbol functionSymbol = (FunctionSymbol) symbol.get();
        NameReferenceNode nameReferenceNode = functionCallExpressionNode.functionName();

        Optional<Documentation> documentation = functionSymbol.documentation();
        String description = documentation.flatMap(Documentation::description).orElse("");

        String functionName = getIdentifierName(nameReferenceNode);

        if (dataMappings.containsKey(functionName)) {
            startNode(NodeKind.DATA_MAPPER_CALL, functionCallExpressionNode.parent());
        } else {
            startNode(NodeKind.FUNCTION_CALL, functionCallExpressionNode.parent());
        }

        if (CommonUtils.isDefaultPackage(functionSymbol, moduleInfo)) {
            functionSymbol.getLocation()
                    .flatMap(location -> CommonUtil.findNode(functionSymbol,
                            CommonUtils.getDocument(project, location).syntaxTree()))
                    .ifPresent(node -> nodeBuilder.properties().view(node.lineRange()));
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();
        ModuleID id = functionSymbol.getModule().get().id();
        Optional<FunctionResult> functionResult = dbManager.getAction(id.orgName(), id.moduleName(),
                functionSymbol.getName().get(), null, DatabaseManager.FunctionKind.FUNCTION);

        final Map<String, Node> namedArgValueMap = new HashMap<>();
        final Queue<Node> positionalArgs = new LinkedList<>();
        SeparatedNodeList<FunctionArgumentNode> arguments = functionCallExpressionNode.arguments();
        calculateFunctionArgs(namedArgValueMap, positionalArgs, arguments);

        if (functionResult.isPresent()) { // function details are indexed
            analyzeAndHandleExprArgs(arguments, dbManager, functionResult.get(), functionSymbol, positionalArgs,
                    namedArgValueMap);
        } else {
            handleFunctionCallActionCallsParams(arguments, functionSymbol);
        }
        handleCheckFlag(functionCallExpressionNode, SyntaxKind.CHECK_EXPRESSION, functionSymbol.typeDescriptor());

        nodeBuilder
                .symbolInfo(functionSymbol)
                .metadata()
                .label(functionName)
                .description(description)
                .stepOut()
                .codedata().symbol(functionName);
    }

    private static String getIdentifierName(NameReferenceNode nameReferenceNode) {
        return switch (nameReferenceNode.kind()) {
            case QUALIFIED_NAME_REFERENCE -> ((QualifiedNameReferenceNode) nameReferenceNode).identifier().text();
            case SIMPLE_NAME_REFERENCE -> ((SimpleNameReferenceNode) nameReferenceNode).name().text();
            default -> "";
        };
    }

    @Override
    public void visit(WhileStatementNode whileStatementNode) {
        startNode(NodeKind.WHILE, whileStatementNode)
                .properties().condition(whileStatementNode.condition());

        BlockStatementNode whileBody = whileStatementNode.whileBody();
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, NodeKind.CONDITIONAL, Branch.BranchKind.BLOCK,
                        Branch.Repeatable.ONE);
        analyzeBlock(whileBody, branchBuilder);
        endBranch(branchBuilder, whileBody);
        whileStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(whileStatementNode);
    }

    private void processOnFailClause(OnFailClauseNode onFailClauseNode) {
        Branch.Builder branchBuilder =
                startBranch(Branch.ON_FAILURE_LABEL, NodeKind.ON_FAILURE, Branch.BranchKind.BLOCK,
                        Branch.Repeatable.ZERO_OR_ONE);
        if (onFailClauseNode.typedBindingPattern().isPresent()) {
            branchBuilder.properties().ignore(false).onErrorVariable(onFailClauseNode.typedBindingPattern().get());
        }
        BlockStatementNode onFailClauseBlock = onFailClauseNode.blockStatement();
        analyzeBlock(onFailClauseBlock, branchBuilder);
        endBranch(branchBuilder, onFailClauseBlock);
    }

    @Override
    public void visit(PanicStatementNode panicStatementNode) {
        startNode(NodeKind.PANIC, panicStatementNode)
                .properties().expression(panicStatementNode.expression(), PanicBuilder.PANIC_EXPRESSION_DOC);
        endNode(panicStatementNode);
    }

    @Override
    public void visit(LocalTypeDefinitionStatementNode localTypeDefinitionStatementNode) {
        handleDefaultStatementNode(localTypeDefinitionStatementNode,
                () -> super.visit(localTypeDefinitionStatementNode));
    }

    @Override
    public void visit(StartActionNode startActionNode) {
        startNode(NodeKind.START, startActionNode)
                .properties().expression(startActionNode.expression(), StartBuilder.START_EXPRESSION_DOC);
        endNode(startActionNode);
    }

    @Override
    public void visit(LockStatementNode lockStatementNode) {
        startNode(NodeKind.LOCK, lockStatementNode);
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        BlockStatementNode lockBody = lockStatementNode.blockStatement();
        analyzeBlock(lockBody, branchBuilder);
        endBranch(branchBuilder, lockBody);
        lockStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(lockStatementNode);
    }

    @Override
    public void visit(ForkStatementNode forkStatementNode) {
        handleDefaultStatementNode(forkStatementNode, () -> super.visit(forkStatementNode));
    }

    @Override
    public void visit(TransactionStatementNode transactionStatementNode) {
        startNode(NodeKind.TRANSACTION, transactionStatementNode);
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        BlockStatementNode blockStatementNode = transactionStatementNode.blockStatement();
        analyzeBlock(blockStatementNode, branchBuilder);
        endBranch(branchBuilder, blockStatementNode);
        transactionStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(transactionStatementNode);
    }

    @Override
    public void visit(ForEachStatementNode forEachStatementNode) {
        startNode(NodeKind.FOREACH, forEachStatementNode)
                .properties()
                .dataVariable(forEachStatementNode.typedBindingPattern(), new HashSet<>())
                .collection(forEachStatementNode.actionOrExpressionNode());
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        BlockStatementNode blockStatementNode = forEachStatementNode.blockStatement();
        analyzeBlock(blockStatementNode, branchBuilder);
        endBranch(branchBuilder, blockStatementNode);
        forEachStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(forEachStatementNode);
    }

    @Override
    public void visit(RollbackStatementNode rollbackStatementNode) {
        startNode(NodeKind.ROLLBACK, rollbackStatementNode);
        Optional<ExpressionNode> optExpr = rollbackStatementNode.expression();
        if (optExpr.isPresent()) {
            ExpressionNode expr = optExpr.get();
            expr.accept(this);
            nodeBuilder.properties().expression(expr, RollbackBuilder.ROLLBACK_EXPRESSION_DOC);
        }
        endNode(rollbackStatementNode);
    }

    @Override
    public void visit(RetryStatementNode retryStatementNode) {
        int retryCount = retryStatementNode.arguments().isEmpty() ? 3 :
                Integer.parseInt(retryStatementNode.arguments()
                        .map(arg -> arg.arguments().get(0)).get().toString());

        StatementNode statementNode = retryStatementNode.retryBody();
        if (statementNode.kind() == SyntaxKind.BLOCK_STATEMENT) {
            startNode(NodeKind.RETRY, retryStatementNode)
                    .properties().retryCount(retryCount);

            Branch.Builder branchBuilder =
                    startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
            analyzeBlock((BlockStatementNode) statementNode, branchBuilder);
            endBranch(branchBuilder, statementNode);
            retryStatementNode.onFailClause().ifPresent(this::processOnFailClause);
            endNode(retryStatementNode);
        } else { // retry transaction node
            TransactionStatementNode transactionStatementNode = (TransactionStatementNode) statementNode;
            BlockStatementNode blockStatementNode = transactionStatementNode.blockStatement();
            startNode(NodeKind.TRANSACTION, retryStatementNode)
                    .properties().retryCount(retryCount);
            Branch.Builder branchBuilder =
                    startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
            analyzeBlock(blockStatementNode, branchBuilder);
            endBranch(branchBuilder, blockStatementNode);
            transactionStatementNode.onFailClause().ifPresent(this::processOnFailClause);
            endNode(retryStatementNode);
        }
    }

    @Override
    public void visit(CommitActionNode commitActionNode) {
        startNode(NodeKind.COMMIT, commitActionNode);
        endNode();
    }

    @Override
    public void visit(MatchStatementNode matchStatementNode) {
        startNode(NodeKind.MATCH, matchStatementNode)
                .properties().condition(matchStatementNode.condition());

        NodeList<MatchClauseNode> matchClauseNodes = matchStatementNode.matchClauses();
        for (MatchClauseNode matchClauseNode : matchClauseNodes) {
            Optional<MatchGuardNode> matchGuardNode = matchClauseNode.matchGuard();
            String label = matchClauseNode.matchPatterns().stream()
                    .map(node -> node.toSourceCode().strip())
                    .collect(Collectors.joining("|"));
            if (matchGuardNode.isPresent()) {
                label += " " + matchGuardNode.get().toSourceCode().strip();
            }

            Branch.Builder branchBuilder = startBranch(label, NodeKind.CONDITIONAL, Branch.BranchKind.BLOCK,
                    Branch.Repeatable.ONE_OR_MORE)
                    .properties().patterns(matchClauseNode.matchPatterns()).stepOut();

            matchGuardNode.ifPresent(guard -> branchBuilder.properties()
                    .expression(guard.expression(), Property.GUARD_KEY, Property.GUARD_DOC));
            analyzeBlock(matchClauseNode.blockStatement(), branchBuilder);
            endBranch(branchBuilder, matchClauseNode.blockStatement());
        }

        matchStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(matchStatementNode);
    }

    @Override
    public void visit(DoStatementNode doStatementNode) {
        Optional<OnFailClauseNode> optOnFailClauseNode = doStatementNode.onFailClause();
        BlockStatementNode blockStatementNode = doStatementNode.blockStatement();
        if (optOnFailClauseNode.isEmpty()) {
            handleDefaultNodeWithBlock(blockStatementNode);
            return;
        }

        startNode(NodeKind.ERROR_HANDLER, doStatementNode);
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        analyzeBlock(blockStatementNode, branchBuilder);
        endBranch(branchBuilder, blockStatementNode);
        processOnFailClause(optOnFailClauseNode.get());
        endNode(doStatementNode);
    }

    @Override
    public void visit(CheckExpressionNode checkExpressionNode) {
        checkExpressionNode.expression().accept(this);
        if (isNodeUnidentified()) {
            return;
        }

        String checkText = checkExpressionNode.checkKeyword().text();
        switch (checkText) {
            case Constants.CHECK -> nodeBuilder.flag(FlowNode.NODE_FLAG_CHECKED);
            case Constants.CHECKPANIC -> nodeBuilder.flag(FlowNode.NODE_FLAG_CHECKPANIC);
            default -> throw new IllegalStateException("Unexpected value: " + checkText);

        }
        nodeBuilder.codedata().nodeInfo(checkExpressionNode);
    }

    @Override
    public void visit(MappingConstructorExpressionNode mappingCtrExprNode) {
        handleConstructorExpressionNode(mappingCtrExprNode);
    }

    @Override
    public void visit(ListConstructorExpressionNode listCtrExprNode) {
        handleConstructorExpressionNode(listCtrExprNode);
    }

    private void handleConstructorExpressionNode(ExpressionNode constructorExprNode) {
        NonTerminalNode parent = constructorExprNode.parent();
        SyntaxKind kind = parent.kind();

        if (kind != SyntaxKind.ASSIGNMENT_STATEMENT && kind != SyntaxKind.MODULE_VAR_DECL &&
                kind != SyntaxKind.LOCAL_VAR_DECL) {
            return;
        }

        Optional<Symbol> parentSymbol = semanticModel.symbol(parent);
        if (parentSymbol.isPresent() && CommonUtils.getRawType(
                ((VariableSymbol) parentSymbol.get()).typeDescriptor()).typeKind() == TypeDescKind.JSON &&
                !forceAssign) {
            startNode(NodeKind.JSON_PAYLOAD, constructorExprNode)
                    .metadata()
                    .description(JsonPayloadBuilder.DESCRIPTION)
                    .stepOut()
                    .properties().expression(constructorExprNode);
        }
    }

    // Utility methods

    /**
     * It's the responsibility of the parent node to add the children nodes when building the diagram. Hence, the method
     * only adds the node to the diagram if there is no active parent node which is building its branches.
     */
    private void endNode(Node node) {
        nodeBuilder.codedata().nodeInfo(node);
        endNode();
    }

    private void endNode() {
        if (this.flowNodeBuilderStack.isEmpty()) {
            this.flowNodeList.add(buildNode());
        }
    }

    private NodeBuilder startNode(NodeKind kind) {
        this.nodeBuilder = NodeBuilder.getNodeFromKind(kind)
                .semanticModel(semanticModel)
                .defaultModuleName(moduleInfo);
        return this.nodeBuilder;
    }

    private NodeBuilder startNode(NodeKind kind, Node node) {
        this.nodeBuilder = NodeBuilder.getNodeFromKind(kind)
                .semanticModel(semanticModel)
                .diagnosticHandler(diagnosticHandler)
                .defaultModuleName(moduleInfo);
        diagnosticHandler.handle(nodeBuilder,
                node instanceof ExpressionNode ? node.parent().lineRange() : node.lineRange(), false);
        return this.nodeBuilder;
    }

    /**
     * Builds the flow node and resets the node builder.
     *
     * @return the built flow node
     */
    private FlowNode buildNode() {
        FlowNode node = nodeBuilder.build();
        this.nodeBuilder = null;
        return node;
    }

    /**
     * Starts a new branch and sets the node builder to the starting node of the branch.
     */
    private Branch.Builder startBranch(String label, NodeKind node, Branch.BranchKind kind,
                                       Branch.Repeatable repeatable) {
        this.flowNodeBuilderStack.push(nodeBuilder);
        this.nodeBuilder = null;
        return new Branch.Builder()
                .semanticModel(semanticModel)
                .defaultModuleName(moduleInfo)
                .diagnosticHandler(diagnosticHandler)
                .codedata().node(node).stepOut()
                .label(label)
                .kind(kind)
                .repeatable(repeatable);
    }

    /**
     * Ends the current branch and sets the node builder to the parent node.
     */
    private void endBranch(Branch.Builder branchBuilder, Node node) {
        branchBuilder.codedata().nodeInfo(node);
        nodeBuilder = this.flowNodeBuilderStack.pop();
        nodeBuilder.branch(branchBuilder.build());
    }

    private boolean isNodeUnidentified() {
        return this.nodeBuilder == null;
    }

    /**
     * The default procedure to handle the statement nodes. These nodes should be handled explicitly.
     *
     * @param statementNode the statement node
     * @param runnable      The runnable to be called to analyze the child nodes.
     */
    private void handleDefaultStatementNode(NonTerminalNode statementNode,
                                            Runnable runnable) {
        handleExpressionNode(statementNode);
        runnable.run();
        endNode(statementNode);
    }

    private void handleExpressionNode(NonTerminalNode statementNode) {
        startNode(NodeKind.EXPRESSION, statementNode)
                .properties().statement(statementNode);
    }

    /**
     * The default procedure to handle the node with a block statement.
     *
     * @param bodyNode the block statement node
     */
    private void handleDefaultNodeWithBlock(BlockStatementNode bodyNode) {
        handleExpressionNode(bodyNode);
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, NodeKind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        analyzeBlock(bodyNode, branchBuilder);
        endBranch(branchBuilder, bodyNode);
        endNode(bodyNode);
    }

    private void analyzeBlock(BlockStatementNode blockStatement, Branch.Builder thenBranchBuilder) {
        for (Node statementOrComment : blockStatement.statementsWithComments()) {
            statementOrComment.accept(this);
            thenBranchBuilder.node(buildNode());
        }
    }

    private void genCommentNode(CommentMetadata comment) {
        startNode(NodeKind.COMMENT)
                .metadata().description(comment.comment()).stepOut()
                .properties().comment(comment.comment());
        nodeBuilder.codedata()
                .lineRange(comment.position)
                .sourceCode(comment.comment());
        endNode();
    }

    public List<FlowNode> getFlowNodes() {
        return flowNodeList;
    }

    private record CommentMetadata(String comment, LineRange position) {

    }
}
