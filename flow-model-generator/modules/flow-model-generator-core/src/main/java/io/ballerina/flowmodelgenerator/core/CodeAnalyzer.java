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
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
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
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.LocalTypeDefinitionStatementNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.OnFailClauseNode;
import io.ballerina.compiler.syntax.tree.PanicStatementNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.RetryStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.RollbackStatementNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.TransactionStatementNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.Client;
import io.ballerina.flowmodelgenerator.core.model.ExpressionAttributes;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeAttributes;
import io.ballerina.flowmodelgenerator.core.model.node.Break;
import io.ballerina.flowmodelgenerator.core.model.node.ActionCall;
import io.ballerina.flowmodelgenerator.core.model.node.Continue;
import io.ballerina.flowmodelgenerator.core.model.node.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.node.ErrorHandler;
import io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent;
import io.ballerina.flowmodelgenerator.core.model.node.If;
import io.ballerina.flowmodelgenerator.core.model.node.PanicNode;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.While;

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
    private final List<Client> clients;
    private FlowNode.NodeBuilder nodeBuilder;
    private final Client.Builder clientBuilder;
    private final SemanticModel semanticModel;
    private final Stack<FlowNode.NodeBuilder> flowNodeBuilderStack;
    private TypedBindingPatternNode typedBindingPatternNode;

    public CodeAnalyzer(SemanticModel semanticModel) {
        this.flowNodeList = new ArrayList<>();
        this.clientBuilder = new Client.Builder();
        this.nodeBuilder = new FlowNode.NodeBuilder(semanticModel);
        this.semanticModel = semanticModel;
        this.flowNodeBuilderStack = new Stack<>();
        this.clients = new ArrayList<>();
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(functionDefinitionNode);
        if (symbol.isEmpty()) {
            return;
        }

        switch (symbol.get().kind()) {
            case RESOURCE_METHOD -> {
                nodeBuilder
                        .metadata(HttpApiEvent.EVENT_HTTP_API_LABEL, FlowNode.Kind.EVENT_HTTP_API, null, null,
                                HttpApiEvent::new)
                        .flag(FlowNode.NODE_FLAG_RESOURCE)
                        .fixed()
                        .properties()
                        .resourceSymbol((ResourceMethodSymbol) symbol.get());
            }
            default -> {
                nodeBuilder.metadata(DefaultExpression.EXPRESSION_LABEL, FlowNode.Kind.EXPRESSION, null, null,
                        DefaultExpression::new);
            }
        }
        nodeBuilder.lineRange(functionDefinitionNode);

        appendNode();
        super.visit(functionDefinitionNode);
    }

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        Optional<ExpressionNode> expression = returnStatementNode.expression();
        nodeBuilder.lineRange(returnStatementNode)
                .metadata(Return.RETURN_LABEL, FlowNode.Kind.RETURN, null, null, Return::new);

        expression.ifPresent(expressionNode -> expressionNode.accept(this));
        if (nodeBuilder.isDefault()) {
            expression.ifPresent(expressionNode -> nodeBuilder.properties()
                    .setExpressionNode(expressionNode, Return.RETURN_EXPRESSION_DOC));
        }
        nodeBuilder.returning();
        appendNode();
    }

    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        nodeBuilder.lineRange(remoteMethodCallActionNode);
        String methodName = remoteMethodCallActionNode.methodName().name().text();
        ExpressionNode expression = remoteMethodCallActionNode.expression();
        SeparatedNodeList<FunctionArgumentNode> argumentNodes = remoteMethodCallActionNode.arguments();
        handleActionNode(remoteMethodCallActionNode, methodName, expression, argumentNodes, null);
    }

    @Override
    public void visit(ClientResourceAccessActionNode clientResourceAccessActionNode) {
        nodeBuilder.lineRange(clientResourceAccessActionNode);
        String methodName = clientResourceAccessActionNode.methodName()
                .map(simpleNameReference -> simpleNameReference.name().text()).orElse("");
        ExpressionNode expression = clientResourceAccessActionNode.expression();
        SeparatedNodeList<FunctionArgumentNode> functionArgumentNodes =
                clientResourceAccessActionNode.arguments().map(ParenthesizedArgList::arguments).orElse(null);

        handleActionNode(clientResourceAccessActionNode, methodName, expression, functionArgumentNodes,
                clientResourceAccessActionNode.resourceAccessPath());
    }

    private void handleActionNode(ActionNode actionNode, String methodName, ExpressionNode expressionNode,
                                  SeparatedNodeList<FunctionArgumentNode> argumentNodes,
                                  SeparatedNodeList<Node> resourceAccessPathNodes) {
        Optional<Symbol> symbol = semanticModel.symbol(actionNode);
        if (symbol.isEmpty() || (symbol.get().kind() != SymbolKind.METHOD &&
                symbol.get().kind() != SymbolKind.RESOURCE_METHOD)) {
            return;
        }

        MethodSymbol methodSymbol = (MethodSymbol) symbol.get();
        String moduleName = symbol.get().getModule().flatMap(Symbol::getName).orElse("");

        if (moduleName.equals("http")) {
            NodeAttributes.Info info = NodeAttributes.get(methodName);
            nodeBuilder.metadata(info.label(), info.kind(), null, null, ActionCall::new)
                    .properties()
                    .callExpression(expressionNode, ExpressionAttributes.HTTP_CLIENT)
                    .variable(this.typedBindingPatternNode);
            methodSymbol.typeDescriptor().params().ifPresent(params -> nodeBuilder.properties().functionArguments(
                    argumentNodes, params));
        }
    }

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
        nodeBuilder
                .metadata(If.IF_LABEL, FlowNode.Kind.IF, null, null, If::new)
                .lineRange(ifElseStatementNode)
                .properties().setConditionExpression(ifElseStatementNode.condition());

        Branch.Builder thenBranchBuilder = startBranch(If.IF_THEN_LABEL, Branch.BranchKind.BLOCK);
        for (StatementNode statement : ifElseStatementNode.ifBody().statements()) {
            statement.accept(this);
            thenBranchBuilder.node(buildNode());
        }
        endBranch(thenBranchBuilder);

        Optional<Node> elseBody = ifElseStatementNode.elseBody();
        if (elseBody.isPresent()) {
            Branch.Builder elseBranchBuilder = startBranch(If.IF_ELSE_LABEL, Branch.BranchKind.BLOCK);
            List<FlowNode> elseBodyChildNodes = analyzeElseBody(elseBody.get());
            elseBranchBuilder.nodes(elseBodyChildNodes);
            endBranch(elseBranchBuilder);
        }

        appendNode();
    }

    private List<FlowNode> analyzeElseBody(Node elseBody) {
        return switch (elseBody.kind()) {
            case ELSE_BLOCK -> analyzeElseBody(((ElseBlockNode) elseBody).elseBody());
            case BLOCK_STATEMENT -> {
                List<FlowNode> elseNodes = new ArrayList<>();
                for (StatementNode statement : ((BlockStatementNode) elseBody).statements()) {
                    statement.accept(this);
                    elseNodes.add(buildNode());
                }
                yield elseNodes;
            }
            case IF_ELSE_STATEMENT -> {
                elseBody.accept(this);
                yield List.of(buildNode());
            }
            default -> new ArrayList<>();
        };
    }

    @Override
    public void visit(ImplicitNewExpressionNode implicitNewExpressionNode) {
        checkForPossibleClient(implicitNewExpressionNode);
        super.visit(implicitNewExpressionNode);
    }

    @Override
    public void visit(ExplicitNewExpressionNode explicitNewExpressionNode) {
        checkForPossibleClient(explicitNewExpressionNode);
        super.visit(explicitNewExpressionNode);
    }

    private void checkForPossibleClient(NewExpressionNode newExpressionNode) {
        this.clientBuilder.setTypedBindingPattern(this.typedBindingPatternNode);
        semanticModel.typeOf(CommonUtils.getExpressionWithCheck(newExpressionNode))
                .flatMap(symbol -> CommonUtils.buildClient(this.clientBuilder, symbol, Client.ClientScope.LOCAL))
                .ifPresent(clients::add);
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        Optional<ExpressionNode> initializer = variableDeclarationNode.initializer();
        nodeBuilder.lineRange(variableDeclarationNode)
                .metadata(DefaultExpression.EXPRESSION_LABEL, FlowNode.Kind.EXPRESSION, null, null,
                        DefaultExpression::new);
        if (initializer.isEmpty()) {
            return;
        }
        variableDeclarationNode.finalKeyword().ifPresent(token -> nodeBuilder.flag(FlowNode.NODE_FLAG_FINAL));
        ExpressionNode initializerNode = initializer.get();
        this.typedBindingPatternNode = variableDeclarationNode.typedBindingPattern();
        initializerNode.accept(this);

        // Generate the default expression node if a node is not built
        if (nodeBuilder.isDefault()) {
            nodeBuilder.properties()
                    .expression(initializerNode)
                    .variable(this.typedBindingPatternNode);
        }

        appendNode();
        this.typedBindingPatternNode = null;
    }

    @Override
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        ExpressionNode expression = assignmentStatementNode.expression();
        expression.accept(this);

        if (nodeBuilder.isDefault()) {
            nodeBuilder.lineRange(assignmentStatementNode)
                    .metadata(DefaultExpression.EXPRESSION_LABEL, FlowNode.Kind.EXPRESSION, null, null,
                            DefaultExpression::new)
                    .properties()
                    .expression(expression)
                    .variable(assignmentStatementNode.varRef());
        }

        appendNode();
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
        nodeBuilder.lineRange(breakStatementNode)
                .metadata(Break.BREAK_LABEL, FlowNode.Kind.BREAK, null, null, Break::new);
        appendNode();
    }

    @Override
    public void visit(FailStatementNode failStatementNode) {
        handleDefaultStatementNode(failStatementNode, () -> super.visit(failStatementNode));
    }

    @Override
    public void visit(ExpressionStatementNode expressionStatementNode) {
        handleDefaultStatementNode(expressionStatementNode, () -> super.visit(expressionStatementNode));
    }

    @Override
    public void visit(ContinueStatementNode continueStatementNode) {
        nodeBuilder.lineRange(continueStatementNode)
                .metadata(Continue.CONTINUE_LABEL, FlowNode.Kind.CONTINUE, null, null,
                        Continue::new);
        appendNode();
    }

    @Override
    public void visit(WhileStatementNode whileStatementNode) {
        nodeBuilder.lineRange(whileStatementNode)
                .metadata(While.WHILE_LABEL, FlowNode.Kind.WHILE, null, null, While::new)
                .properties().setConditionExpression(whileStatementNode.condition());

        BlockStatementNode whileBody = whileStatementNode.whileBody();
        Branch.Builder branchBuilder = startBranch(Branch.BODY_LABEL, Branch.BranchKind.BLOCK);
        for (StatementNode statement : whileBody.statements()) {
            statement.accept(this);
            branchBuilder.node(buildNode());
        }
        endBranch(branchBuilder);

        Optional<OnFailClauseNode> optOnFailClauseNode = whileStatementNode.onFailClause();
        if (optOnFailClauseNode.isPresent()) {
            Branch.Builder onFailBranchBuilder = startBranch(Branch.ON_FAIL_LABEL, Branch.BranchKind.BLOCK);
            OnFailClauseNode onFailClauseNode = optOnFailClauseNode.get();
            onFailClauseNode.typedBindingPattern().ifPresent(onFailBranchBuilder::variable);
            for (StatementNode statement : onFailClauseNode.blockStatement().statements()) {
                statement.accept(this);
                onFailBranchBuilder.node(buildNode());
            }
            endBranch(onFailBranchBuilder);
        }

        appendNode();
    }

    @Override
    public void visit(PanicStatementNode panicStatementNode) {
        nodeBuilder.lineRange(panicStatementNode);

        PanicNode.Builder panicNodeBuilder = new PanicNode.Builder(semanticModel);
        panicNodeBuilder.setExpressionNode(panicStatementNode.expression());
        nodeBuilder.propertiesBuilder(panicNodeBuilder);

        appendNode();
    }

    @Override
    public void visit(LocalTypeDefinitionStatementNode localTypeDefinitionStatementNode) {
        handleDefaultStatementNode(localTypeDefinitionStatementNode,
                () -> super.visit(localTypeDefinitionStatementNode));
    }

    @Override
    public void visit(LockStatementNode lockStatementNode) {
        handleDefaultStatementNode(lockStatementNode, () -> super.visit(lockStatementNode));
    }

    @Override
    public void visit(ForkStatementNode forkStatementNode) {
        handleDefaultStatementNode(forkStatementNode, () -> super.visit(forkStatementNode));
    }

    @Override
    public void visit(TransactionStatementNode transactionStatementNode) {
        handleDefaultNodeWithBlock(transactionStatementNode.blockStatement());
    }

    @Override
    public void visit(ForEachStatementNode forEachStatementNode) {
        handleDefaultStatementNode(forEachStatementNode, () -> super.visit(forEachStatementNode));
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
        if (optOnFailClauseNode.isEmpty()) {
            handleDefaultNodeWithBlock(doStatementNode.blockStatement());
            return;
        }

        nodeBuilder.lineRange(doStatementNode)
                .metadata(ErrorHandler.ERROR_HANDLER_LABEL, FlowNode.Kind.ERROR_HANDLER, null, null,
                        ErrorHandler::new);
        Branch.Builder branchBuilder = startBranch(Branch.BODY_LABEL, Branch.BranchKind.BLOCK);
        for (StatementNode statement : doStatementNode.blockStatement().statements()) {
            statement.accept(this);
            branchBuilder.node(buildNode());
        }
        endBranch(branchBuilder);

        OnFailClauseNode onFailClauseNode = optOnFailClauseNode.get();
        Branch.Builder onFailBranchBuilder = startBranch(Branch.ON_FAIL_LABEL, Branch.BranchKind.BLOCK);
        onFailClauseNode.typedBindingPattern().ifPresent(onFailBranchBuilder::variable);
        for (StatementNode statement : onFailClauseNode.blockStatement().statements()) {
            statement.accept(this);
            onFailBranchBuilder.node(buildNode());
        }
        endBranch(onFailBranchBuilder);
        appendNode();
    }

    @Override
    public void visit(CheckExpressionNode checkExpressionNode) {
        nodeBuilder.lineRange(checkExpressionNode);
        switch (checkExpressionNode.checkKeyword().text()) {
            case Constants.CHECK -> nodeBuilder.flag(FlowNode.NODE_FLAG_CHECKED);
            case Constants.CHECKPANIC -> nodeBuilder.flag(FlowNode.NODE_FLAG_CHECKPANIC);
            default -> {
            }
        }
        checkExpressionNode.expression().accept(this);
    }

    // Utility methods

    /**
     * It's the responsibility of the parent node to add the children nodes when building the diagram. Hence, the method
     * only adds the node to the diagram if there is no active parent node which is building its branches.
     */
    private void appendNode() {
        if (this.flowNodeBuilderStack.isEmpty()) {
            this.flowNodeList.add(buildNode());
        }
    }

    /**
     * Starts a new branch and sets the node builder to the starting node of the branch.
     */
    private Branch.Builder startBranch(String label, Branch.BranchKind kind) {
        this.flowNodeBuilderStack.push(nodeBuilder);
        nodeBuilder = new FlowNode.NodeBuilder(semanticModel);
        return new Branch.Builder(semanticModel).label(label).kind(kind);
    }

    /**
     * Ends the current branch and sets the node builder to the parent node.
     */
    private void endBranch(Branch.Builder branchBuilder) {
        nodeBuilder = this.flowNodeBuilderStack.pop();
        nodeBuilder.branch(branchBuilder.build());
    }

    /**
     * Builds the flow node and resets the node builder.
     *
     * @return the built flow node
     */
    private FlowNode buildNode() {
        FlowNode flowNode = nodeBuilder.build();
        nodeBuilder = new FlowNode.NodeBuilder(semanticModel);
        return flowNode;
    }

    /**
     * The default procedure to handle the statement nodes. These nodes should be handled explicitly.
     *
     * @param statementNode the statement node
     * @param runnable      The runnable to be called to analyze the child nodes.
     */
    private void handleDefaultStatementNode(NonTerminalNode statementNode, Runnable runnable) {
        nodeBuilder.lineRange(statementNode)
                .metadata(DefaultExpression.EXPRESSION_LABEL, FlowNode.Kind.EXPRESSION, null, null,
                        DefaultExpression::new);
        runnable.run();
        appendNode();
    }

    /**
     * The default procedure to handle the node with a block statement.
     *
     * @param bodyNode the block statement node
     */
    private void handleDefaultNodeWithBlock(BlockStatementNode bodyNode) {
        nodeBuilder.metadata(DefaultExpression.EXPRESSION_LABEL, FlowNode.Kind.EXPRESSION, null, null,
                DefaultExpression::new);
        Branch.Builder branchBuilder = startBranch(Branch.BODY_LABEL, Branch.BranchKind.BLOCK);
        for (StatementNode statement : bodyNode.statements()) {
            statement.accept(this);
            branchBuilder.node(buildNode());
        }
        endBranch(branchBuilder);
        appendNode();
    }

    public List<FlowNode> getFlowNodes() {
        return flowNodeList;
    }

    public List<Client> getClients() {
        return clients;
    }
}