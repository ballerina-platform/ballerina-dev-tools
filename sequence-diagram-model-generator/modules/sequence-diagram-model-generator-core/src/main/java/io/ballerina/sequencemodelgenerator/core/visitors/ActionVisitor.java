package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Package;
import io.ballerina.sequencemodelgenerator.core.model.*;
import io.ballerina.tools.diagnostics.Location;

import java.util.*;

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
                    SeparatedNodeList<Node> resourceAccessPath = clientResourceAccessActionNode.resourceAccessPath();
                    // iterate over the resourceAccessPath and create resourcePath by concatenating all the strings in SeparatedNodeList
                    String resourceName = "";

                    for (Node node : resourceAccessPath) {
                        if(node.kind().equals(SyntaxKind.IDENTIFIER_TOKEN)) {
                            if (!resourceName.isEmpty()) {
                                resourceName = resourceName.concat("/");
                            }
                            resourceName = resourceName.concat(((Token) node).text());
                        }
                    }

                    //  TODO Check for duplicate participants
                    Participant participant = new Participant(clientID, clientNode.toString(),
                            ParticipantKind.ENDPOINT, objectTypeSymbol.getModule().get().id().toString(), objectTypeSymbol.signature());
                    this.visitorContext.addToParticipants(participant);
                    String methodName = "get";
                    if (clientResourceAccessActionNode.methodName().isPresent()) {
                        methodName = clientResourceAccessActionNode.methodName().get().toString();
                    }

                    EndpointActionStatement actionStatement = new EndpointActionStatement(
                            this.visitorContext.getCurrentParticipant().getId(), clientID, clientNode.toString(),methodName, resourceName);
                    if (this.visitorContext.getDiagramElementWithChildren() != null) {
                        this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(actionStatement);
                    } else {
                        this.visitorContext.getCurrentParticipant().addChildDiagramElements(actionStatement);
                    }
                }
            }
        }
    }

    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        NameReferenceNode clientNode = null;

        try {
            if (remoteMethodCallActionNode.expression().kind().equals(SyntaxKind.FIELD_ACCESS)) {
                NameReferenceNode fieldName = ((FieldAccessExpressionNode)
                        remoteMethodCallActionNode.expression()).fieldName();
                if (fieldName.kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE)) {
                    clientNode = fieldName;
                }

            } else if (remoteMethodCallActionNode.expression().kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE)) {
                clientNode = (SimpleNameReferenceNode) remoteMethodCallActionNode.expression();
            } else if (remoteMethodCallActionNode.expression().kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
                clientNode = (QualifiedNameReferenceNode) remoteMethodCallActionNode.expression();
            }

            if (clientNode != null) {
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
                                    ParticipantKind.ENDPOINT, objectTypeSymbol.getModule().get().id().toString(), objectTypeSymbol.signature());
                            this.visitorContext.addToParticipants(participant);


                            SeparatedNodeList<FunctionArgumentNode> resourceAccessPath = remoteMethodCallActionNode.arguments();
                            // iterate over the resourceAccessPath and create resourcePath by concatenating all the strings in SeparatedNodeList
                            String resourceName = "";

                            for (Node node : resourceAccessPath) {
                                if(node.kind().equals(SyntaxKind.POSITIONAL_ARG)) {
                                    resourceName = resourceName.concat(node.toSourceCode());
                                }
                            }



                            EndpointActionStatement actionStatement = new EndpointActionStatement(
                                    this.visitorContext.getCurrentParticipant().getId(), clientID, clientNode.toString(),
                                    remoteMethodCallActionNode.methodName().toString(), removeDoubleQuotes(resourceName));
                            if (this.visitorContext.getDiagramElementWithChildren() != null) {
                                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(actionStatement);
                            } else {
                                this.visitorContext.getCurrentParticipant().addChildDiagramElements(actionStatement);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error occurred while visiting remote method call action node" + e.getMessage());
        }


    }

    private void findInteractions(NameReferenceNode nameNode, Symbol methodSymbol) {
        if (isNodeVisited(nameNode)) {
            Participant participant = getParticipantInList(nameNode.toString().trim(), this.currentPackage.packageName().toString());
            // todo make it support for multiple packages
            FunctionActionStatement functionActionStatement = new FunctionActionStatement(visitorContext.getCurrentParticipant().getId(),
                    participant.getId(), nameNode.toString().trim());
            if (this.visitorContext.getDiagramElementWithChildren() != null) {
                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(functionActionStatement);
            } else {
                visitorContext.getCurrentParticipant().addChildDiagramElements(functionActionStatement);
            }

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
                                    // TODO accept this
                                    node.accept(actionVisitor);
                                    visitorContext.addToVisitedFunctionNames(nameNode);

                                    // get targetParticipant
                                    // TODO get the module participant delails
                                    Participant targetParticipant = getParticipantInList(nameNode.toString().trim(), this.currentPackage.packageName().toString());

//                                    if (!actionVisitor.visitorContext.getCurrentParticipant().equals(visitorContext.getCurrentParticipant())) {
                                        FunctionActionStatement functionActionStatement = new FunctionActionStatement(visitorContext.getCurrentParticipant().getId(),
                                                targetParticipant.getId(), nameNode.toString().trim());

//                                        this.visitorContext.getCurrentParticipant().addChildDiagramElements(actionStatement);


                                        if (this.visitorContext.getDiagramElementWithChildren() != null) {
                                            this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(functionActionStatement);
//                                            this.visitorContext.getCurrentParticipant().addChildDiagramElements(this.visitorContext.getDiagramElementWithChildren());
                                        } else {
                                            visitorContext.getCurrentParticipant().addChildDiagramElements(functionActionStatement);
                                        }
                                   // }
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
//              setVisitorContext(visitorContext);
                ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
                functionDefinitionNode.functionBody().accept(actionVisitor);




//                participant.addChildDiagramElements(actionVisitor.visitorContext.getDiagramElementWithChildren());
            }

        }
    }

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
        IfStatement ifStatement = new IfStatement(ifElseStatementNode.condition().toString());
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), ifStatement, this.visitorContext.getVisitedFunctionNames());
//        setVisitorContext(visitorContext);
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        ifElseStatementNode.ifBody().accept(actionVisitor);

        if (ifElseStatementNode.elseBody().isPresent()) {
            ElseStatement elseStatement = new ElseStatement();
            VisitorContext visitorContext1 = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), elseStatement, this.visitorContext.getVisitedFunctionNames());
