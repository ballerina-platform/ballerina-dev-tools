package io.ballerina.sequencemodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.SequenceNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ParticipantAnalyzer extends NodeVisitor {

    private final List<SequenceNode> sequenceNodes;
    private final SemanticModel semanticModel;
    private final Stack<SequenceNode.Builder> nodeBuilderStack;

    // State variables
    private SequenceNode.Builder nodeBuilder;
    private TypedBindingPatternNode typedBindingPatternNode;

    // Output variables
    private Participant participant;
    private List<Participant> dependentParticipants;

    public ParticipantAnalyzer(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.sequenceNodes = new ArrayList<>();
        this.nodeBuilderStack = new Stack<>();
        this.nodeBuilder = new SequenceNode.Builder();
        this.dependentParticipants = new ArrayList<>();
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        Participant.Builder participantBuilder = new Participant.Builder()
                .name(functionDefinitionNode.functionName().text())
                .kind(Participant.ParticipantKind.FUNCTION)
                .location(functionDefinitionNode);

        functionDefinitionNode.functionBody().accept(this);
        participantBuilder.nodes(sequenceNodes);

        participant = participantBuilder.build();
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        this.typedBindingPatternNode = variableDeclarationNode.typedBindingPattern();
        variableDeclarationNode.initializer().ifPresent(expressionNode -> expressionNode.accept(this));
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
    }

    // Utility method
    public List<Participant> getDependentParticipants() {
        return dependentParticipants;
    }

    public Participant getParticipant() {
        return participant;
    }

    private void appendNode() {
        if (this.nodeBuilderStack.isEmpty()) {
            this.sequenceNodes.add(buildNode());
        }
    }

    private void startBranch() {
        this.nodeBuilderStack.push(nodeBuilder);
        nodeBuilder = new SequenceNode.Builder();
    }

    private void endBranch() {
        nodeBuilder = this.nodeBuilderStack.pop();
    }

    private SequenceNode buildNode() {
        SequenceNode sequenceNode = nodeBuilder.build();
        nodeBuilder = new SequenceNode.Builder();
        return sequenceNode;
    }
}
