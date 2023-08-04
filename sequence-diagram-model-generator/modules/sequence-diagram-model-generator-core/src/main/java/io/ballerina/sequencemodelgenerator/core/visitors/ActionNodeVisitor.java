package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Package;
import io.ballerina.sequencemodelgenerator.core.model.Interaction;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.ParticipantKind;
import io.ballerina.tools.diagnostics.Location;
import static io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils.getRawType;
import static io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils.getQualifiedNameRefNodeFuncNameText;

import java.util.*;

public class ActionNodeVisitor extends NodeVisitor {
    private final SemanticModel semanticModel;
    private final String workerId;
    private final Package currentPackage;
    private List<Participant> participants;
    private List<Interaction> interactions;
    private Set<NameReferenceNode> visitedFunctionNames = new HashSet<>();
    private final String modulePrefix =null;

    public ActionNodeVisitor(SemanticModel semanticModel, String workerId, Package currentPackage, List<Participant> participants, List<Interaction> interactions) {
        this.semanticModel = semanticModel;
        this.workerId = workerId;
        this.currentPackage = currentPackage;
        this.participants = participants;
        this.interactions = interactions;

    }

    public Set<NameReferenceNode> getVisitedFunctionNames() {
        return visitedFunctionNames;
    }

    public void setVisitedFunctionNames(Set<NameReferenceNode> visitedFunctionNames) {
        this.visitedFunctionNames = visitedFunctionNames;
    }




    public ActionNodeVisitor(SemanticModel semanticModel, String workerId, Package currentPackage, List<Participant> participants, List<Interaction> interactions, Set<NameReferenceNode> visitedFunctionNames) {

        this.semanticModel = semanticModel;
        this.workerId = workerId;
        this.currentPackage = currentPackage;
        this.participants = participants;
        this.interactions = interactions;


        this.visitedFunctionNames.addAll(visitedFunctionNames);
    }

    public String getWorkerId() {
        return workerId;
    }

    @Override
    public void visit(ClientResourceAccessActionNode clientResourceAccessActionNode) {
        NameReferenceNode clientNode = null;

        String resourceMethod = null;
        String resourcePath = null;
        String serviceId = null;
        String serviceLabel = null;

        try {
            if (clientResourceAccessActionNode.expression().kind().equals(SyntaxKind.FIELD_ACCESS)) {
                NameReferenceNode fieldName = ((FieldAccessExpressionNode)
                        clientResourceAccessActionNode.expression()).fieldName();
                if (fieldName.kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE)) {
                    clientNode = fieldName;
                }
            } else if (clientResourceAccessActionNode.expression().kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE)) {
                clientNode = (SimpleNameReferenceNode) clientResourceAccessActionNode.expression();
            } else if (clientResourceAccessActionNode.expression().kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
                clientNode = (QualifiedNameReferenceNode) clientResourceAccessActionNode.expression();
            }


        } catch (Exception e) {
            // Diagnostic message is logged in the visit() method
        }

        Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(clientNode);

        if (typeSymbol.isPresent()) {
            TypeSymbol rawType = getRawType(typeSymbol.get());
            if (rawType.typeKind() == TypeDescKind.OBJECT) {
                ObjectTypeSymbol objectTypeSymbol = (ObjectTypeSymbol) rawType;
                boolean isEndpoint = objectTypeSymbol.qualifiers()
                        .contains(Qualifier.CLIENT);
                if (isEndpoint) {
                    String clientID = UUID.randomUUID().toString();
                    Participant participant = new Participant(clientID, clientNode.toString(),
                            ParticipantKind.ENDPOINT,objectTypeSymbol.getModule().get().id().toString(),objectTypeSymbol.signature());
                    participants.add(participant);
                    Interaction interaction = new Interaction(this.workerId, clientID);
                    interactions.add(interaction);
                }
            }
        }
    }


    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        if ((functionCallExpressionNode.functionName() instanceof SimpleNameReferenceNode ||
                functionCallExpressionNode.functionName() instanceof QualifiedNameReferenceNode) &&
                !isNodeVisited(functionCallExpressionNode.functionName())) {

            // this is for the visitedFunction definitions based on functionCalls
            visitedFunctionNames.add(functionCallExpressionNode.functionName());
            Optional<Symbol> symbol = semanticModel.symbol(functionCallExpressionNode.functionName());
            symbol.ifPresent(value -> findInteractions(functionCallExpressionNode.functionName(), value));

        }
        // todo : Other combinations
    }


    private void findInteractions(NameReferenceNode nameNode, Symbol methodSymbol) {

        Optional<Location> location = methodSymbol.getLocation();
        Optional<ModuleSymbol> optionalModuleSymbol = methodSymbol.getModule();
        if (optionalModuleSymbol.isPresent()) {
            ModuleID moduleID = optionalModuleSymbol.get().id();
            // get ST of the selected methodSymbol
            currentPackage.modules().forEach(module -> {
                if (Objects.equals(moduleID.moduleName(), module.moduleName().toString())) {
                    Collection<DocumentId> documentIds = module.documentIds();
                    for (DocumentId documentId : documentIds) {
                        if (module.document(documentId).syntaxTree().filePath().equals(location.get().lineRange().fileName())) {
                            SyntaxTree syntaxTree = module.document(documentId).syntaxTree();
                            NonTerminalNode node = ((ModulePartNode) syntaxTree.rootNode())
                                    .findNode(location.get().textRange());

                            SubParticipantNodeVisitor subParticipantNodeVisitor = new SubParticipantNodeVisitor(workerId, semanticModel, currentPackage, participants, interactions, visitedFunctionNames);
                            node.accept(subParticipantNodeVisitor);
                            visitedFunctionNames.addAll(subParticipantNodeVisitor.getVisitedFunctionNames());


//                                if (!getWorkerId().equals(subParticipantNodeVisitor.getCurrentParticipant())) {
//                                    Interaction interaction = new Interaction(getWorkerId(), subParticipantNodeVisitor.getCurrentParticipant());
//                                    interactions.add(interaction);
//                                }
                        }
                    }
                }
            });
        }
    }









    private boolean isNodeVisited(NameReferenceNode functionName) {
        return visitedFunctionNames.stream().anyMatch(nameNode -> {
            if (functionName instanceof SimpleNameReferenceNode) {
                if ((nameNode instanceof QualifiedNameReferenceNode && modulePrefix != null)) {
                    return getQualifiedNameRefNodeFuncNameText((QualifiedNameReferenceNode) nameNode)
                            .equals(modulePrefix + ":" + ((SimpleNameReferenceNode) functionName).name().text());
                } else if (nameNode instanceof SimpleNameReferenceNode) {
                    return ((SimpleNameReferenceNode) nameNode).name().text()
                            .equals(((SimpleNameReferenceNode) functionName).name().text());
                }
            } else if (functionName instanceof QualifiedNameReferenceNode) {
                return getQualifiedNameRefNodeFuncNameText((QualifiedNameReferenceNode) nameNode)
                        .equals(getQualifiedNameRefNodeFuncNameText((QualifiedNameReferenceNode) functionName));
            }
            return false;
        });
    }






}