//            setVisitorContext(visitorContext1);
            ActionVisitor actionVisitor1 = new ActionVisitor(semanticModel, currentPackage, visitorContext1);
            // TODO try with the same actionVisitor
            ifElseStatementNode.elseBody().get().accept(actionVisitor1);
            ifStatement.setElseStatement(actionVisitor1.visitorContext.getDiagramElementWithChildren());
//            setVisitorContext(visitorContext);
        }


        if (this.visitorContext.getDiagramElementWithChildren() != null) {
            if (this.visitorContext.getDiagramElementWithChildren() instanceof ElseStatement) {
                ElseStatement elseStatement = (ElseStatement) this.visitorContext.getDiagramElementWithChildren();
                elseStatement.addChildDiagramElements(ifStatement);
            } else {
                if (ifStatement.getChildElements() != null || ifStatement.getElseStatement() != null) {
                    this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(ifStatement);
                }
            }
        } else {
            if (ifStatement.getChildElements() != null || ifStatement.getElseStatement() != null) {
                this.visitorContext.getCurrentParticipant().addChildDiagramElements(ifStatement);
            }
        }
//        this.visitorContext.getCurrentParticipant().addChildDiagramElements(actionVisitor.visitorContext.getDiagramElementWithChildren());

    }

//    @Override
//    public void visit(ElseBlockNode elseBlockNode) {
////        ElseStatement elseStatement = new ElseStatement();
////        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
////                this.visitorContext.getParticipants(), this.visitorContext.getDiagramElementWithChildren(), this.visitorContext.getVisitedFunctionNames());
////        setVisitorContext(visitorContext);
//        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
//        elseBlockNode.elseBody().accept(actionVisitor);
//
//    }

    @Override
    public void visit(WhileStatementNode whileStatementNode) {
        WhileStatement whileStatement = new WhileStatement(whileStatementNode.condition().toString());
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), whileStatement, this.visitorContext.getVisitedFunctionNames());
//        setVisitorContext(visitorContext);
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        whileStatementNode.whileBody().accept(actionVisitor);
        if (this.visitorContext.getDiagramElementWithChildren() != null && this.visitorContext.getDiagramElementWithChildren().getChildElements() != null) {
            this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(actionVisitor.visitorContext.getDiagramElementWithChildren());
        } else {
            this.visitorContext.getCurrentParticipant().addChildDiagramElements(whileStatement);
        }
    }

    @Override
    public void visit(ForEachStatementNode forEachStatementNode) {
        ForEachStatement forEachStatement = new ForEachStatement(forEachStatementNode.actionOrExpressionNode().toString());
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), forEachStatement, this.visitorContext.getVisitedFunctionNames());
//        setVisitorContext(visitorContext);
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        forEachStatementNode.blockStatement().accept(actionVisitor);
        if (this.visitorContext.getDiagramElementWithChildren() != null && this.visitorContext.getDiagramElementWithChildren().getChildElements() != null) {
            this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(actionVisitor.visitorContext.getDiagramElementWithChildren());
        } else {
            this.visitorContext.getCurrentParticipant().addChildDiagramElements(forEachStatement);
        }
