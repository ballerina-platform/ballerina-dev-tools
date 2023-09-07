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

    private boolean isHiddenVariableStmt = false;

    public void setHiddenVariableStmt(boolean hiddenVariableStmt) {
        isHiddenVariableStmt = hiddenVariableStmt;
    }

    public boolean isHiddenVariableStmt() {
        return isHiddenVariableStmt;
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

            Optional<Symbol> symbol = semanticModel.symbol(functionCallExpressionNode.functionName());
            boolean isHidden = this.isHiddenVariableStmt();
//            if (isCommentPresentInParentNode(functionCallExpressionNode.parent())) {
//                if (functionCallExpressionNode.parent().kind().equals(SyntaxKind.CALL_STATEMENT)) {
//                    isHidden = isHiddenInSequenceFlagPresent(functionCallExpressionNode.parent());
//                } else {
//                    isHidden = false;
//                }
//            } else {
//                isHidden = this.isHiddenVariableStmt();
//            }


//            ParentStatementFindingVisitor parentStatementFindingVisitor = new ParentStatementFindingVisitor();
//            parentStatementFindingVisitor.analyze(functionCallExpressionNode);
//            Node parentStatement = parentStatementFindingVisitor.getParentStatement();
//            if (functionCallExpressionNode.parent().kind().equals(SyntaxKind.CALL_STATEMENT)) {
//                isHidden = isHiddenInSequenceFlagPresent(functionCallExpressionNode.parent());
//            } else {
//                isHidden = this.isHiddenVariableStmt();
//            }
//

            symbol.ifPresent(value -> findInteractions(functionCallExpressionNode.functionName(), value, isHidden));
        }
    }


    @Override
    public void visit(ExpressionStatementNode expressionStatementNode) {
        if (isStatementBlockCommentPresent(expressionStatementNode)) {
            generateStatementBlock(expressionStatementNode);
        }
        if (!expressionStatementNode.expression().isMissing()) {
            boolean isHidden = isHiddenInSequenceFlagPresent(expressionStatementNode);
            this.setHiddenVariableStmt(isHidden);
            expressionStatementNode.expression().accept(this);
        }
    }



