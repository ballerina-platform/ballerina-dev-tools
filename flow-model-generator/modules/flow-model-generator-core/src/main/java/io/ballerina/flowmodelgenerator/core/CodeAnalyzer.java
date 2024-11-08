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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ActionNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BreakStatementNode;
import io.ballerina.compiler.syntax.tree.ByteArrayLiteralNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClientResourceAccessActionNode;
import io.ballerina.compiler.syntax.tree.CommentNode;
import io.ballerina.compiler.syntax.tree.CommitActionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
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
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.OnFailClauseNode;
import io.ballerina.compiler.syntax.tree.PanicStatementNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
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
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.node.Assign;
import io.ballerina.flowmodelgenerator.core.model.node.BinaryData;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapper;
import io.ballerina.flowmodelgenerator.core.model.node.Fail;
import io.ballerina.flowmodelgenerator.core.model.node.If;
import io.ballerina.flowmodelgenerator.core.model.node.JsonPayload;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnection;
import io.ballerina.flowmodelgenerator.core.model.node.Panic;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.Rollback;
import io.ballerina.flowmodelgenerator.core.model.node.Start;
import io.ballerina.flowmodelgenerator.core.model.node.XmlPayload;
import io.ballerina.projects.ModuleDescriptor;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Analyzes the source code and generates the flow model.
 *
 * @since 1.4.0
 */
class CodeAnalyzer extends NodeVisitor {

    //TODO: Wrap the class variables inside another class
    private final Project project;
    private final List<FlowNode> flowNodeList;
    private NodeBuilder nodeBuilder;
    private final SemanticModel semanticModel;
    private final Stack<NodeBuilder> flowNodeBuilderStack;
    private final Map<String, LineRange> dataMappings;
    private TypedBindingPatternNode typedBindingPatternNode;
    private final String connectionScope;
    private final TextDocument textDocument;
    private final ModuleDescriptor moduleDescriptor;
    private final boolean forceAssign;
    private final DiagnosticHandler diagnosticHandler;

