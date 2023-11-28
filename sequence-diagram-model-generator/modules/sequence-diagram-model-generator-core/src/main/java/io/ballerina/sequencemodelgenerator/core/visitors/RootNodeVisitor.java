/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Package;
import io.ballerina.sequencemodelgenerator.core.exception.SequenceModelGenerationException;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.ParticipantKind;
import io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils;

import java.util.Optional;

import static io.ballerina.sequencemodelgenerator.core.Constants.ISSUE_IN_VISITING_ROOT_NODE;
import static io.ballerina.sequencemodelgenerator.core.Constants.UNABLE_TO_FIND_SYMBOL;

/**
 * Visitor which captures the root node/entry point of the sequence diagram.
 *
 * @since 2201.8.0
 */
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
        return this.visitorContext;
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
                    StringBuilder resourcePathBuilder = new StringBuilder();
                    NodeList<Node> relativeResourcePaths = functionDefinitionNode.relativeResourcePath();
                    for (Node path : relativeResourcePaths) {
                        resourcePathBuilder.append(path);
                    }

                    String resourcePath = resourcePathBuilder.toString().trim();

                    Optional<Symbol> typeSymbol = semanticModel.symbol(functionDefinitionNode);
                    if (typeSymbol.isPresent() && typeSymbol.get().getModule().isPresent()) {
                        String packageName = typeSymbol.get().getModule().get().id().packageName();
                        String functionID = ModelGeneratorUtils.generateResourceID(typeSymbol.get(),
                                functionDefinitionNode);
                        if (functionID != null) {
                            Participant participant = new Participant(functionID,
                                    resourcePath, ParticipantKind.WORKER, packageName,
                                    functionDefinitionNode.lineRange());
                            this.visitorContext.setCurrentParticipant(participant);
                            this.visitorContext.setRootParticipant(participant);
                            this.visitorContext.addToParticipants(participant);

                            ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage,
                                    this.visitorContext);
                            functionDefinitionNode.functionBody().accept(actionVisitor);
                            if (participant.getElementBody() != null) {
                                participant.setHasInteractions(true);
                            }
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
                        String functionID = ModelGeneratorUtils.generateFunctionID(typeSymbol.get(),
                                functionDefinitionNode);
                        if (functionID != null) {
                            Participant participant = new Participant(functionID,
                                    functionDefinitionNode.functionName().toString(), ParticipantKind.WORKER,
                                    packageName, functionDefinitionNode.lineRange());
                            this.visitorContext.setCurrentParticipant(participant);
                            this.visitorContext.setRootParticipant(participant);
                            this.visitorContext.addToParticipants(participant);

                            ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage,
                                    this.visitorContext);
                            functionDefinitionNode.functionBody().accept(actionVisitor);
                            if (participant.getElementBody() != null) {
                                participant.setHasInteractions(true);
                            }
                        }
                    } else {
                        throw new SequenceModelGenerationException(UNABLE_TO_FIND_SYMBOL);
                    }
                    break;
                }
                case OBJECT_METHOD_DEFINITION: {
                    Optional<Symbol> typeSymbol = semanticModel.symbol(functionDefinitionNode);
                    if (typeSymbol.isPresent() && typeSymbol.get().getModule().isPresent()) {
                        String packageName = typeSymbol.get().getModule().get().id().packageName();

                        String functionID = ModelGeneratorUtils.generateFunctionID(typeSymbol.get(),
                                functionDefinitionNode);
                        if (functionID != null) {
                            Participant participant = new Participant(functionID,
                                    functionDefinitionNode.functionName().toString(), ParticipantKind.WORKER,
                                    packageName, functionDefinitionNode.lineRange());
                            this.visitorContext.setCurrentParticipant(participant);
                            this.visitorContext.setRootParticipant(participant);
                            this.visitorContext.addToParticipants(participant);

                            ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage,
                                    this.visitorContext);
                            functionDefinitionNode.functionBody().accept(actionVisitor);
                            if (participant.getElementBody() != null) {
                                participant.setHasInteractions(true);
                            }
                        }
                    } else {
                        throw new SequenceModelGenerationException(UNABLE_TO_FIND_SYMBOL);
                    }
                }
            }
        } catch (SequenceModelGenerationException e) {
            this.setModelGenerationException(
                    new SequenceModelGenerationException(ISSUE_IN_VISITING_ROOT_NODE + e.getMessage()));
            
        }
    }
}