//    @Override
//    public void visit(CheckExpressionNode checkExpressionNode) {
//        if (isStatementBlockCommentPresent(checkExpressionNode)) {
//            generateStatementBlock(checkExpressionNode);
//        }
//        if (!checkExpressionNode.expression().isMissing()) {
//            boolean isHidden = isHiddenInSequenceFlagPresent(checkExpressionNode);
//            this.setHiddenVariableStmt(isHidden);
//            checkExpressionNode.expression().accept(this);
//        }
//    }


    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        if (isStatementBlockCommentPresent(variableDeclarationNode)) {
            generateStatementBlock(variableDeclarationNode);
        }
        if (variableDeclarationNode.initializer().isPresent()) {
            boolean isHidden = isHiddenInSequenceFlagPresent(variableDeclarationNode);
            this.setHiddenVariableStmt(isHidden);
            variableDeclarationNode.initializer().get().accept(this);
        }
    }

    @Override
    public void visit(CompoundAssignmentStatementNode compoundAssignmentStatementNode) {
        if (isStatementBlockCommentPresent(compoundAssignmentStatementNode)) {
            generateStatementBlock(compoundAssignmentStatementNode);
        }
        if (!compoundAssignmentStatementNode.rhsExpression().isMissing()) {
            boolean isHidden = isHiddenInSequenceFlagPresent(compoundAssignmentStatementNode);
            this.setHiddenVariableStmt(isHidden);
            compoundAssignmentStatementNode.rhsExpression().accept(this);
        }
    }

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        if (isStatementBlockCommentPresent(returnStatementNode)) {
            generateStatementBlock(returnStatementNode);
        }
        if (returnStatementNode.expression().isPresent()) {
            boolean isHidden = isHiddenInSequenceFlagPresent(returnStatementNode);
            this.setHiddenVariableStmt(isHidden);
            returnStatementNode.expression().get().accept(this);
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
                if (isEndpoint && clientNode != null && objectTypeSymbol.getModule().isPresent()) {
                    // generate endpoint participant FQN
                    String clientPkgName = objectTypeSymbol.getModule().get().id().toString().trim().replace(":", "_");
                    String clientID = clientPkgName + "_" + objectTypeSymbol.signature().trim() + "_" + clientNode.toString().trim();
                    SeparatedNodeList<Node> resourceAccessPath = clientResourceAccessActionNode.resourceAccessPath();
                    // iterate over the resourceAccessPath and create resourcePath by concatenating all the strings in SeparatedNodeList
                    String resourceName = "";

                    for (Node node : resourceAccessPath) {
                        if (node.kind().equals(SyntaxKind.IDENTIFIER_TOKEN)) {
                            if (!resourceName.isEmpty()) {
                                resourceName = resourceName.concat("/");
                            }
                            resourceName = resourceName.concat(((Token) node).text()).trim();
                        }
                    }
                    // TODO: rename the clientiDI as endpointID
                    if (!isEndpointPresent(clientID)) {
                        Participant participant = new Participant(clientID, clientNode.toString().trim(),
                                ParticipantKind.ENDPOINT, clientPkgName, objectTypeSymbol.signature().trim());
                        this.visitorContext.addToParticipants(participant);
                    }

                    String methodName = "get";
                    if (clientResourceAccessActionNode.methodName().isPresent()) {
                        methodName = clientResourceAccessActionNode.methodName().get().toString().trim();
                    }

//                    boolean isHidden = isHiddenInSequenceFlagPresent(clientResourceAccessActionNode);
                    boolean isHidden = this.isHiddenVariableStmt();
                    EndpointActionStatement actionStatement = new EndpointActionStatement(
                            this.visitorContext.getCurrentParticipant().getId().trim(), clientID, clientNode.toString().trim(),
                            methodName, resourceName, isHidden);
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
                        if (isEndpoint && objectTypeSymbol.getModule().isPresent()) {
                            String clientPkgName = objectTypeSymbol.getModule().get().id().toString().trim().replace(":", "_");
                            String clientID = clientPkgName + "_" + objectTypeSymbol.signature().trim() + "_" + clientNode.toString().trim();

                            if (!isEndpointPresent(clientID)) {
                                Participant participant = new Participant(clientID, clientNode.toString().trim(),
                                        ParticipantKind.ENDPOINT, clientPkgName, objectTypeSymbol.signature().trim());
                                this.visitorContext.addToParticipants(participant);
                            }

                            SeparatedNodeList<FunctionArgumentNode> resourceAccessPath = remoteMethodCallActionNode.arguments();
                            // iterate over the resourceAccessPath and create resourcePath by concatenating all the strings in SeparatedNodeList
                            String resourceName = "";
                            for (Node node : resourceAccessPath) {
                                if (node.kind().equals(SyntaxKind.POSITIONAL_ARG)) {
                                    resourceName = resourceName.concat(node.toSourceCode());
                                }
                            }

                            // boolean isHidden = isHiddenInSequenceFlagPresent(remoteMethodCallActionNode);
                            boolean isHidden = this.isHiddenVariableStmt();
                            EndpointActionStatement actionStatement = new EndpointActionStatement(
                                    this.visitorContext.getCurrentParticipant().getId().trim(), clientID, clientNode.toString().trim(),
                                    remoteMethodCallActionNode.methodName().toString().trim(), removeDoubleQuotes(resourceName), isHidden);
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
            // System.out.println("Error occurred while visiting remote method call action node" + e.getMessage());
        }


    }

    private void findInteractions(NameReferenceNode nameNode, Symbol methodSymbol, boolean isHidden) {
        if (isNodeVisited(nameNode)) {
            Participant participant = getParticipantInList(nameNode.toString().trim(), this.currentPackage.packageName().toString().trim());
            if (participant != null) {
                FunctionActionStatement functionActionStatement = new FunctionActionStatement(visitorContext.getCurrentParticipant().getId().trim(),
                        participant.getId().trim(), nameNode.toString().trim(), isHidden);
                if (this.visitorContext.getDiagramElementWithChildren() != null) {
                    this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(functionActionStatement);
                } else {
                    visitorContext.getCurrentParticipant().addChildDiagramElements(functionActionStatement);
                }
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
                                    SemanticModel nextSemanticModel = currentPackage.getCompilation().getSemanticModel(module.moduleId());
                                    ActionVisitor actionVisitor = new ActionVisitor(nextSemanticModel, currentPackage, visitorContext);
                                    node.accept(actionVisitor);
                                    visitorContext.addToVisitedFunctionNames(nameNode);

                                    Participant targetParticipant = null;
                                    String functionName = null;
                                    if (nameNode.kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
                                        targetParticipant = getParticipantInList(((QualifiedNameReferenceNode) nameNode).identifier().toString().trim(), this.currentPackage.packageName().toString());
                                        functionName = ((QualifiedNameReferenceNode) nameNode).identifier().toString().trim();
                                    } else {
                                        targetParticipant = getParticipantInList(((SimpleNameReferenceNode) nameNode).name().text().trim(), this.currentPackage.packageName().toString());
                                        functionName = ((SimpleNameReferenceNode) nameNode).name().text().trim();
                                    }

                                    if (targetParticipant != null) {


                                        FunctionActionStatement functionActionStatement = new FunctionActionStatement(visitorContext.getCurrentParticipant().getId().trim(),
                                                targetParticipant.getId().trim(), functionName, isHidden);

                                        if (this.visitorContext.getDiagramElementWithChildren() != null) {
                                            this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(functionActionStatement);
                                        } else {
                                            visitorContext.getCurrentParticipant().addChildDiagramElements(functionActionStatement);
                                        }
                                    }
                                } catch (Exception e) {
                                    // System.out.println("Error is visiting sub participant for currentStatement");
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
            String packageName = typeSymbol.get().getModule().get().id().packageName().trim();
            if (!isParticipantInList(functionDefinitionNode.functionName().text().trim(), packageName)) {
//                String functionID = UUID.randomUUID().toString(); // TODO: move to FQN
                String moduleID = typeSymbol.get().getModule().get().id().toString().trim().replace(":", "_");
                String functionID = moduleID + "_" + functionDefinitionNode.functionName().text().trim();
                Participant participant = new Participant(functionID,
                        functionDefinitionNode.functionName().text().trim(), ParticipantKind.WORKER, packageName);
                VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), participant,
                        this.visitorContext.getParticipants(), this.visitorContext.getVisitedFunctionNames());
                visitorContext.addToParticipants(participant);
                ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
                functionDefinitionNode.functionBody().accept(actionVisitor);
            }
        }
    }

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
        if (isStatementBlockCommentPresent(ifElseStatementNode)) {
            generateStatementBlock(ifElseStatementNode);
        }
        boolean isHidden = isHiddenInSequenceFlagPresent(ifElseStatementNode);
        IfStatement ifStatement = new IfStatement(ifElseStatementNode.condition().toString(), isHidden);

        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), ifStatement, this.visitorContext.getVisitedFunctionNames());
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        ifElseStatementNode.ifBody().accept(actionVisitor);

        if (ifElseStatementNode.elseBody().isPresent()) {
            ElseStatement elseStatement = new ElseStatement(isHidden);
            VisitorContext visitorContext1 = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), elseStatement, this.visitorContext.getVisitedFunctionNames());
            ActionVisitor actionVisitor1 = new ActionVisitor(semanticModel, currentPackage, visitorContext1);

            ifElseStatementNode.elseBody().get().accept(actionVisitor1);
            ifStatement.setElseStatement(actionVisitor1.visitorContext.getDiagramElementWithChildren());
        }

        if (this.visitorContext.getDiagramElementWithChildren() != null) {
            if (this.visitorContext.getDiagramElementWithChildren() instanceof ElseStatement) {
                ElseStatement elseStatement = (ElseStatement) this.visitorContext.getDiagramElementWithChildren();
                elseStatement.addChildDiagramElements(ifStatement);
            } else {
                // Adding ifStatement if the body of statement has interactions
                if (ifStatement.getChildElements() != null || (ifStatement.getElseStatement() != null && ifStatement.getElseStatement().getChildElements() != null)) {
                    this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(ifStatement);
                }
            }
        } else {
            if (ifStatement.getChildElements() != null || (ifStatement.getElseStatement() != null && ifStatement.getElseStatement().getChildElements() != null)) {
                this.visitorContext.getCurrentParticipant().addChildDiagramElements(ifStatement);
            }
        }
    }
    @Override
    public void visit(WhileStatementNode whileStatementNode) {
        if (isStatementBlockCommentPresent(whileStatementNode)) {
            generateStatementBlock(whileStatementNode);
        }
        boolean isHidden = isHiddenInSequenceFlagPresent(whileStatementNode);
        WhileStatement whileStatement = new WhileStatement(whileStatementNode.condition().toString(), isHidden);
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), whileStatement, this.visitorContext.getVisitedFunctionNames());
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        whileStatementNode.whileBody().accept(actionVisitor);

        if (this.visitorContext.getDiagramElementWithChildren() != null) {
            // if the while statement is inside another statement block
            if (whileStatement.getChildElements() != null) {
                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(whileStatement);
            }
        } else if (whileStatement.getChildElements() != null) {
            // Above check is to avoid adding statements without interactions to the participant
            this.visitorContext.getCurrentParticipant().addChildDiagramElements(whileStatement);
        }
    }





    @Override
    public void visit(ForEachStatementNode forEachStatementNode) {
        if (isStatementBlockCommentPresent(forEachStatementNode)) {
            generateStatementBlock(forEachStatementNode);
        }
        boolean isHidden = isHiddenInSequenceFlagPresent(forEachStatementNode);
        ForEachStatement forEachStatement = new ForEachStatement(forEachStatementNode.actionOrExpressionNode().toString(), isHidden);
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), forEachStatement, this.visitorContext.getVisitedFunctionNames());
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        forEachStatementNode.blockStatement().accept(actionVisitor);
        if (this.visitorContext.getDiagramElementWithChildren() != null) {
            if (forEachStatement.getChildElements() != null) {
                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(forEachStatement);
            }
        } else if (forEachStatement.getChildElements() != null) {
            this.visitorContext.getCurrentParticipant().addChildDiagramElements(forEachStatement);
        }
    }

    @Override
    public void visit(LockStatementNode lockStatementNode) {
        if (isStatementBlockCommentPresent(lockStatementNode)) {
            generateStatementBlock(lockStatementNode);
        }
        boolean isHidden = isHiddenInSequenceFlagPresent(lockStatementNode);
        LockStatement lockStatement = new LockStatement(isHidden);
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), lockStatement, this.visitorContext.getVisitedFunctionNames());
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        lockStatementNode.blockStatement().accept(actionVisitor);

        if (lockStatementNode.onFailClause().isPresent()) {
            OnFailStatement onFailStatement = new OnFailStatement(lockStatementNode.onFailClause().get().typeDescriptor().toString(),
                    lockStatementNode.onFailClause().get().failErrorName().toString(), isHidden);
            VisitorContext visitorContext1 = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), onFailStatement, this.visitorContext.getVisitedFunctionNames());
            ActionVisitor actionVisitor1 = new ActionVisitor(semanticModel, currentPackage, visitorContext1);
            lockStatementNode.onFailClause().get().blockStatement().accept(actionVisitor1);
            lockStatement.setOnFailStatement(onFailStatement);
        }

        if (this.visitorContext.getDiagramElementWithChildren() != null) {
            if (lockStatement.getChildElements() != null || (lockStatement.getOnFailStatement() != null && lockStatement.getOnFailStatement().getChildElements() != null)) {
                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(lockStatement);
            }
        } else if (lockStatement.getChildElements() != null || (lockStatement.getOnFailStatement() != null && lockStatement.getOnFailStatement().getChildElements() != null)) {
            this.visitorContext.getCurrentParticipant().addChildDiagramElements(lockStatement);
        }
    }

    @Override
    public void visit(DoStatementNode doStatementNode) {
        if (isStatementBlockCommentPresent(doStatementNode)) {
            generateStatementBlock(doStatementNode);
        }
        boolean isHidden = isHiddenInSequenceFlagPresent(doStatementNode);
        DoStatement doStatement = new DoStatement(isHidden);
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), doStatement, this.visitorContext.getVisitedFunctionNames());
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        doStatementNode.blockStatement().accept(actionVisitor);

        if (doStatementNode.onFailClause().isPresent()) {
            OnFailStatement onFailStatement = new OnFailStatement(
                    doStatementNode.onFailClause().get().typeDescriptor().isPresent() ? doStatementNode.onFailClause().get().typeDescriptor().get().toString() : "",
                    doStatementNode.onFailClause().get().failErrorName().isPresent() ? doStatementNode.onFailClause().get().failErrorName().get().toString() : "",
                    isHidden);
            VisitorContext visitorContext1 = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), onFailStatement, this.visitorContext.getVisitedFunctionNames());
            ActionVisitor actionVisitor1 = new ActionVisitor(semanticModel, currentPackage, visitorContext1);
            doStatementNode.onFailClause().get().blockStatement().accept(actionVisitor1);
            doStatement.setOnFailStatement(onFailStatement);
        }

        if (this.visitorContext.getDiagramElementWithChildren() != null) {
            if (doStatement.getChildElements() != null || (doStatement.getOnFailStatement() != null && doStatement.getOnFailStatement().getChildElements()!= null)) {
                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(doStatement);
            }
        } else if (doStatement.getChildElements() != null || (doStatement.getOnFailStatement() != null && doStatement.getOnFailStatement().getChildElements()!= null)) {
            this.visitorContext.getCurrentParticipant().addChildDiagramElements(doStatement);
        }
    }

