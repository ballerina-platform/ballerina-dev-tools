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
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.ActionNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BreakStatementNode;
import io.ballerina.compiler.syntax.tree.ClientResourceAccessActionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ContinueStatementNode;
import io.ballerina.compiler.syntax.tree.DoStatementNode;
import io.ballerina.compiler.syntax.tree.ElseBlockNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FailStatementNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.ForkStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.LocalTypeDefinitionStatementNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.PanicStatementNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.RetryStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.RollbackStatementNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TransactionStatementNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.properties.HttpGetNodeProperties;
import io.ballerina.flowmodelgenerator.core.model.properties.IfNodeProperties;
import io.ballerina.flowmodelgenerator.core.model.properties.NodePropertiesBuilder;
import io.ballerina.flowmodelgenerator.core.model.properties.ReturnNodeProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

/**
 * Analyzes the source code and generates the flow model.
 *
 * @since 2201.9.0
 */
class CodeAnalyzer extends NodeVisitor {

    private final List<FlowNode> flowNodeList;
    private FlowNode.Builder nodeBuilder;
    private final SemanticModel semanticModel;
    private final Stack<FlowNode.Builder> flowNodeBuilderStack;

    public CodeAnalyzer(SemanticModel semanticModel) {
        this.flowNodeList = new ArrayList<>();
        this.nodeBuilder = new FlowNode.Builder();
        this.semanticModel = semanticModel;
        this.flowNodeBuilderStack = new Stack<>();
    }

