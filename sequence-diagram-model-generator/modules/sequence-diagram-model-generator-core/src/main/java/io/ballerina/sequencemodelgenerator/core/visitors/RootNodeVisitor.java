package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ResourcePathParameterNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Package;
import io.ballerina.sequencemodelgenerator.core.exception.SequenceModelGenerationException;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.ParticipantKind;
import io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils;

import java.util.Optional;

import static io.ballerina.sequencemodelgenerator.core.Constants.*;

public class RootNodeVisitor extends NodeVisitor {
    private final SemanticModel semanticModel;
    private final Package currentPackage;
    private final VisitorContext visitorContext;

    private SequenceModelGenerationException modelGenerationException;

    public RootNodeVisitor(SemanticModel semanticModel, Package currentPackage, VisitorContext visitorContext) {
        this.semanticModel = semanticModel;
        this.currentPackage = currentPackage;
        this.visitorContext = visitorContext;
    }

    public VisitorContext getVisitorContext() {
        return visitorContext;
    }

    public SequenceModelGenerationException getModelGenerationException() {
        return modelGenerationException;
    }

    public void setModelGenerationException(SequenceModelGenerationException modelGenerationException) {
        this.modelGenerationException = modelGenerationException;
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        SyntaxKind kind = functionDefinitionNode.kind();
        try {
            switch (kind) {
                case RESOURCE_ACCESSOR_DEFINITION: {
                    // TODO : Check for parameters and send parameters separately without in the same string
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
                        String functionID = ModelGeneratorUtils.generateFunctionID(typeSymbol.get(), functionDefinitionNode);
                        if (functionID != null) {
                            Participant participant = new Participant(functionID,
                                    resourcePath, ParticipantKind.WORKER, packageName, null);
                            this.visitorContext.setCurrentParticipant(participant);
                            this.visitorContext.setRootParticipant(participant);
                            this.visitorContext.addToParticipants(participant);

                            ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, this.visitorContext);
                            functionDefinitionNode.functionBody().accept(actionVisitor);
                        }
                    } else {
                        throw new SequenceModelGenerationException(UNABLE_TO_FIND_SYMBOL);
                    }
                    break;
                }
                case FUNCTION_DEFINITION: {
                    Optional<Symbol> typeSymbol = semanticModel.symbol(functionDefinitionNode);
                    if (typeSymbol.isPresent() && typeSymbol.get().getModule().isPresent()) {
                        String packageName = typeSymbol.get().getModule().get().id().packageName();
                        String functionID = ModelGeneratorUtils.generateFunctionID(typeSymbol.get(), functionDefinitionNode);
                        if (functionID != null){
                            Participant participant = new Participant(functionID,
                                    functionDefinitionNode.functionName().toString(), ParticipantKind.WORKER, packageName, null);
                            this.visitorContext.setCurrentParticipant(participant);
                            this.visitorContext.setRootParticipant(participant);
                            this.visitorContext.addToParticipants(participant);

                            ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, this.visitorContext);
                            functionDefinitionNode.functionBody().accept(actionVisitor);
                        }
                    } else {
                        throw new SequenceModelGenerationException(UNABLE_TO_FIND_SYMBOL);
                    }
                    break;
                } case OBJECT_METHOD_DEFINITION: {
                    Optional<Symbol> typeSymbol = semanticModel.symbol(functionDefinitionNode);
                    if (typeSymbol.isPresent() && typeSymbol.get().getModule().isPresent()) {
                        String packageName = typeSymbol.get().getModule().get().id().packageName();

                        String functionID = ModelGeneratorUtils.generateFunctionID(typeSymbol.get(), functionDefinitionNode);
                        if (functionID != null) {
                            Participant participant = new Participant(functionID,
                                    functionDefinitionNode.functionName().toString(), ParticipantKind.WORKER, packageName, null);
                            this.visitorContext.setCurrentParticipant(participant);
                            this.visitorContext.setRootParticipant(participant);
                            this.visitorContext.addToParticipants(participant);

                            ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, this.visitorContext);
                            functionDefinitionNode.functionBody().accept(actionVisitor);
                        }
                    } else {
                        throw new SequenceModelGenerationException(UNABLE_TO_FIND_SYMBOL);
                    }
                }
            }
        } catch (Exception e) {
            this.setModelGenerationException(new SequenceModelGenerationException(ISSUE_IN_VISITING_ROOT_NODE + e.getMessage()));
        }
    }
}