//    @Override
//    public void visit(Token token) {
//        if (token.containsLeadingMinutiae()) {
//            token.leadingMinutiae().forEach(minutiae -> {
//                if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
//                    if (minutiae.text().contains("@statementBlock-Start:")) {
//                        StatementBlock statementBlock = new StatementBlock();
//                        statementBlock.setStartLocation(minutiae.parentToken().location().lineRange());
//                        statementBlock.setStatementBlockText(extractBlockComment(minutiae.text()));
////                        this.visitorContext.getCurrentParticipant().addChildDiagramElements(statementBlock);
//                        this.visitorContext.addStatementBlock(statementBlock); // pop the last statement
//                    }
//                    if (minutiae.text().contains("@statementBlock-End")) {
//                        StatementBlock statementBlock = this.visitorContext.getLastAddedStatementBlock();
//                        statementBlock.setEndLocation(minutiae.parentToken().location().lineRange());
//                        this.visitorContext.getCurrentParticipant().addChildDiagramElements(statementBlock);
//                    }
//                }
//            });
//        }
//
////        if (token.containsTrailingMinutiae()) {
////            token.trailingMinutiae().forEach(minutiae -> {
////                if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
////                    if (minutiae.text().contains("@statementBlock-End")) {
////                        StatementBlock statementBlock = this.visitorContext.getLastAddedStatementBlock();
////                        statementBlock.setEndLocation(minutiae.parentToken().location().lineRange());
////                        this.visitorContext.getCurrentParticipant().addChildDiagramElements(statementBlock);
////                    }
////                }
////            });
////        }
//    }


