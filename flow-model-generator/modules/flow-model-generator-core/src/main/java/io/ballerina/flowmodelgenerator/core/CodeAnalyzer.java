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
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ActionNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BreakStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClientResourceAccessActionNode;
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
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.MinutiaeList;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
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
import io.ballerina.compiler.syntax.tree.TransactionStatementNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.flowmodelgenerator.core.central.Central;
import io.ballerina.flowmodelgenerator.core.central.CentralProxy;
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.node.Comment;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapper;
import io.ballerina.flowmodelgenerator.core.model.node.Fail;
import io.ballerina.flowmodelgenerator.core.model.node.If;
import io.ballerina.flowmodelgenerator.core.model.node.NewData;
import io.ballerina.flowmodelgenerator.core.model.node.Panic;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.Start;
import io.ballerina.flowmodelgenerator.core.model.node.UpdateData;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

/**
 * Analyzes the source code and generates the flow model.
 *
 * @since 1.4.0
 */
class CodeAnalyzer extends NodeVisitor {

    //TODO: Wrap the class variables inside another class
    private final List<FlowNode> flowNodeList;
    private NodeBuilder nodeBuilder;
    private final SemanticModel semanticModel;
    private final Stack<NodeBuilder> flowNodeBuilderStack;
    private final Central central;
    private final List<String> dataMappings;
    private TypedBindingPatternNode typedBindingPatternNode;
    private final String connectionScope;
    private final TextDocument textDocument;

    public CodeAnalyzer(SemanticModel semanticModel, String connectionScope, List<String> dataMappings,
                        TextDocument textDocument) {
        this.flowNodeList = new ArrayList<>();
        this.semanticModel = semanticModel;
        this.flowNodeBuilderStack = new Stack<>();
        this.central = CentralProxy.getInstance();
        this.dataMappings = dataMappings;
        this.connectionScope = connectionScope;
        this.textDocument = textDocument;
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(functionDefinitionNode);
        if (symbol.isEmpty()) {
            return;
        }

        switch (symbol.get().kind()) {
            case RESOURCE_METHOD -> {
                startNode(FlowNode.Kind.EVENT_HTTP_API)
                        .flag(FlowNode.NODE_FLAG_RESOURCE)
                        .properties()
                            .resourceSymbol((ResourceMethodSymbol) symbol.get());
            }
            default -> {
                startNode(FlowNode.Kind.EXPRESSION);
            }
        }
        endNode(functionDefinitionNode);
        super.visit(functionDefinitionNode);
    }

    @Override
    public void visit(ObjectFieldNode objectFieldNode) {
        objectFieldNode.expression().ifPresent(expressionNode -> expressionNode.accept(this));
        nodeBuilder.properties()
                .type(objectFieldNode.typeName())
                .variable(objectFieldNode.fieldName());
        endNode(objectFieldNode);
    }