//        this.visitorContext.getCurrentParticipant().addChildDiagramElements(actionVisitor.visitorContext.getDiagramElementWithChildren());
    }

    @Override
    public void visit(LockStatementNode lockStatementNode) {
        LockStatement lockStatement = new LockStatement();
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), lockStatement, this.visitorContext.getVisitedFunctionNames());
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        lockStatementNode.blockStatement().accept(actionVisitor);

        if (lockStatementNode.onFailClause().isPresent()) {
            OnFailStatement onFailStatement = new OnFailStatement(lockStatementNode.onFailClause().get().typeDescriptor().toString(),
                    lockStatementNode.onFailClause().get().failErrorName().toString());
            VisitorContext visitorContext1 = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), onFailStatement, this.visitorContext.getVisitedFunctionNames());
            ActionVisitor actionVisitor1 = new ActionVisitor(semanticModel, currentPackage, visitorContext1);
            lockStatementNode.onFailClause().get().blockStatement().accept(actionVisitor1);
            lockStatement.setOnFailStatement(onFailStatement);
        }

        if (lockStatement.getChildElements() != null) {
            this.visitorContext.getCurrentParticipant().addChildDiagramElements(lockStatement);
        }


    }

    @Override
    public void visit(DoStatementNode doStatementNode) {
        DoStatement doStatement = new DoStatement();
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), doStatement, this.visitorContext.getVisitedFunctionNames());
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        doStatementNode.blockStatement().accept(actionVisitor);

        if (doStatementNode.onFailClause().isPresent()) {
            OnFailStatement onFailStatement = new OnFailStatement(doStatementNode.onFailClause().get().typeDescriptor().toString(),
                    doStatementNode.onFailClause().get().failErrorName().toString());
            VisitorContext visitorContext1 = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), onFailStatement, this.visitorContext.getVisitedFunctionNames());
            ActionVisitor actionVisitor1 = new ActionVisitor(semanticModel, currentPackage, visitorContext1);
            doStatementNode.onFailClause().get().blockStatement().accept(actionVisitor1);
            doStatement.setOnFailStatement(onFailStatement);
        }

        if (doStatement.getChildElements() != null) {
            this.visitorContext.getCurrentParticipant().addChildDiagramElements(doStatement);
        }
    }


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

    private static String removeDoubleQuotes(String input) {
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }
}
