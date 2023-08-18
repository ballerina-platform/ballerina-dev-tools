package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Package;
import io.ballerina.sequencemodelgenerator.core.model.*;
import io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils;
import io.ballerina.tools.diagnostics.Location;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils.getQualifiedNameRefNodeFuncNameText;
import static io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils.getRawType;

public class ActionVisitor extends NodeVisitor {
    private final SemanticModel semanticModel;
    private final Package currentPackage;

    private VisitorContext visitorContext;

    private final String modulePrefix = null;

    public void setVisitorContext(VisitorContext visitorContext) {
        this.visitorContext = visitorContext;
    }

    public ActionVisitor(SemanticModel semanticModel, Package currentPackage, VisitorContext visitorContext) {
        this.semanticModel = semanticModel;
        this.currentPackage = currentPackage;
        this.visitorContext = visitorContext;
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        if ((functionCallExpressionNode.functionName() instanceof SimpleNameReferenceNode ||
                functionCallExpressionNode.functionName() instanceof QualifiedNameReferenceNode)) {

            // this is for the visitedFunction definitions based on functionCalls
//            visitorContext.addToVisitedFunctionNames(functionCallExpressionNode.functionName());
            Optional<Symbol> symbol = semanticModel.symbol(functionCallExpressionNode.functionName());
            symbol.ifPresent(value -> findInteractions(functionCallExpressionNode.functionName(), value));

        }
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
                    //  TODO Check for duplicate participants
                    Participant participant = new Participant(clientID, clientNode.toString(),
                            ParticipantKind.ENDPOINT, objectTypeSymbol.getModule().get().id().toString(), objectTypeSymbol.signature());
                    this.visitorContext.addToParticipants(participant);

                    ActionStatement actionStatement = new ActionStatement(this.visitorContext.getCurrentParticipant().getId(), clientID, clientNode.toString());
                    this.visitorContext.getCurrentParticipant().addChildDiagramElements(actionStatement);
                }
            }
        }
    }

    private void findInteractions(NameReferenceNode nameNode, Symbol methodSymbol) {
        if (isNodeVisited(nameNode)) {
            Participant participant = getParticipantInList(nameNode.toString(), this.currentPackage.packageName().toString());
            // todo make it support for multiple packages
            ActionStatement actionStatement = new ActionStatement(visitorContext.getCurrentParticipant().getId(),
                    participant.getId(), nameNode.toString());
            visitorContext.getCurrentParticipant().addChildDiagramElements(actionStatement);

        } else {


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

                                try {
                                    ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
                                    node.accept(actionVisitor);
                                    visitorContext.addToVisitedFunctionNames(nameNode);
                                    if (!actionVisitor.visitorContext.getCurrentParticipant().equals(visitorContext.getCurrentParticipant())) {
                                        ActionStatement actionStatement = new ActionStatement(visitorContext.getCurrentParticipant().getId(),
                                                actionVisitor.visitorContext.getCurrentParticipant().getId(), nameNode.toString());
                                        visitorContext.getCurrentParticipant().addChildDiagramElements(actionStatement);
                                    }
                                } catch (Exception e) {
                                    System.out.println("Error is visiting sub participant for currentStatement");
                                }

                            }
                        }
                    }
                });
            }
        }
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
                VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), participant,
                        this.visitorContext.getParticipants(), this.visitorContext.getVisitedFunctionNames());
                visitorContext.addToParticipants(participant);
                setVisitorContext(visitorContext);
                ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
                functionDefinitionNode.functionBody().accept(actionVisitor);
            }

        }
    }

//    @Override
//    public void visit(IfElseStatementNode ifElseStatementNode) {
//        IfStatement ifStatement = new IfStatement(ifElseStatementNode.condition().toString());
//        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), ifStatement,
//                this.visitorContext.getParticipants(), this.visitorContext.getVisitedFunctionNames());
//        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
//    }


    private boolean isNodeVisited(NameReferenceNode functionName) {
        return visitorContext.getVisitedFunctionNames().stream().anyMatch(nameNode -> {
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

    private boolean isParticipantInList(String name, String pkgName) {
        for (Participant item : visitorContext.getParticipants()) {
            if (item.getName().trim().equals(name.trim()) &&
                    item.getPackageName().trim().equals(pkgName.trim())) {
                return true; // Return true when the conditions are met
            }
        }
        return false; // Return false if no match is found
    }

    private Participant getParticipantInList(String name, String pkgName) {
        for (Participant item : visitorContext.getParticipants()) {
            if (item.getName().trim().equals(name.trim()) &&
                    item.getPackageName().trim().equals(pkgName.trim())) {
                return item; // Return true when the conditions are met
            }
        }
        return null; // Return false if no match is found
    }
}