//    @Override
//    public void visit(AnnotationNode annotationNode){
//        if (annotationNode.annotValue().isPresent()) {
//            if(annotationNode.annotValue().get().fields().get(0).toString().contains("StatementBlock")){
//                StatementBlock statementBlock = new StatementBlock();
//                statementBlock.setLocation(annotationNode.location().lineRange());
//                statementBlock.setStatementBlockText(extractAnnotationData(annotationNode.annotValue().get().fields().get(1).toString()));
//                this.visitorContext.getCurrentParticipant().addChildDiagramElements(statementBlock);
//            }
//
//        }
//    }


    public boolean isStatementBlockCommentPresent(Node node) {
        if (!node.leadingMinutiae().isEmpty()) {
            for (Minutiae minutiae : node.leadingMinutiae()) {
                if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
                    if (minutiae.text().contains("@sq-comment:")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isCommentPresentInParentNode(Node node) {
        if (node.parent() != null) {
            if (!node.parent().leadingMinutiae().isEmpty()) {
                for (Minutiae minutiae : node.parent().leadingMinutiae()) {
                    if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isHiddenInSequenceFlagPresent(Node node) {
        if (!node.leadingMinutiae().isEmpty()) {
            for (Minutiae minutiae : node.leadingMinutiae()) {
                if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
                    if (minutiae.text().contains("@sq-ignore")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void generateStatementBlock(Node token) {
        if (!token.leadingMinutiae().isEmpty()) {
            token.leadingMinutiae().forEach(minutiae -> {
                if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
                    if (minutiae.text().contains("@sq-comment:")) {

                        // Split the input string into individual lines using the newline character as the delimiter
                        String[] lines = minutiae.text().split("\\n");

                        // Iterate through the lines and process each one
                        for (String line : lines) {
                            StatementBlock statementBlock = new StatementBlock();
                            statementBlock.setLocation(minutiae.parentToken().location().lineRange()); // TODO : Fix the correct location
                            statementBlock.setStatementBlockText(extractBlockComment(line));
                            if (this.visitorContext.getDiagramElementWithChildren() != null) {
                                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(statementBlock);
                            } else {
                                this.visitorContext.getCurrentParticipant().addChildDiagramElements(statementBlock);
                            }
                        }
//                        StatementBlock statementBlock = new StatementBlock();
//                        statementBlock.setLocation(minutiae.parentToken().location().lineRange());
//                        statementBlock.setStatementBlockText(extractBlockComment(minutiae.text()));
//                        if (this.visitorContext.getDiagramElementWithChildren() != null) {
//                            this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(statementBlock);
//                        } else {
//                            this.visitorContext.getCurrentParticipant().addChildDiagramElements(statementBlock);
//                        }
//                        this.visitorContext.getCurrentParticipant().addChildDiagramElements(statementBlock);
                    }
//                    if (minutiae.text().contains("@statementBlock-End")) {
//                        StatementBlock statementBlock = this.visitorContext.getLastAddedStatementBlock();
//                        statementBlock.setEndLocation(minutiae.parentToken().location().lineRange());
//                        this.visitorContext.getCurrentParticipant().addChildDiagramElements(statementBlock);
//                    }
                }
            });
        }

//        if (!token.trailingMinutiae().isEmpty()) {
//            token.trailingMinutiae().forEach(minutiae -> {
//                if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
//                    if (minutiae.text().contains("@statementBlock-End")) {
//                        StatementBlock statementBlock = this.visitorContext.getLastAddedStatementBlock();
//                        statementBlock.setEndLocation(minutiae.parentToken().location().lineRange());
//                        this.visitorContext.getCurrentParticipant().addChildDiagramElements(statementBlock);
//                    }
//                }
//            });
//        }
    }

    // TODO : check the node visited logic
//    private boolean isNodeVisited(NameReferenceNode functionName) {
//        return visitorContext.getVisitedFunctionNames().stream().anyMatch(nameNode -> {
//            if (functionName instanceof SimpleNameReferenceNode) {
//                if ((nameNode instanceof QualifiedNameReferenceNode && modulePrefix != null)) {
//                    return getQualifiedNameRefNodeFuncNameText((QualifiedNameReferenceNode) nameNode)
//                            .equals(modulePrefix + ":" + ((SimpleNameReferenceNode) functionName).name().text());
//                } else if (nameNode instanceof SimpleNameReferenceNode) {
//                    return ((SimpleNameReferenceNode) nameNode).name().text()
//                            .equals(((SimpleNameReferenceNode) functionName).name().text());
//                }
//            } else if (functionName instanceof QualifiedNameReferenceNode) {
//                return getQualifiedNameRefNodeFuncNameText((QualifiedNameReferenceNode) nameNode)
//                        .equals(getQualifiedNameRefNodeFuncNameText((QualifiedNameReferenceNode) functionName));
//            }
//            return false;
//        });
//    }

    private boolean isNodeVisited(NameReferenceNode functionName) {
        if (functionName instanceof SimpleNameReferenceNode) {
            return visitorContext.getVisitedFunctionNames().stream().anyMatch(nameNode -> {
                if (nameNode instanceof SimpleNameReferenceNode) {
                    return ((SimpleNameReferenceNode) nameNode).name().text()
                            .equals(((SimpleNameReferenceNode) functionName).name().text());
                } else if (nameNode instanceof QualifiedNameReferenceNode && modulePrefix != null) {
                    return getQualifiedNameRefNodeFuncNameText((QualifiedNameReferenceNode) nameNode)
                            .equals(modulePrefix + ":" + ((SimpleNameReferenceNode) functionName).name().text());
                }
                return false;
            });
        } else if (functionName instanceof QualifiedNameReferenceNode) {
            return visitorContext.getVisitedFunctionNames().stream().anyMatch(nameNode -> {
                if (nameNode instanceof QualifiedNameReferenceNode) {
                    return getQualifiedNameRefNodeFuncNameText((QualifiedNameReferenceNode) nameNode)
                            .equals(getQualifiedNameRefNodeFuncNameText((QualifiedNameReferenceNode) functionName));
                }
                return false;
            });
        }
        return false;
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

    private boolean isEndpointPresent(String clientID) {
        for (Participant endpoint : visitorContext.getParticipants()) {
            if (endpoint.getParticipantKind().toString().equals("ENDPOINT")) {
                if (endpoint.getId().equals(clientID.trim())) {
                    return true; // Return true when the conditions are met
                }
            }
        }
        return false; // Return false if no match is found
    }

    private static String removeDoubleQuotes(String input) {
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        }
        return input.trim();
    }

    private String extractBlockComment(String input) {
        String prefix = "@sq-comment:";
        int startIndex = input.indexOf(prefix);

        if (startIndex != -1) {
            String extracted = input.substring(startIndex + prefix.length()).trim();
            return extracted;
        }
        return null;
    }

    private String extractAnnotationData(String input) {
        String prefix = "description:";
        int startIndex = input.indexOf(prefix);

        if (startIndex != -1) {
            String extracted = input.substring(startIndex + prefix.length()).trim();
            return removeDoubleQuotes(extracted);
        }
        return null;
    }
}