    @Override
    public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
        super.visit(functionBodyBlockNode);
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        handleDefaultStatementNode(variableDeclarationNode, () -> super.visit(variableDeclarationNode));
    }

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        this.nodeBuilder.kind(FlowNode.NodeKind.RETURN);
        this.nodeBuilder.returning(true);
        this.nodeBuilder.setNode(returnStatementNode);

        ReturnNodeProperties.Builder returnNodePropertiesBuilder = new ReturnNodeProperties.Builder(semanticModel);
        returnStatementNode.expression().ifPresent(returnNodePropertiesBuilder::setExpression);
        addNodeProperties(returnNodePropertiesBuilder);
        appendNode();
    }

    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        String methodName = remoteMethodCallActionNode.methodName().name().text();
        ExpressionNode expression = remoteMethodCallActionNode.expression();
        SeparatedNodeList<FunctionArgumentNode> argumentNodes = remoteMethodCallActionNode.arguments();
        handleActionNode(remoteMethodCallActionNode, methodName, expression, argumentNodes, null);
    }

    @Override
    public void visit(ClientResourceAccessActionNode clientResourceAccessActionNode) {
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
        NonTerminalNode parentNode = actionNode.parent();
        NonTerminalNode statementNode = parentNode.kind() == SyntaxKind.CHECK_EXPRESSION ?
                parentNode : actionNode;
        this.nodeBuilder.setNode(actionNode.parent());

        Optional<Symbol> symbol = semanticModel.symbol(actionNode);
        if (symbol.isEmpty() || (symbol.get().kind() != SymbolKind.METHOD &&
                symbol.get().kind() != SymbolKind.RESOURCE_METHOD)) {
            return;
        }

        MethodSymbol methodSymbol = (MethodSymbol) symbol.get();
        String moduleName = symbol.get().getModule().flatMap(Symbol::getName).orElse("");

        switch (moduleName) {
            case "http" -> {
                switch (methodName) {
                    case "get" -> {
                        HttpGetNodeProperties.Builder httpGetNodePropertiesBuilder =
                                new HttpGetNodeProperties.Builder(semanticModel);
                        nodeBuilder.label("HTTP GET Call");
                        nodeBuilder.kind(FlowNode.NodeKind.HTTP_API_GET_CALL);
                        httpGetNodePropertiesBuilder.addClient(expressionNode);
                        httpGetNodePropertiesBuilder.addTargetTypeValue(statementNode);
                        httpGetNodePropertiesBuilder.addFunctionArguments(argumentNodes);
                        httpGetNodePropertiesBuilder.addHttpParameters(methodSymbol.typeDescriptor().params().get());
                        httpGetNodePropertiesBuilder.addResourceAccessPath(resourceAccessPathNodes);
                        addNodeProperties(httpGetNodePropertiesBuilder);
                    }
                    case "post" -> {
                        nodeBuilder.label("HTTP POST Call");
                        nodeBuilder.kind(FlowNode.NodeKind.HTTP_API_POST_CALL);
                    }
                    default -> {
                    }
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
        this.nodeBuilder.kind(FlowNode.NodeKind.IF);
        this.nodeBuilder.label("If block");
        this.nodeBuilder.setNode(ifElseStatementNode);
        IfNodeProperties.Builder ifNodePropertiesBuilder = new IfNodeProperties.Builder(semanticModel);
        ifNodePropertiesBuilder.setConditionExpression(ifElseStatementNode.condition());

        BlockStatementNode ifBody = ifElseStatementNode.ifBody();
        List<FlowNode> ifNodes = new ArrayList<>();
        stepIn();
        this.nodeBuilder.kind(FlowNode.NodeKind.BLOCK);
        this.nodeBuilder.label("Then block");
        this.nodeBuilder.setNode(ifBody);
        stepIn();
        for (StatementNode statement : ifBody.statements()) {
            statement.accept(this);
            ifNodes.add(buildNode());
        }
        stepOut();
        this.nodeBuilder.children(ifNodes);
        ifNodePropertiesBuilder.setThenBranchNode(buildNode());
        stepOut();

        Optional<Node> elseBody = ifElseStatementNode.elseBody();
        if (elseBody.isPresent()) {
            Node elseBodyNode = elseBody.get();
            stepIn();
            this.nodeBuilder.kind(FlowNode.NodeKind.BLOCK);
            this.nodeBuilder.label("Else block");
            this.nodeBuilder.setNode(elseBodyNode);
            stepIn();
            List<FlowNode> elseBodyChildNodes = analyzeElseBody(elseBodyNode);
            stepOut();
            this.nodeBuilder.children(elseBodyChildNodes);
            ifNodePropertiesBuilder.setElseBranchNode(buildNode());
            stepOut();
        }

        addNodeProperties(ifNodePropertiesBuilder);
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
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        handleDefaultStatementNode(assignmentStatementNode, () -> super.visit(assignmentStatementNode));
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
        handleDefaultStatementNode(breakStatementNode, () -> super.visit(breakStatementNode));
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
        handleDefaultStatementNode(continueStatementNode, () -> super.visit(continueStatementNode));
    }

    @Override
    public void visit(WhileStatementNode whileStatementNode) {
        handleDefaultStatementNode(whileStatementNode, () -> super.visit(whileStatementNode));
    }

    @Override
    public void visit(PanicStatementNode panicStatementNode) {
        handleDefaultStatementNode(panicStatementNode, () -> super.visit(panicStatementNode));
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
        handleDefaultStatementNode(transactionStatementNode, () -> super.visit(transactionStatementNode));
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
        handleDefaultStatementNode(doStatementNode, () -> super.visit(doStatementNode));
    }

    private void appendNode() {
        if (this.flowNodeBuilderStack.isEmpty()) {
            this.flowNodeList.add(buildNode());
        }
    }

    private void stepIn() {
        this.flowNodeBuilderStack.push(this.nodeBuilder);
        this.nodeBuilder = new FlowNode.Builder();
    }

    private void stepOut() {
        this.nodeBuilder = this.flowNodeBuilderStack.pop();
    }

    private FlowNode buildNode() {
        FlowNode flowNode = this.nodeBuilder.build();
        this.nodeBuilder = new FlowNode.Builder();
        return flowNode;
    }

    private void addNodeProperties(NodePropertiesBuilder nodePropertiesBuilder) {
        this.nodeBuilder.nodeProperties(nodePropertiesBuilder.build());
    }

    private void handleDefaultStatementNode(StatementNode statementNode, Runnable runnable) {
        this.nodeBuilder.setNode(statementNode);
        this.nodeBuilder.label(statementNode.toSourceCode());
        this.nodeBuilder.kind(FlowNode.NodeKind.EXPRESSION);
        runnable.run();
        appendNode();
    }

    public List<FlowNode> getFlowNodes() {
        return flowNodeList;
    }
}
