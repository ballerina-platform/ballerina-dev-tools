package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.projects.Package;
import io.ballerina.sequencemodelgenerator.core.model.Interaction;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.ParticipantKind;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SubParticipantNodeVisitor extends NodeVisitor {
    private final String sourceId;
    private final SemanticModel semanticModel;
    private final Package currentPackage;
    private List<Participant> participants;
    private List<Interaction> interactions;

    private Set<NameReferenceNode> visitedFunctionNames;

    private String currentParticipant;

    public Set<NameReferenceNode> getVisitedFunctionNames() {
        return visitedFunctionNames;
    }

    public String getCurrentParticipant() {
        return currentParticipant;
    }

    public void setCurrentParticipant(String currentParticipant) {
        this.currentParticipant = currentParticipant;
    }

    public SubParticipantNodeVisitor(String sourceId, SemanticModel semanticModel, Package currentPackage, List<Participant> participants, List<Interaction> interactions, Set<NameReferenceNode> visitedFunctionNames) {
        this.sourceId = sourceId;
        this.semanticModel = semanticModel;
        this.currentPackage = currentPackage;
        this.participants = participants;
        this.interactions = interactions;
        this.visitedFunctionNames = visitedFunctionNames;
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        Optional<Symbol> typeSymbol = semanticModel.symbol(functionDefinitionNode);
        if (typeSymbol.isPresent()) {

            String packageName = typeSymbol.get().getModule().get().id().packageName();

            if (!isParticipantInList(functionDefinitionNode.functionName().text().trim(), packageName)) {
                String functionID = UUID.randomUUID().toString();
                Participant participant = new Participant(functionID,
                        functionDefinitionNode.functionName().text().trim(), ParticipantKind.WORKER, packageName);
                participants.add(participant);
                setCurrentParticipant(functionID);

                //trying to fix interaction order
                Interaction interaction = new Interaction(this.sourceId, participant.getId());
                interactions.add(interaction);

                ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, functionID, currentPackage, participants, interactions, visitedFunctionNames);
                functionDefinitionNode.accept(actionNodeVisitor);
            } else {
                Participant participant = getParticipantInList(functionDefinitionNode.functionName().text().trim(), packageName);
                setCurrentParticipant(participant.getId());
                Interaction interaction = new Interaction(this.sourceId, participant.getId());
                interactions.add(interaction);
            }

        }
    }

    private boolean isParticipantInList(String name, String pkgName) {
        for (Participant item : participants) {
            if (item.getName().trim().equals(name.trim()) &&
                    item.getPackageName().trim().equals(pkgName.trim())) {
                return true; // Return true when the conditions are met
            }
        }
        return false; // Return false if no match is found
    }

    private Participant getParticipantInList(String name, String pkgName) {
        for (Participant item : participants) {
            if (item.getName().trim().equals(name.trim()) &&
                    item.getPackageName().trim().equals(pkgName.trim())) {
                return item; // Return true when the conditions are met
            }
        }
        return null; // Return false if no match is found
    }
}
