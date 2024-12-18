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

package io.ballerina.sequencemodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.ClientResourceAccessActionNode;
import io.ballerina.compiler.syntax.tree.ElseBlockNode;
import io.ballerina.compiler.syntax.tree.ExpressionFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.sequencemodelgenerator.core.model.Expression;
import io.ballerina.sequencemodelgenerator.core.model.Interaction;
import io.ballerina.sequencemodelgenerator.core.model.SequenceNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Analyzes the body of a participant.
 *
 * @since 2.0.0
 */
public class ParticipantBodyAnalyzer extends NodeVisitor {

    private final List<SequenceNode> sequenceNodes;
    private final SemanticModel semanticModel;
    private final Stack<SequenceNode.Builder> nodeBuilderStack;
    private SequenceNode.Builder nodeBuilder;
    private Node variableNode;

    public ParticipantBodyAnalyzer(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.sequenceNodes = new ArrayList<>();
        this.nodeBuilderStack = new Stack<>();
        this.nodeBuilder = new SequenceNode.Builder(semanticModel);
    }

    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        String targetId = ParticipantManager.getInstance().getParticipantId(remoteMethodCallActionNode.expression());

        nodeBuilder = new Interaction.Builder(semanticModel)
                .interactionType(Interaction.InteractionType.ENDPOINT_CALL)
                .targetId(targetId)
                .location(remoteMethodCallActionNode);

        nodeBuilder
                .property(Interaction.PARAMS_LABEL, getParamList(remoteMethodCallActionNode.arguments()))
                .property(Interaction.NAME_LABEL,
                        Expression.Factory.createStringType(remoteMethodCallActionNode.methodName()))
                .property(Interaction.EXPRESSION_LABEL, remoteMethodCallActionNode.expression())
                .property(Interaction.VALUE_LABEL, Expression.Factory.create(semanticModel,
                        remoteMethodCallActionNode, variableNode));