    @Override
    public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
        for (StatementNode statementNode : functionBodyBlockNode.statements()) {
            visitStatement(statementNode);
        }
        genCommentNode(functionBodyBlockNode.closeBraceToken());
    }

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        Optional<ExpressionNode> optExpr = returnStatementNode.expression();
        if (optExpr.isEmpty()) {
            startNode(FlowNode.Kind.STOP);
        } else {
            ExpressionNode expr = optExpr.get();
            expr.accept(this);
            if (isNodeUnidentified()) {
                startNode(FlowNode.Kind.RETURN)
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
        nodeBuilder.codedata().lineRange(remoteMethodCallActionNode);
    }

    @Override
    public void visit(ClientResourceAccessActionNode clientResourceAccessActionNode) {
        String methodName = clientResourceAccessActionNode.methodName()
                .map(simpleNameReference -> simpleNameReference.name().text()).orElse("");
        ExpressionNode expression = clientResourceAccessActionNode.expression();
        SeparatedNodeList<FunctionArgumentNode> functionArgumentNodes =
                clientResourceAccessActionNode.arguments().map(ParenthesizedArgList::arguments).orElse(null);
        handleActionNode(clientResourceAccessActionNode, methodName, expression, functionArgumentNodes);
        nodeBuilder.codedata().lineRange(clientResourceAccessActionNode);
    }

    private void handleActionNode(ActionNode actionNode, String methodName, ExpressionNode expressionNode,
                                  SeparatedNodeList<FunctionArgumentNode> argumentNodes) {
        Optional<Symbol> symbol = semanticModel.symbol(actionNode);
        if (symbol.isEmpty() || (symbol.get().kind() != SymbolKind.METHOD &&
                symbol.get().kind() != SymbolKind.RESOURCE_METHOD)) {
            startNode(FlowNode.Kind.EXPRESSION);
            return;
        }

        MethodSymbol methodSymbol = (MethodSymbol) symbol.get();
        String moduleName = symbol.get().getModule().flatMap(Symbol::getName).orElse("");
        String orgName = CommonUtils.getOrgName(methodSymbol);

        FlowNode nodeTemplate = central.getNodeTemplate(
                new Codedata(FlowNode.Kind.ACTION_CALL, orgName, moduleName, "Client", methodName, null));
        if (nodeTemplate == null) {
            startNode(FlowNode.Kind.EXPRESSION);
            return;
        }

        startNode(FlowNode.Kind.ACTION_CALL)
                .metadata()
                    .label(nodeTemplate.metadata().label())
                    .description(nodeTemplate.metadata().description())
                    .icon(nodeTemplate.metadata().icon())
                    .stepOut()
                .codedata()
                    .org(nodeTemplate.codedata().org())
                    .module(nodeTemplate.codedata().module())
                    .object(nodeTemplate.codedata().object())
                    .symbol(nodeTemplate.codedata().symbol())
                    .stepOut()
                .properties()
                    .callExpression(expressionNode, Property.CONNECTION_KEY,
                        nodeTemplate.properties().get(Property.CONNECTION_KEY))
                    .variable(this.typedBindingPatternNode);
        methodSymbol.typeDescriptor().params().ifPresent(params -> nodeBuilder.properties().functionArguments(
                argumentNodes, params, nodeTemplate.properties()));
    }

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
        startNode(FlowNode.Kind.IF);

        Branch.Builder thenBranchBuilder = startBranch(If.IF_THEN_LABEL, FlowNode.Kind.CONDITIONAL,
                Branch.BranchKind.BLOCK, Branch.Repeatable.ONE_OR_MORE)
                .label(If.IF_THEN_LABEL)
                .kind(Branch.BranchKind.BLOCK)
                .repeatable(Branch.Repeatable.ONE_OR_MORE)
                .codedata().node(FlowNode.Kind.CONDITIONAL).stepOut()
                .properties().condition(ifElseStatementNode.condition()).stepOut();
        BlockStatementNode ifBody = ifElseStatementNode.ifBody();
        analyzeBlock(ifBody, thenBranchBuilder);
        endBranch(thenBranchBuilder, ifBody);

        Optional<Node> elseBody = ifElseStatementNode.elseBody();
        if (elseBody.isPresent()) {
            Branch.Builder elseBranchBuilder =
                    startBranch(If.IF_ELSE_LABEL, FlowNode.Kind.ELSE, Branch.BranchKind.BLOCK,
                            Branch.Repeatable.ZERO_OR_ONE);
            analyzeElseBody(elseBody.get(), elseBranchBuilder);
            endBranch(elseBranchBuilder, elseBody.get());
        }

        endNode(ifElseStatementNode);
    }

    private void analyzeElseBody(Node elseBody, Branch.Builder branchBuilder) {
        switch (elseBody.kind()) {
            case ELSE_BLOCK -> analyzeElseBody(((ElseBlockNode) elseBody).elseBody(), branchBuilder);
            case BLOCK_STATEMENT -> {
                analyzeBlock(((BlockStatementNode) elseBody), branchBuilder);
            }
            case IF_ELSE_STATEMENT -> {
                elseBody.accept(this);
                branchBuilder.node(buildNode());
            }
            default -> {
            }
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
        String orgName = CommonUtils.getOrgName(typeSymbol.get());
        FlowNode nodeTemplate = central.getNodeTemplate(
                new Codedata(FlowNode.Kind.NEW_CONNECTION, orgName, moduleName, "Client", "init", null));
        if (nodeTemplate == null) {
            startNode(FlowNode.Kind.EXPRESSION);
            return;
        }
        startNode(FlowNode.Kind.NEW_CONNECTION)
                .metadata().description(nodeTemplate.metadata().description()).stepOut()
                .codedata()
                    .org(nodeTemplate.codedata().org())
                    .module(nodeTemplate.codedata().module())
                    .object(nodeTemplate.codedata().object())
                    .symbol(nodeTemplate.codedata().symbol())
                    .stepOut()
                .properties().scope(connectionScope);
        try {
            MethodSymbol methodSymbol =
                    ((ClassSymbol) ((TypeReferenceTypeSymbol) typeSymbol.get()).definition()).initMethod()
                            .orElseThrow();
            methodSymbol.typeDescriptor().params().ifPresent(params -> nodeBuilder.properties().functionArguments(
                    argumentNodes, params, nodeTemplate.properties()));
        } catch (RuntimeException ignored) {

        }
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        Optional<ExpressionNode> initializer = variableDeclarationNode.initializer();
        if (initializer.isEmpty()) {
            return;
        }
        ExpressionNode initializerNode = initializer.get();
        this.typedBindingPatternNode = variableDeclarationNode.typedBindingPattern();
        initializerNode.accept(this);

        // Generate the default expression node if a node is not built
        if (isNodeUnidentified()) {
            startNode(FlowNode.Kind.NEW_DATA)
                    .metadata()
                        .description(NewData.DESCRIPTION, variableDeclarationNode.typedBindingPattern(),
                            variableDeclarationNode.typedBindingPattern().typeDescriptor())
                        .stepOut()
                    .properties().expression(initializerNode);
        }

        // TODO: Find a better way on how we can achieve this
        if (nodeBuilder instanceof DataMapper) {
            nodeBuilder.properties().data(variableDeclarationNode.typedBindingPattern());
        } else {
            nodeBuilder.properties().dataVariable(variableDeclarationNode.typedBindingPattern());
        }
        variableDeclarationNode.finalKeyword().ifPresent(token -> nodeBuilder.flag(FlowNode.NODE_FLAG_FINAL));
        endNode(variableDeclarationNode);
        this.typedBindingPatternNode = null;
    }

    @Override
    public void visit(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        Optional<ExpressionNode> initializer = moduleVariableDeclarationNode.initializer();
        if (initializer.isEmpty()) {
            return;
        }
        ExpressionNode initializerNode = initializer.get();
        this.typedBindingPatternNode = moduleVariableDeclarationNode.typedBindingPattern();
        initializerNode.accept(this);
        nodeBuilder.properties().dataVariable(moduleVariableDeclarationNode.typedBindingPattern());
        endNode(moduleVariableDeclarationNode);
        this.typedBindingPatternNode = null;
    }

    @Override
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        ExpressionNode expression = assignmentStatementNode.expression();
        expression.accept(this);

        if (isNodeUnidentified()) {
            startNode(FlowNode.Kind.UPDATE_DATA)
                    .metadata()
                        .description(String.format(UpdateData.DESCRIPTION,
                            CommonUtils.getVariableName(assignmentStatementNode.varRef())))
                        .stepOut()
                    .properties()
                        .expression(expression, UpdateData.UPDATE_DATA_EXPRESSION_DOC)
                        .variable(assignmentStatementNode.varRef());
        }

        endNode(assignmentStatementNode);
    }

    @Override
    public void visit(CompoundAssignmentStatementNode compoundAssignmentStatementNode) {
        handleDefaultStatementNode(compoundAssignmentStatementNode, () -> super.visit(compoundAssignmentStatementNode));
    }

    @Override
    public void visit(BlockStatementNode blockStatementNode) {
        handleDefaultStatementNode(blockStatementNode, () -> super.visit(blockStatementNode));
    }

    @Override
    public void visit(BreakStatementNode breakStatementNode) {
        startNode(FlowNode.Kind.BREAK);
        endNode(breakStatementNode);
    }

    @Override
    public void visit(FailStatementNode failStatementNode) {
        startNode(FlowNode.Kind.FAIL)
                .properties().expression(failStatementNode.expression(), Fail.FAIL_EXPRESSION_DOC);
        endNode(failStatementNode);
    }

    @Override
    public void visit(ExpressionStatementNode expressionStatementNode) {
        handleDefaultStatementNode(expressionStatementNode, () -> super.visit(expressionStatementNode));
    }

    @Override
    public void visit(ContinueStatementNode continueStatementNode) {
        startNode(FlowNode.Kind.CONTINUE);
        endNode(continueStatementNode);
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(functionCallExpressionNode);
        if (symbol.isEmpty() || symbol.get().kind() != SymbolKind.FUNCTION) {
            startNode(FlowNode.Kind.EXPRESSION);
            return;
        }

        FunctionSymbol functionSymbol = (FunctionSymbol) symbol.get();
        String orgName = CommonUtils.getOrgName(functionSymbol);
        NameReferenceNode nameReferenceNode = functionCallExpressionNode.functionName();

        if (nameReferenceNode.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
            String moduleName = ((QualifiedNameReferenceNode) nameReferenceNode).modulePrefix().text();
            String functionName = ((QualifiedNameReferenceNode) nameReferenceNode).identifier().text();
            FlowNode nodeTemplate = central.getNodeTemplate(
                    new Codedata(FlowNode.Kind.FUNCTION_CALL, orgName, moduleName, null, functionName, null));

            startNode(FlowNode.Kind.FUNCTION_CALL)
                    .metadata()
                        .label(nodeTemplate.metadata().label())
                        .description(nodeTemplate.metadata().description())
                        .icon(nodeTemplate.metadata().icon())
                        .stepOut()
                    .codedata()
                        .org(nodeTemplate.codedata().org())
                        .module(nodeTemplate.codedata().module())
                        .object(nodeTemplate.codedata().object())
                        .symbol(nodeTemplate.codedata().symbol());

            functionSymbol.typeDescriptor().params().ifPresent(params -> nodeBuilder.properties().functionArguments(
                    functionCallExpressionNode.arguments(), params, nodeTemplate.properties()));
        } else if (nameReferenceNode.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            SimpleNameReferenceNode simpleNameReferenceNode = (SimpleNameReferenceNode) nameReferenceNode;
            String functionName = simpleNameReferenceNode.name().text();
            if (dataMappings.contains(functionName)) {
                startNode(FlowNode.Kind.DATA_MAPPER)
                        .properties()
                        .functionName(functionName)
                        .output(this.typedBindingPatternNode);
                functionSymbol.typeDescriptor().params().ifPresent(
                        params -> nodeBuilder.properties().inputs(functionCallExpressionNode.arguments(), params));
            }
        } else {
            startNode(FlowNode.Kind.EXPRESSION);
        }
    }

    @Override
    public void visit(WhileStatementNode whileStatementNode) {
        startNode(FlowNode.Kind.WHILE)
                .properties().condition(whileStatementNode.condition());

        BlockStatementNode whileBody = whileStatementNode.whileBody();
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, FlowNode.Kind.CONDITIONAL, Branch.BranchKind.BLOCK,
                        Branch.Repeatable.ONE);
        analyzeBlock(whileBody, branchBuilder);
        endBranch(branchBuilder, whileBody);
        whileStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(whileStatementNode);
    }

    private void processOnFailClause(OnFailClauseNode onFailClauseNode) {
        Branch.Builder branchBuilder =
                startBranch(Branch.ON_FAILURE_LABEL, FlowNode.Kind.ON_FAILURE, Branch.BranchKind.BLOCK,
                        Branch.Repeatable.ZERO_OR_ONE);
        if (onFailClauseNode.typedBindingPattern().isPresent()) {
            branchBuilder.properties().ignore().onErrorVariable(onFailClauseNode.typedBindingPattern().get());
        }
        BlockStatementNode onFailClauseBlock = onFailClauseNode.blockStatement();
        analyzeBlock(onFailClauseBlock, branchBuilder);
        endBranch(branchBuilder, onFailClauseBlock);
    }

    @Override
    public void visit(PanicStatementNode panicStatementNode) {
        startNode(FlowNode.Kind.PANIC)
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
        startNode(FlowNode.Kind.START)
                .properties().expression(startActionNode.expression(), Start.START_EXPRESSION_DOC);
        endNode(startActionNode);
    }

    @Override
    public void visit(LockStatementNode lockStatementNode) {
        startNode(FlowNode.Kind.LOCK);
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, FlowNode.Kind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
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
        startNode(FlowNode.Kind.TRANSACTION);
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, FlowNode.Kind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        BlockStatementNode blockStatementNode = transactionStatementNode.blockStatement();
        analyzeBlock(blockStatementNode, branchBuilder);
        endBranch(branchBuilder, blockStatementNode);
        transactionStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(transactionStatementNode);
    }

    @Override
    public void visit(ForEachStatementNode forEachStatementNode) {
        startNode(FlowNode.Kind.FOREACH)
                .properties()
                .dataVariable(forEachStatementNode.typedBindingPattern())
                .collection(forEachStatementNode.actionOrExpressionNode());
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, FlowNode.Kind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        BlockStatementNode blockStatementNode = forEachStatementNode.blockStatement();
        analyzeBlock(blockStatementNode, branchBuilder);
        endBranch(branchBuilder, blockStatementNode);
        forEachStatementNode.onFailClause().ifPresent(this::processOnFailClause);
        endNode(forEachStatementNode);
    }

    @Override
    public void visit(RollbackStatementNode rollbackStatementNode) {
        handleDefaultStatementNode(rollbackStatementNode, () -> super.visit(rollbackStatementNode));
    }

    @Override
    public void visit(RetryStatementNode retryStatementNode) {
        handleDefaultStatementNode(retryStatementNode, () -> super.visit(retryStatementNode));
    }

    @Override
    public void visit(MatchStatementNode matchStatementNode) {
        handleDefaultStatementNode(matchStatementNode, () -> super.visit(matchStatementNode));
    }

    @Override
    public void visit(DoStatementNode doStatementNode) {
        Optional<OnFailClauseNode> optOnFailClauseNode = doStatementNode.onFailClause();
        BlockStatementNode blockStatementNode = doStatementNode.blockStatement();
        if (optOnFailClauseNode.isEmpty()) {
            handleDefaultNodeWithBlock(blockStatementNode);
            return;
        }

        startNode(FlowNode.Kind.ERROR_HANDLER);
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, FlowNode.Kind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        analyzeBlock(blockStatementNode, branchBuilder);
        endBranch(branchBuilder, blockStatementNode);
        processOnFailClause(optOnFailClauseNode.get());
        endNode(doStatementNode);
    }

    @Override
    public void visit(CheckExpressionNode checkExpressionNode) {
        checkExpressionNode.expression().accept(this);
        if (isNodeUnidentified()) {
            startNode(FlowNode.Kind.EXPRESSION)
                    .properties()
                    .expression(checkExpressionNode);
        }

        String checkText = checkExpressionNode.checkKeyword().text();
        switch (checkText) {
            case Constants.CHECK -> nodeBuilder.flag(FlowNode.NODE_FLAG_CHECKED);
            case Constants.CHECKPANIC -> nodeBuilder.flag(FlowNode.NODE_FLAG_CHECKPANIC);
            default -> throw new IllegalStateException("Unexpected value: " + checkText);

        }
        nodeBuilder.codedata().lineRange(checkExpressionNode);
    }

    @Override
    public void visit(MappingConstructorExpressionNode mappingCtrExprNode) {
        handleConstructorExpressionNode(mappingCtrExprNode, "map");
    }

    @Override
    public void visit(ListConstructorExpressionNode listCtrExprNode) {
        handleConstructorExpressionNode(listCtrExprNode, "list");
    }

    private void handleConstructorExpressionNode(ExpressionNode constructorExprNode, String typeDescription) {
        SyntaxKind kind = constructorExprNode.parent().kind();
        if (kind == SyntaxKind.LOCAL_VAR_DECL || kind == SyntaxKind.MODULE_VAR_DECL) {
            startNode(FlowNode.Kind.NEW_DATA)
                    .metadata()
                        .description(NewData.DESCRIPTION, this.typedBindingPatternNode, typeDescription)
                        .stepOut()
                    .properties().expression(constructorExprNode);
        } else if (kind == SyntaxKind.ASSIGNMENT_STATEMENT) {
            startNode(FlowNode.Kind.UPDATE_DATA).properties()
                    .expression(constructorExprNode)
                    .variable(((AssignmentStatementNode) constructorExprNode.parent()).varRef());
        }
    }

    // Utility methods

    /**
     * It's the responsibility of the parent node to add the children nodes when building the diagram. Hence, the method
     * only adds the node to the diagram if there is no active parent node which is building its branches.
     */
    private void endNode(Node node) {
        endNode(node.lineRange());
    }

    private void endNode(LineRange lineRange) {
        nodeBuilder.codedata().lineRange(lineRange);

        if (this.flowNodeBuilderStack.isEmpty()) {
            this.flowNodeList.add(buildNode());
        }
    }

    private NodeBuilder startNode(FlowNode.Kind kind) {
        this.nodeBuilder = NodeBuilder.getNodeFromKind(kind).semanticModel(semanticModel);
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
    private Branch.Builder startBranch(String label, FlowNode.Kind node, Branch.BranchKind kind,
                                       Branch.Repeatable repeatable) {
        this.flowNodeBuilderStack.push(nodeBuilder);
        this.nodeBuilder = null;
        return new Branch.Builder()
                .semanticModel(semanticModel)
                .codedata().node(node).stepOut()
                .label(label)
                .kind(kind)
                .repeatable(repeatable);
    }

    /**
     * Ends the current branch and sets the node builder to the parent node.
     */
    private void endBranch(Branch.Builder branchBuilder, Node node) {
        branchBuilder.codedata().lineRange(node);
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
        startNode(FlowNode.Kind.EXPRESSION)
                .properties().statement(statementNode);
        runnable.run();
        endNode(statementNode);
    }

    /**
     * The default procedure to handle the node with a block statement.
     *
     * @param bodyNode the block statement node
     */
    private void handleDefaultNodeWithBlock(BlockStatementNode bodyNode) {
        startNode(FlowNode.Kind.EXPRESSION);
        Branch.Builder branchBuilder =
                startBranch(Branch.BODY_LABEL, FlowNode.Kind.BODY, Branch.BranchKind.BLOCK, Branch.Repeatable.ONE);
        analyzeBlock(bodyNode, branchBuilder);
        endBranch(branchBuilder, bodyNode);
        endNode(bodyNode);
    }

    private void analyzeBlock(BlockStatementNode blockStatement, Branch.Builder thenBranchBuilder) {
        for (StatementNode statement : blockStatement.statements()) {
            genCommentNode(statement, thenBranchBuilder);
            statement.accept(this);
            thenBranchBuilder.node(buildNode());
        }
        genCommentNode(blockStatement.closeBraceToken(), thenBranchBuilder);
    }

    private void visitStatement(StatementNode statementNode) {
        genCommentNode(statementNode);
        statementNode.accept(this);
    }

    private LineRange getCommentPosition(Node commentAttachedNode) {
        TextRange textRangeWithMinutiae = commentAttachedNode.textRangeWithMinutiae();
        TextRange textRange = commentAttachedNode.textRange();
        LinePosition startPos = textDocument.linePositionFrom(textRangeWithMinutiae.startOffset());
        LinePosition endPos = textDocument.linePositionFrom(textRange.startOffset());

        return LineRange.from(commentAttachedNode.lineRange().fileName(), startPos, endPos);
    }

    private String getComment(MinutiaeList minutiaes) {
        int size = minutiaes.size();
        List<String> commentLines = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String minutiae = minutiaes.get(i).text();
            String[] splits = minutiae.split("// ");
            if (splits.length >= 2) {
                commentLines.add(splits[1]);
            }
        }
        if (commentLines.isEmpty()) {
            return "";
        }
        StringBuilder commentBuilder = new StringBuilder();
        commentBuilder.append(commentLines.get(0));
        for (int i = 1; i < commentLines.size(); i++) {
            commentBuilder.append("\n");
            commentBuilder.append(commentLines.get(i));
        }
        return commentBuilder.toString();
    }

    private void genCommentNode(Node node, Branch.Builder builder) {
        String comment = getComment(node.leadingMinutiae());
        if (comment.isEmpty()) {
            return;
        }
        genCommentNode(node, comment);
        builder.node(buildNode());
    }

    private void genCommentNode(Node node) {
        String comment = getComment(node.leadingMinutiae());
        if (comment.isEmpty()) {
            return;
        }
        genCommentNode(node, comment);
    }

    private void genCommentNode(Node node, String comment) {
        startNode(FlowNode.Kind.COMMENT)
                .metadata()
                .description(Comment.DESCRIPTION)
                .stepOut()
                .properties()
                .comment(comment);
        endNode(getCommentPosition(node));
    }

    public List<FlowNode> getFlowNodes() {
        return flowNodeList;
    }
}