    public CodeAnalyzer(Project project, SemanticModel semanticModel, String connectionScope,
                        Map<String, LineRange> dataMappings, TextDocument textDocument,
                        ModuleDescriptor moduleDescriptor,
                        boolean forceAssign) {
        this.project = project;
        this.flowNodeList = new ArrayList<>();
        this.semanticModel = semanticModel;
        this.flowNodeBuilderStack = new Stack<>();
        this.dataMappings = dataMappings;
        this.connectionScope = connectionScope;
        this.textDocument = textDocument;
        this.moduleDescriptor = moduleDescriptor;
        this.forceAssign = forceAssign;
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
                        .metadata()
                            .description(String.format(Return.DESCRIPTION, expr))
                            .stepOut()
                        .properties()
                            .expression(expr, Return.RETURN_EXPRESSION_DOC);
            }
        }
        nodeBuilder.returning();
        endNode(returnStatementNode);
    }

    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        String methodName = remoteMethodCallActionNode.methodName().name().text();
        ExpressionNode expression = remoteMethodCallActionNode.expression();
        SeparatedNodeList<FunctionArgumentNode> argumentNodes = remoteMethodCallActionNode.arguments();
        handleActionNode(remoteMethodCallActionNode, methodName, expression, argumentNodes);
        nodeBuilder.codedata().nodeInfo(remoteMethodCallActionNode);
    }

    @Override
    public void visit(ClientResourceAccessActionNode clientResourceAccessActionNode) {
        String methodName = clientResourceAccessActionNode.methodName()
                .map(simpleNameReference -> simpleNameReference.name().text()).orElse("");
        ExpressionNode expression = clientResourceAccessActionNode.expression();
        SeparatedNodeList<FunctionArgumentNode> functionArgumentNodes =
                clientResourceAccessActionNode.arguments().map(ParenthesizedArgList::arguments).orElse(null);
        handleResourceActionNode(clientResourceAccessActionNode, methodName, expression, functionArgumentNodes);
        nodeBuilder.codedata().nodeInfo(clientResourceAccessActionNode);
    }

    private void handleActionNode(ActionNode actionNode, String methodName, ExpressionNode expressionNode,
                                  SeparatedNodeList<FunctionArgumentNode> argumentNodes) {
        Optional<Symbol> symbol = semanticModel.symbol(actionNode);
        if (symbol.isEmpty() || (symbol.get().kind() != SymbolKind.METHOD &&
                symbol.get().kind() != SymbolKind.RESOURCE_METHOD)) {
            handleExpressionNode(actionNode);
            return;
        }

        MethodSymbol methodSymbol = (MethodSymbol) symbol.get();
        Optional<Documentation> documentation = methodSymbol.documentation();
        String description = documentation.flatMap(Documentation::description).orElse("");
        Map<String, String> documentationMap = documentation.map(Documentation::parameterMap).orElse(Map.of());

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
                .properties()
                    .callExpression(expressionNode, Property.CONNECTION_KEY);
        handleFunctionParams(argumentNodes, methodSymbol, documentationMap);
        handleCheckFlag(actionNode, SyntaxKind.CHECK_ACTION, methodSymbol.typeDescriptor());
    }

    private void handleFunctionParams(SeparatedNodeList<FunctionArgumentNode> argumentNodes,
                                      FunctionSymbol functionSymbol,
                                      Map<String, String> documentationMap) {
        Optional<List<ParameterSymbol>> funcParams = functionSymbol.typeDescriptor().params();
        if (funcParams.isPresent()) {
            List<ParameterSymbol> params = funcParams.get().stream()
                    .filter(p -> p.paramKind() != ParameterKind.INCLUDED_RECORD)
                    .toList();
            nodeBuilder.properties().functionArguments(
                    argumentNodes, params, documentationMap, functionSymbol.external());
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

    private void handleResourceActionNode(ClientResourceAccessActionNode actionNode, String methodName,
                                          ExpressionNode expressionNode,
                                          SeparatedNodeList<FunctionArgumentNode> argumentNodes) {
        Optional<Symbol> symbol = semanticModel.symbol(actionNode);
        if (symbol.isEmpty() || (symbol.get().kind() != SymbolKind.METHOD &&
                symbol.get().kind() != SymbolKind.RESOURCE_METHOD)) {
            handleExpressionNode(actionNode);
            return;
        }

        MethodSymbol methodSymbol = (MethodSymbol) symbol.get();
        Optional<Documentation> documentation = methodSymbol.documentation();
        String description = documentation.flatMap(Documentation::description).orElse("");
        Map<String, String> documentationMap = documentation.map(Documentation::parameterMap).orElse(Map.of());

        SeparatedNodeList<Node> nodes = actionNode.resourceAccessPath();
        String resourcePath = nodes.stream().map(Node::toSourceCode).collect(Collectors.joining("/"));
        String fullPath = "/" + resourcePath;

        String resourcePathTemplate = CommonUtils.buildResourcePathTemplate(methodSymbol);

        startNode(NodeKind.RESOURCE_ACTION_CALL, expressionNode.parent())
                .symbolInfo(methodSymbol)
                .metadata()
                    .label(methodName)
                    .description(description)
                    .stepOut()
                .codedata()
                    .object("Client")
                    .symbol(methodName)
                    .resourcePath(resourcePathTemplate)
                    .stepOut()
                .properties()
                    .callExpression(expressionNode, Property.CONNECTION_KEY)
                    .resourcePath(fullPath)
                    .data(this.typedBindingPatternNode, false, new HashSet<>());

        handleFunctionParams(argumentNodes, methodSymbol, documentationMap);
        handleCheckFlag(actionNode, SyntaxKind.CHECK_ACTION, methodSymbol.typeDescriptor());

    }

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
        startNode(NodeKind.IF, ifElseStatementNode);
        addConditionalBranch(ifElseStatementNode.condition(), ifElseStatementNode.ifBody(), If.IF_THEN_LABEL);
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
                        startBranch(If.IF_ELSE_LABEL, NodeKind.ELSE, Branch.BranchKind.BLOCK,
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
                    .object(NewConnection.CLIENT_SYMBOL)
                    .symbol(NewConnection.INIT_SYMBOL)
                    .stepOut()
                .properties()
                    .scope(connectionScope)
                    .checkError(true, NewConnection.CHECK_ERROR_DOC, false);
        try {
            MethodSymbol methodSymbol =
                    ((ClassSymbol) ((TypeReferenceTypeSymbol) typeSymbol.get()).definition()).initMethod()
                            .orElseThrow();
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
                    .description(XmlPayload.DESCRIPTION)
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
                        .description(Assign.DESCRIPTION)
                        .stepOut()
                    .properties().expression(null, true);
        } else {
            ExpressionNode initializerNode = initializer.get();
            initializerNode.accept(this);

            // Generate the default expression node if a node is not built
            if (isNodeUnidentified()) {
                implicit = true;
                startNode(NodeKind.VARIABLE, variableDeclarationNode)
                        .metadata()
                            .description(Assign.DESCRIPTION)
                            .stepOut()
                        .properties().expression(initializerNode, true);
            }
        }

        // TODO: Find a better way on how we can achieve this
        if (nodeBuilder instanceof DataMapper) {
            nodeBuilder.properties().data(this.typedBindingPatternNode, new HashSet<>());
        } else if (nodeBuilder instanceof XmlPayload) {
            nodeBuilder.properties().payload(this.typedBindingPatternNode, "xml");
        } else if (nodeBuilder instanceof JsonPayload) {
            nodeBuilder.properties().payload(this.typedBindingPatternNode, "json");
        } else if (nodeBuilder instanceof BinaryData) {
            nodeBuilder.properties().payload(this.typedBindingPatternNode, "byte[]");
        } else if (nodeBuilder instanceof NewConnection) {
            nodeBuilder.properties().dataVariable(this.typedBindingPatternNode, NewConnection.CONNECTION_NAME_LABEL,
                    NewConnection.CONNECTION_TYPE_LABEL, new HashSet<>());
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
                        .description(Assign.DESCRIPTION)
                        .stepOut()
                    .properties()
                        .expression(expression)
                        .data(assignmentStatementNode.varRef(), true, new HashSet<>());
        }

        if (nodeBuilder instanceof XmlPayload || nodeBuilder instanceof JsonPayload
                || nodeBuilder instanceof BinaryData) {
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
                .properties().expression(failStatementNode.expression(), Fail.FAIL_EXPRESSION_DOC);
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
        Map<String, String> documentationMap = documentation.map(Documentation::parameterMap).orElse(Map.of());
        SeparatedNodeList<FunctionArgumentNode> arguments = functionCallExpressionNode.arguments();

        String functionName = switch (nameReferenceNode.kind()) {
            case QUALIFIED_NAME_REFERENCE -> ((QualifiedNameReferenceNode) nameReferenceNode).identifier().text();
            case SIMPLE_NAME_REFERENCE -> ((SimpleNameReferenceNode) nameReferenceNode).name().text();
            default -> "";
        };

        if (dataMappings.containsKey(functionName)) {
            startNode(NodeKind.DATA_MAPPER, functionCallExpressionNode.parent()).properties()
                    .functionName(functionName)
                    .output(this.typedBindingPatternNode);
            Optional<List<ParameterSymbol>> funcParams = functionSymbol.typeDescriptor().params();
            if (funcParams.isPresent()) {
                List<ParameterSymbol> params = funcParams.get().stream()
                        .filter(p -> p.paramKind() != ParameterKind.INCLUDED_RECORD)
                        .toList();
                nodeBuilder.properties().inputs(arguments, params);
            }
            nodeBuilder.properties().view(dataMappings.get(functionName));
        } else {
            startNode(NodeKind.FUNCTION_CALL, functionCallExpressionNode.parent());
            handleFunctionParams(arguments, functionSymbol, documentationMap);
            if (CommonUtils.isDefaultPackage(functionSymbol, moduleDescriptor)) {
                functionSymbol.getLocation()
                        .flatMap(location -> CommonUtil.findNode(functionSymbol,
                                CommonUtils.getDocument(project, location).syntaxTree()))
                        .ifPresent(node -> nodeBuilder.properties().view(node.lineRange()));
            }
        }

        nodeBuilder
                .symbolInfo(functionSymbol)
                .metadata()
                    .label(functionName)
                    .description(description)
                    .stepOut()
                .codedata().symbol(functionName);
        handleCheckFlag(functionCallExpressionNode, SyntaxKind.CHECK_EXPRESSION, functionSymbol.typeDescriptor());
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
                .properties().expression(panicStatementNode.expression(), Panic.PANIC_EXPRESSION_DOC);
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
                .properties().expression(startActionNode.expression(), Start.START_EXPRESSION_DOC);
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
            nodeBuilder.properties().expression(expr, Rollback.ROLLBACK_EXPRESSION_DOC);
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
                    .description(JsonPayload.DESCRIPTION)
                    .stepOut()
                    .properties().expression(constructorExprNode);
            return;
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
                .defaultModuleName(moduleDescriptor);
        return this.nodeBuilder;
    }

    private NodeBuilder startNode(NodeKind kind, Node node) {
        this.nodeBuilder = NodeBuilder.getNodeFromKind(kind)
                .semanticModel(semanticModel)
                .diagnosticHandler(diagnosticHandler)
                .defaultModuleName(moduleDescriptor);
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
                .defaultModuleName(moduleDescriptor)
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