        appendNode();
    }

    @Override
    public void visit(ClientResourceAccessActionNode resourceAccessActionNode) {
        String targetId = ParticipantManager.getInstance().getParticipantId(resourceAccessActionNode.expression());

        nodeBuilder = new Interaction.Builder(semanticModel)
                .interactionType(Interaction.InteractionType.ENDPOINT_CALL)
                .targetId(targetId)
                .location(resourceAccessActionNode);

        resourceAccessActionNode.arguments().ifPresent(arguments -> {
            nodeBuilder.property(Interaction.PARAMS_LABEL, getParamList(arguments.arguments()));
        });
        resourceAccessActionNode.methodName().ifPresent(name -> {
            nodeBuilder.property(Interaction.NAME_LABEL, Expression.Factory.createStringType(name));
        });

        SeparatedNodeList<Node> nodes = resourceAccessActionNode.resourceAccessPath();
        String resourcePath = nodes.stream()
                .map(Node::toSourceCode)
                .collect(Collectors.joining("/"));
        nodeBuilder.property(Interaction.RESOURCE_PATH, resourcePath);

        nodeBuilder
                .property(Interaction.EXPRESSION_LABEL, resourceAccessActionNode.expression())
                .property(Interaction.VALUE_LABEL, Expression.Factory.create(semanticModel,
                        resourceAccessActionNode, variableNode));

        appendNode();
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        this.variableNode = variableDeclarationNode.typedBindingPattern().bindingPattern();
        variableDeclarationNode.initializer().ifPresent(expressionNode -> expressionNode.accept(this));
        this.variableNode = null;
    }

    @Override
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        this.variableNode = assignmentStatementNode.varRef();
        super.visit(assignmentStatementNode);
        this.variableNode = null;
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        NameReferenceNode functionName = functionCallExpressionNode.functionName();

        String targetId = ParticipantManager.getInstance().getParticipantId(functionName);
        nodeBuilder = new Interaction.Builder(semanticModel)
                .interactionType(Interaction.InteractionType.FUNCTION_CALL)
                .targetId(targetId)
                .location(functionCallExpressionNode);

        nodeBuilder
                .property(Interaction.PARAMS_LABEL, getParamList(functionCallExpressionNode.arguments()))
                .property(Interaction.NAME_LABEL, Expression.Factory.createStringType(functionName));

        if (variableNode != null) {
            nodeBuilder.property(Interaction.VALUE_LABEL, Expression.Factory.create(semanticModel,
                    functionCallExpressionNode, variableNode));
        }

        appendNode();
    }

    private List<Expression> getParamList(SeparatedNodeList<FunctionArgumentNode> arguments) {
        return arguments.stream()
                .map(argument -> Expression.Factory.create(semanticModel, argument))
                .toList();
    }

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        handleReturnAction(returnStatementNode, returnStatementNode.expression().orElse(null));
    }

    @Override
    public void visit(ExpressionFunctionBodyNode expressionFunctionBodyNode) {
        ExpressionNode expression = expressionFunctionBodyNode.expression();
        handleReturnAction(expression, expression);
    }

    private void handleReturnAction(Node locationNode, ExpressionNode expression) {
        nodeBuilder
                .kind(SequenceNode.NodeKind.RETURN)
                .location(locationNode);
        if (expression != null && expression.kind() != SyntaxKind.NIL_LITERAL) {
            nodeBuilder.property(Interaction.VALUE_LABEL,
                    Expression.Factory.createType(semanticModel, expression, true));
        }
        appendNode();
    }

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
        nodeBuilder
                .kind(SequenceNode.NodeKind.IF)
                .location(ifElseStatementNode)
                .property(SequenceNode.CONDITION_LABEL, ifElseStatementNode.condition());

        List<SequenceNode> thenBlockNodes = new ArrayList<>();
        startBranch();
        for (StatementNode statement : ifElseStatementNode.ifBody().statements()) {
            statement.accept(this);
            addNode(thenBlockNodes, statement);
        }
        endBranch();
        nodeBuilder.branch(SequenceNode.IF_THEN_LABEL, thenBlockNodes);

        Optional<Node> elseBody = ifElseStatementNode.elseBody();
        if (elseBody.isPresent()) {
            startBranch();
            List<SequenceNode> elseNodes = analyzeElseBody(elseBody.get());
            endBranch();
            nodeBuilder.branch(SequenceNode.IF_ELSE_LABEL, elseNodes);
        }

        appendNode();
    }

    private List<SequenceNode> analyzeElseBody(Node elseBody) {
        return switch (elseBody.kind()) {
            case ELSE_BLOCK -> analyzeElseBody(((ElseBlockNode) elseBody).elseBody());
            case BLOCK_STATEMENT -> {
                List<SequenceNode> elseNodes = new ArrayList<>();
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
    public void visit(WhileStatementNode whileStatementNode) {
        nodeBuilder
                .location(whileStatementNode)
                .kind(SequenceNode.NodeKind.WHILE)
                .property(SequenceNode.CONDITION_LABEL, whileStatementNode.condition());

        List<SequenceNode> bodyBlockNodes = new ArrayList<>();
        startBranch();
        for (StatementNode statement : whileStatementNode.whileBody().statements()) {
            statement.accept(this);
            addNode(bodyBlockNodes, statement);
        }
        endBranch();
        nodeBuilder.branch(SequenceNode.BODY_LABEL, bodyBlockNodes);

        appendNode();
    }

    public List<SequenceNode> getSequenceNodes() {
        return sequenceNodes;
    }

    // Utility methods
    private void appendNode() {
        if (this.nodeBuilderStack.isEmpty()) {
            this.sequenceNodes.add(buildNode());
        }
    }

    private void startBranch() {
        this.nodeBuilderStack.push(nodeBuilder);
        nodeBuilder = new SequenceNode.Builder(semanticModel);
    }

    private void endBranch() {
        nodeBuilder = this.nodeBuilderStack.pop();
    }

    private SequenceNode buildNode() {
        SequenceNode sequenceNode = nodeBuilder.build();
        nodeBuilder = new SequenceNode.Builder(semanticModel);
        return sequenceNode;
    }

    private void addNode(List<SequenceNode> nodeList, Node node) {
        if (nodeBuilder.hasModified()) {
            nodeList.add(buildNode());
        }
    }
}
