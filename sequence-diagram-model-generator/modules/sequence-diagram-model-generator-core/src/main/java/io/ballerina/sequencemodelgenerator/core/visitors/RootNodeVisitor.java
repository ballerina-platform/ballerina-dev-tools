package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.projects.Package;
import io.ballerina.sequencemodelgenerator.core.exception.SequenceModelGenerationException;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.ParticipantKind;

import java.util.Optional;
import java.util.UUID;

public class RootNodeVisitor extends NodeVisitor {
    private final SemanticModel semanticModel;
    private final Package currentPackage;
    private final VisitorContext visitorContext;

    public RootNodeVisitor(SemanticModel semanticModel, Package currentPackage, VisitorContext visitorContext) {
        this.semanticModel = semanticModel;
        this.currentPackage = currentPackage;
        this.visitorContext = visitorContext;
    }

    public VisitorContext getVisitorContext() {
        return visitorContext;
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        SyntaxKind kind = functionDefinitionNode.kind();
        try {

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

                    // Name of the resource
                    String resourcePath = resourcePathBuilder.toString().trim();
                    Optional<Symbol> typeSymbol = semanticModel.symbol(functionDefinitionNode);
                    if (typeSymbol.isPresent() && typeSymbol.get().getModule().isPresent()) {
                        String packageName = typeSymbol.get().getModule().get().id().packageName();

//                        String uuid = UUID.randomUUID().toString(); // TODO: Generate FQN
                        String moduleID = typeSymbol.get().getModule().get().id().toString().replace(":", "_");
                        String functionID = moduleID + "_" + functionDefinitionNode.functionName().text().trim();
                        Participant participant = new Participant(functionID,
                                resourcePath, ParticipantKind.WORKER, packageName, null);
                        this.visitorContext.setCurrentParticipant(participant);
                        this.visitorContext.setRootParticipant(participant);
                        this.visitorContext.addToParticipants(participant);

                        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, this.visitorContext);
                        // Provide support module level endpoints

                        try {
                            functionDefinitionNode.functionBody().accept(actionVisitor);
                        } catch (Exception e) {
                            throw new SequenceModelGenerationException("Error in visiting functionDefinitionNode: " + e.getMessage());
                        }
                    } // TODO: else throw exception
                    break;
                }
                case FUNCTION_DEFINITION: {
                    Optional<Symbol> typeSymbol = semanticModel.symbol(functionDefinitionNode);
                    if (typeSymbol.isPresent() && typeSymbol.get().getModule().isPresent()) {
                        String packageName = typeSymbol.get().getModule().get().id().packageName();

//                        String uuid = UUID.randomUUID().toString(); // ToDO: Generate FQN
                        String moduleID = typeSymbol.get().getModule().get().id().toString().replace(":", "_");
                        String functionID = moduleID + "_" + functionDefinitionNode.functionName().text().trim();
                        Participant participant = new Participant(functionID,
                                functionDefinitionNode.functionName().toString(), ParticipantKind.WORKER, packageName, null);
                        this.visitorContext.setCurrentParticipant(participant);
                        this.visitorContext.setRootParticipant(participant);
                        this.visitorContext.addToParticipants(participant);

                        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, this.visitorContext);
                        // add worked node memeber

                        try {
                            functionDefinitionNode.functionBody().accept(actionVisitor);
                        } catch (Exception e) {
                            throw new SequenceModelGenerationException("Error in visiting functionDefinitionNode: " + e.getMessage());
                        }
                    } // TODO: else throw exception
                    break;
                } case OBJECT_METHOD_DEFINITION: {
                    Optional<Symbol> typeSymbol = semanticModel.symbol(functionDefinitionNode);
                    if (typeSymbol.isPresent() && typeSymbol.get().getModule().isPresent()) {
                        String packageName = typeSymbol.get().getModule().get().id().packageName();
                        String moduleID = typeSymbol.get().getModule().get().id().toString().replace(":", "_");
                        String functionID = moduleID + "_" + functionDefinitionNode.functionName().text().trim();
                        Participant participant = new Participant(functionID,
                                functionDefinitionNode.functionName().toString(), ParticipantKind.WORKER, packageName, null);
                        this.visitorContext.setCurrentParticipant(participant);
                        this.visitorContext.setRootParticipant(participant);
                        this.visitorContext.addToParticipants(participant);

                        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, this.visitorContext);
                        // add worked node memeber

                        try {
                            functionDefinitionNode.functionBody().accept(actionVisitor);
                        } catch (Exception e) {
                            throw new SequenceModelGenerationException("Error in visiting functionDefinitionNode: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // System.out.printf("Error in visiting functionDefinitionNode: %s\n", e.getMessage());
            // TODO : ADD exception to visitor context
        }
    }
}
