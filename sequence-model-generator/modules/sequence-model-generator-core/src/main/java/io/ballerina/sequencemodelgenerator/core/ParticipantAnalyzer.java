package io.ballerina.sequencemodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.SequenceNode;
import io.ballerina.tools.text.LineRange;

import java.util.List;
import java.util.Objects;

public class ParticipantAnalyzer extends NodeVisitor {

    private final SemanticModel semanticModel;
    private String name;
    private final String moduleName;
    private Participant.ParticipantKind kind;
    private LineRange location;
    private List<SequenceNode> sequenceNodes;

    public ParticipantAnalyzer(SemanticModel semanticModel, String moduleName) {
        this.semanticModel = semanticModel;
        this.moduleName = moduleName;
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        name = functionDefinitionNode.functionName().text();
        kind = Participant.ParticipantKind.FUNCTION;
        location = functionDefinitionNode.location().lineRange();

        ParticipantBodyAnalyzer participantBodyAnalyzer = new ParticipantBodyAnalyzer(semanticModel);
        functionDefinitionNode.functionBody().accept(participantBodyAnalyzer);
        sequenceNodes = participantBodyAnalyzer.getSequenceNodes();
    }

    @Override
    public void visit(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        kind = Participant.ParticipantKind.ENDPOINT;
        location = moduleVariableDeclarationNode.location().lineRange();
    }

    @Override
    public void visit(CaptureBindingPatternNode captureBindingPatternNode) {
        name = captureBindingPatternNode.variableName().text();
        captureBindingPatternNode.parent().parent().accept(this);
    }

    public Participant getParticipant() {
        String id = String.valueOf(Objects.hash(location));
        return new Participant(id, name, kind, moduleName, sequenceNodes, location);
    }
}
