package io.ballerina.sequencemodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ExpressionFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.sequencemodelgenerator.core.model.Expression;
import io.ballerina.sequencemodelgenerator.core.model.Interaction;
import io.ballerina.sequencemodelgenerator.core.model.SequenceNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ParticipantBodyAnalyzer extends NodeVisitor {

    private final List<SequenceNode> sequenceNodes;
    private final SemanticModel semanticModel;
    private final Stack<SequenceNode.Builder> nodeBuilderStack;

    private SequenceNode.Builder nodeBuilder;
    private TypedBindingPatternNode typedBindingPatternNode;

    public ParticipantBodyAnalyzer(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.sequenceNodes = new ArrayList<>();
        this.nodeBuilderStack = new Stack<>();

        this.nodeBuilder = new SequenceNode.Builder(semanticModel);
    }

    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        String targetId = ParticipantManager.getInstance().getParticipantId(remoteMethodCallActionNode.expression());

        SequenceNode.Builder interactionBuilder = new Interaction.Builder(semanticModel)
                .interactionType(Interaction.InteractionType.ENDPOINT_CALL)
                .targetId(targetId)
                .location(remoteMethodCallActionNode);

        List<Expression> paramList = getParamList(remoteMethodCallActionNode.arguments());
        interactionBuilder
                .property(Interaction.PARAMS_LABEL, paramList)
                .property(Interaction.NAME_LABEL,
                        Expression.Factory.createStringType(remoteMethodCallActionNode.methodName()))
                .property(Interaction.VALUE_LABEL, Expression.Factory.create(semanticModel,
                        remoteMethodCallActionNode, typedBindingPatternNode.bindingPattern()));

        appendNode(interactionBuilder);
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        this.typedBindingPatternNode = variableDeclarationNode.typedBindingPattern();
        variableDeclarationNode.initializer().ifPresent(expressionNode -> expressionNode.accept(this));
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        NameReferenceNode functionName = functionCallExpressionNode.functionName();

        String targetId = ParticipantManager.getInstance().getParticipantId(functionName);
        SequenceNode.Builder interactionBuilder = new Interaction.Builder(semanticModel)
                .interactionType(Interaction.InteractionType.FUNCTION_CALL)
                .targetId(targetId)
                .location(functionCallExpressionNode);

        List<Expression> paramList = getParamList(functionCallExpressionNode.arguments());

        interactionBuilder
                .property(Interaction.PARAMS_LABEL, paramList)
                .property(Interaction.NAME_LABEL, Expression.Factory.createStringType(functionName))
                .property(Interaction.VALUE_LABEL, Expression.Factory.create(semanticModel,
                        functionCallExpressionNode, typedBindingPatternNode.bindingPattern()));

        appendNode(interactionBuilder);
    }

    private List<Expression> getParamList(SeparatedNodeList<FunctionArgumentNode> arguments) {
        return arguments.stream()
                .map(argument -> Expression.Factory.create(semanticModel, argument))
                .toList();
    }

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        returnStatementNode.expression().ifPresent(this::handleReturnInteraction);
    }

    @Override
    public void visit(ExpressionFunctionBodyNode expressionFunctionBodyNode) {
        handleReturnInteraction(expressionFunctionBodyNode.expression());
    }

    // Handle methods
    private void handleReturnInteraction(ExpressionNode expressionNode) {
        SequenceNode.Builder builder = new SequenceNode.Builder(semanticModel)
                .kind(SequenceNode.NodeKind.RETURN)
                .location(expressionNode)
                .property(Interaction.VALUE_LABEL,
                        Expression.Factory.createType(semanticModel, expressionNode));

        appendNode(builder);
    }

    public List<SequenceNode> getSequenceNodes() {
        return sequenceNodes;
    }

    // Utility method
    private void appendNode(SequenceNode.Builder builder) {
        if (this.nodeBuilderStack.isEmpty()) {
            this.sequenceNodes.add(builder.build());
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
}
