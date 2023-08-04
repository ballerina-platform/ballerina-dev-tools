package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.projects.Package;
import io.ballerina.sequencemodelgenerator.core.model.Interaction;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.ParticipantKind;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntryNodeVisitor extends NodeVisitor  {
    private List<Participant> participants = new LinkedList<>();
    private List<Interaction> interactions = new LinkedList<>();


    private final SemanticModel semanticModel;
    private final Package currentPackage;
    private String currentParticipant;

    public EntryNodeVisitor(SemanticModel semanticModel, Package currentPackage) {
        this.semanticModel = semanticModel;
        this.currentPackage = currentPackage;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public List<Interaction> getInteractions() {
        return interactions;
    }

    public void setCurrentParticipant(String currentParticipant) {
        this.currentParticipant = currentParticipant;
    }


// TODO:


    // TODO: 3. Identify the endpoint/ functions from different modules/files
    // check the packageName
    // Check shadow variables
    // support for remote functions


    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        SyntaxKind kind = functionDefinitionNode.kind();
        switch (kind) {
            case RESOURCE_ACCESSOR_DEFINITION: {
                StringBuilder identifierBuilder = new StringBuilder();
                StringBuilder resourcePathBuilder = new StringBuilder();
                NodeList<Node> relativeResourcePaths = functionDefinitionNode.relativeResourcePath();
                for (Node path : relativeResourcePaths) {
                    if (path instanceof ResourcePathParameterNode) {
                        ResourcePathParameterNode pathParam = (ResourcePathParameterNode) path;

                        identifierBuilder.append(String.format("[%s]",
                                pathParam.typeDescriptor().toSourceCode().trim()));
                    } else {
                        identifierBuilder.append(path);
                    }
                    resourcePathBuilder.append(path);
                }

                String resourcePath = resourcePathBuilder.toString().trim();
                String method = functionDefinitionNode.functionName().text().trim();
                Optional<Symbol> typeSymbol = semanticModel.symbol(functionDefinitionNode);
                if (typeSymbol.isPresent()) {
                    String packageName = typeSymbol.get().getModule().get().id().packageName();

                    String uuid = UUID.randomUUID().toString();
                    Participant participant = new Participant(uuid,
                            resourcePath, ParticipantKind.WORKER, packageName, null);
                    setCurrentParticipant(uuid);

                    participants.add(participant);


                    ActionNodeVisitor actionNodeVisitor =
                            new ActionNodeVisitor(semanticModel, uuid, currentPackage, participants, interactions);
                    WorkerMemberNodeVisitor workerMemberNodeVisitor = new WorkerMemberNodeVisitor(semanticModel, uuid, participants);

                    try {
                        functionDefinitionNode.accept(actionNodeVisitor);
                        functionDefinitionNode.accept(workerMemberNodeVisitor);
                    } catch (Exception e) {
                        System.out.printf("Error in visiting functionDefinitionNode: %s\n", e.getMessage());
                    }
                }
                break;
            }

            case FUNCTION_DEFINITION: {
                Optional<Symbol> typeSymbol = semanticModel.symbol(functionDefinitionNode);
                if (typeSymbol.isPresent()) {
                    String packageName = typeSymbol.get().getModule().get().id().packageName();

                    String uuid = UUID.randomUUID().toString();
                    Participant participant = new Participant(uuid,
                            functionDefinitionNode.functionName().toString(), ParticipantKind.WORKER, packageName, null);
                    setCurrentParticipant(uuid);

                    participants.add(participant);


                    ActionNodeVisitor actionNodeVisitor =
                            new ActionNodeVisitor(semanticModel, uuid, currentPackage, participants, interactions);
//                    WorkerMemberNodeVisitor workerMemberNodeVisitor = new WorkerMemberNodeVisitor(semanticModel, uuid, participants);

                    try {
                        functionDefinitionNode.accept(actionNodeVisitor);
//                        functionDefinitionNode.accept(workerMemberNodeVisitor);
                    } catch (Exception e) {
                        System.out.printf("Error in visiting functionDefinitionNode: %s\n", e.getMessage());
                    }

                }
                break;
            }
        }
    }
}
