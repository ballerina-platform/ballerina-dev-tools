package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BreakStatementNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ClientResourceAccessActionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ContinueStatementNode;
import io.ballerina.compiler.syntax.tree.DoStatementNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FailStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.ForkStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.LocalTypeDefinitionStatementNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MatchClauseNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.NamedWorkerDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.OnFailClauseNode;
import io.ballerina.compiler.syntax.tree.PanicStatementNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.RetryStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.RollbackStatementNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TransactionStatementNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.compiler.syntax.tree.XMLNamespaceDeclarationNode;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Package;
import io.ballerina.sequencemodelgenerator.core.model.*;
import io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils;
import io.ballerina.tools.diagnostics.Location;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils.getRawType;

public class ActionVisitor extends NodeVisitor {
    private final SemanticModel semanticModel;
    private final Package currentPackage;
    private final VisitorContext visitorContext;
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

    // TODO:
//    action-stmt
//   | destructuring-assignment-stmt
//   | retry-transaction-stmt

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        if ((functionCallExpressionNode.functionName() instanceof SimpleNameReferenceNode ||
                functionCallExpressionNode.functionName() instanceof QualifiedNameReferenceNode)) {

            Optional<Symbol> symbol = semanticModel.symbol(functionCallExpressionNode.functionName());
            boolean isHidden = this.isHiddenVariableStmt();
                functionCallExpressionNode.arguments().forEach(functionArgumentNode -> {
                    if (semanticModel.symbol(functionArgumentNode).isPresent()) {
                        if (semanticModel.symbol(functionArgumentNode).get().kind().equals(SymbolKind.FUNCTION)) {
                            functionArgumentNode.accept(this);
                        }
                    }
                });
                symbol.ifPresent(value -> findInteractions(functionCallExpressionNode.functionName(), value, isHidden, null));
        }
    }

    @Override
    public void visit(MethodCallExpressionNode methodCallExpressionNode) {
        if ((methodCallExpressionNode.methodName() instanceof SimpleNameReferenceNode ||
                methodCallExpressionNode.methodName() instanceof QualifiedNameReferenceNode)) {

            Optional<Symbol> symbol = semanticModel.symbol(methodCallExpressionNode.methodName());
            boolean isHidden = this.isHiddenVariableStmt();
            symbol.ifPresent(value -> findInteractions(methodCallExpressionNode.methodName(), value, isHidden, methodCallExpressionNode.expression()));
        }
    }


    @Override
    public void visit(ExpressionStatementNode expressionStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(expressionStatementNode)) {
            generateStatementBlock(expressionStatementNode);
        }
        if (!expressionStatementNode.expression().isMissing()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(expressionStatementNode);
            this.setHiddenVariableStmt(isHidden);
            expressionStatementNode.expression().accept(this);
        }
    }

   @Override
   public void visit(BlockStatementNode blockStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(blockStatementNode)) {
            generateStatementBlock(blockStatementNode);
        }
        if (!blockStatementNode.statements().isEmpty()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(blockStatementNode);
            this.setHiddenVariableStmt(isHidden);
            for (StatementNode statementNode : blockStatementNode.statements()) {
                statementNode.accept(this);
            }
        }
   }


    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(variableDeclarationNode)) {
            generateStatementBlock(variableDeclarationNode);
        }
        if (variableDeclarationNode.initializer().isPresent()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(variableDeclarationNode);
            this.setHiddenVariableStmt(isHidden);
            variableDeclarationNode.initializer().get().accept(this);
        }
    }

    @Override
    public void visit(CompoundAssignmentStatementNode compoundAssignmentStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(compoundAssignmentStatementNode)) {
            generateStatementBlock(compoundAssignmentStatementNode);
        }
        if (!compoundAssignmentStatementNode.rhsExpression().isMissing()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(compoundAssignmentStatementNode);
            this.setHiddenVariableStmt(isHidden);
            compoundAssignmentStatementNode.rhsExpression().accept(this);
        }
    }

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(returnStatementNode)) {
            generateStatementBlock(returnStatementNode);
        }
        if (returnStatementNode.expression().isPresent()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(returnStatementNode);
            this.setHiddenVariableStmt(isHidden);
            returnStatementNode.expression().get().accept(this);
        }
    }



    @Override
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(assignmentStatementNode)) {
            generateStatementBlock(assignmentStatementNode);
        }
        if (!assignmentStatementNode.expression().isMissing()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(assignmentStatementNode);
            this.setHiddenVariableStmt(isHidden);
            assignmentStatementNode.expression().accept(this);
        }
    }

    @Override
    public void visit(BreakStatementNode breakStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(breakStatementNode)) {
            generateStatementBlock(breakStatementNode);
        }
    }

    @Override
    public void visit(ContinueStatementNode continueStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(continueStatementNode)) {
            generateStatementBlock(continueStatementNode);
        }
    }

    @Override
    public void visit(PanicStatementNode panicStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(panicStatementNode)) {
            generateStatementBlock(panicStatementNode);
        }
        if (!panicStatementNode.expression().isMissing()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(panicStatementNode);
            this.setHiddenVariableStmt(isHidden);
            panicStatementNode.expression().accept(this);
        }
    }


    @Override
    public void visit(ForkStatementNode forkStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(forkStatementNode)) {
            generateStatementBlock(forkStatementNode);
        }
        if (!forkStatementNode.namedWorkerDeclarations().isEmpty()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(forkStatementNode);
            this.setHiddenVariableStmt(isHidden);
            for (NamedWorkerDeclarationNode namedWorkerDeclarationNode : forkStatementNode.namedWorkerDeclarations()) {
                namedWorkerDeclarationNode.accept(this);
            }
        }
    }

    @Override
    public void visit(LocalTypeDefinitionStatementNode localTypeDefinitionStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(localTypeDefinitionStatementNode)) {
            generateStatementBlock(localTypeDefinitionStatementNode);
        }
    }

    @Override
    public void visit(MatchStatementNode matchStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(matchStatementNode)) {
            generateStatementBlock(matchStatementNode);
        }
        if (!matchStatementNode.condition().isMissing()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(matchStatementNode);
            this.setHiddenVariableStmt(isHidden);
            matchStatementNode.condition().accept(this);
        }
        if (!matchStatementNode.matchClauses().isEmpty()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(matchStatementNode);
            this.setHiddenVariableStmt(isHidden);
            for (MatchClauseNode matchClauseNode : matchStatementNode.matchClauses()) {
                matchClauseNode.accept(this);
            }
        }
    }

    @Override
    public void visit(RetryStatementNode retryStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(retryStatementNode)) {
            generateStatementBlock(retryStatementNode);
        }
        if (!retryStatementNode.retryBody().isMissing()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(retryStatementNode);
            this.setHiddenVariableStmt(isHidden);
            retryStatementNode.retryBody().accept(this);
        }
        // TODO: check if the retry statement has on fail statement node, if so generate a retry statement type
    }

    @Override
    public void visit(XMLNamespaceDeclarationNode xmlNamespaceDeclarationNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(xmlNamespaceDeclarationNode)) {
            generateStatementBlock(xmlNamespaceDeclarationNode);
        }
    }

    @Override
    public void visit(TransactionStatementNode transactionStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(transactionStatementNode)) {
            generateStatementBlock(transactionStatementNode);
        }
        if (!transactionStatementNode.blockStatement().isMissing()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(transactionStatementNode);
            this.setHiddenVariableStmt(isHidden);
            transactionStatementNode.blockStatement().accept(this);
        }
    }

    @Override
    public void visit(RollbackStatementNode rollbackStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(rollbackStatementNode)) {
            generateStatementBlock(rollbackStatementNode);
        }
        if(rollbackStatementNode.expression().isPresent()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(rollbackStatementNode);
            this.setHiddenVariableStmt(isHidden);
            rollbackStatementNode.expression().get().accept(this);
        }
    }

    @Override
    public void visit(FailStatementNode failStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(failStatementNode)) {
            generateStatementBlock(failStatementNode);
        }
        if(!failStatementNode.expression().isMissing()) {
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(failStatementNode);
            this.setHiddenVariableStmt(isHidden);
            failStatementNode.expression().accept(this);
        }
    }



    //self.httpEp->/users/names.get();
    // calls the resource functions of the client
    @Override
    public void visit(ClientResourceAccessActionNode clientResourceAccessActionNode) {
        NameReferenceNode clientNode = null;
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

            Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(clientNode);

            if (typeSymbol.isPresent()) {
                TypeSymbol rawType = getRawType(typeSymbol.get());
                if (rawType.typeKind() == TypeDescKind.OBJECT) {
                    ObjectTypeSymbol objectTypeSymbol = (ObjectTypeSymbol) rawType;
                    boolean isEndpoint = objectTypeSymbol.qualifiers().contains(Qualifier.CLIENT);

                    if (isEndpoint && clientNode != null && objectTypeSymbol.getModule().isPresent()) {
                        String clientID = ModelGeneratorUtils.generateEndpointID(objectTypeSymbol, clientNode);
                        if (clientID != null) {
                            // TODO: recheck the logic on creating resource access path
                            SeparatedNodeList<Node> resourceAccessPath = clientResourceAccessActionNode.resourceAccessPath();
                            // iterate over the resourceAccessPath and create resourcePath by concatenating all the strings in SeparatedNodeList
                            String resourcePath = "";

                            for (Node node : resourceAccessPath) {
                                if (node.kind().equals(SyntaxKind.IDENTIFIER_TOKEN)) {
                                    resourcePath = resourcePath.concat("/");
                                    // Removing escape character from resourcePath
                                    String replaced = ((Token) node).text().trim().replaceAll("\\\\-", "-");
                                    resourcePath = resourcePath.concat(replaced).trim();
                                } else if (node.kind().equals(SyntaxKind.COMPUTED_RESOURCE_ACCESS_SEGMENT)) {
                                    resourcePath = resourcePath.concat("/");
                                    String replaced = node.toSourceCode().trim().replaceAll("\\\\-", "-");
                                    resourcePath = resourcePath.concat(replaced);
                                }
                            }
                            if (resourcePath.isEmpty()) {
                                resourcePath = "/";
                            }

                            String clientPkgName = ModelGeneratorUtils.generateModuleIDFromSymbol(objectTypeSymbol);

                            if (!isEndpointPresent(clientID) && clientPkgName != null) {
                                Participant participant = new Participant(clientID, clientNode.toString().trim(),
                                        ParticipantKind.ENDPOINT, clientPkgName, objectTypeSymbol.signature().trim(),
                                        objectTypeSymbol.getLocation().isPresent() ? objectTypeSymbol.getLocation().get().lineRange() : null);
                                this.visitorContext.addToParticipants(participant);
                            }

                            // default is get method
                            String methodName = "get";
                            if (clientResourceAccessActionNode.methodName().isPresent()) {
                                methodName = clientResourceAccessActionNode.methodName().get().toString().trim();
                            }

                            boolean isHidden = this.isHiddenVariableStmt();
                            EndpointActionStatement actionStatement = new EndpointActionStatement(
                                    this.visitorContext.getCurrentParticipant().getId().trim(), clientID, clientNode.toString().trim(),
                                    methodName, resourcePath, isHidden, Constants.ActionType.RESOURCE_ACTION, clientResourceAccessActionNode.lineRange());
                            if (this.visitorContext.getDiagramElementWithChildren() != null) {
                                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(actionStatement);
                            } else {
                                this.visitorContext.getCurrentParticipant().addChildDiagramElements(actionStatement);
                            }
                        }
                    }
                }
            }
    }


    // self.sheetsEp->createSpreadsheet(name = "");
    // calls the remote function of the client
    // remote calls doesnt have path params
    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        NameReferenceNode clientNode = null;

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

                            String clientID = ModelGeneratorUtils.generateEndpointID(objectTypeSymbol, clientNode);

                            String clientPkgName = ModelGeneratorUtils.generateModuleIDFromSymbol(objectTypeSymbol);
                            if (!isEndpointPresent(clientID) && clientPkgName != null) {
                                Participant participant = new Participant(clientID, clientNode.toString().trim(),
                                        ParticipantKind.ENDPOINT, clientPkgName, objectTypeSymbol.signature().trim(),
                                        objectTypeSymbol.getLocation().isPresent() ? objectTypeSymbol.getLocation().get().lineRange() : null);
                                this.visitorContext.addToParticipants(participant);
                            }

//                            SeparatedNodeList<FunctionArgumentNode> resourceAccessPath = remoteMethodCallActionNode.arguments();
                            // iterate over the resourceAccessPath and create resourcePath by concatenating all the strings in SeparatedNodeList
//                            String resourceName = "";
//                            for (Node node : resourceAccessPath) {
//                                if (node.kind().equals(SyntaxKind.POSITIONAL_ARG)) {
//                                    resourceName = resourceName.concat(node.toSourceCode());
//                                }
//                            }

                            // boolean isHidden = isHiddenInSequenceFlagPresent(remoteMethodCallActionNode);
                            boolean isHidden = this.isHiddenVariableStmt();
                            EndpointActionStatement actionStatement = new EndpointActionStatement(
                                    this.visitorContext.getCurrentParticipant().getId().trim(), clientID, clientNode.toString().trim(),
                                    remoteMethodCallActionNode.methodName().toString().trim(), null, isHidden,
                                    Constants.ActionType.REMOTE_ACTION, remoteMethodCallActionNode.lineRange());
                            if (this.visitorContext.getDiagramElementWithChildren() != null) {
                                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(actionStatement);
                            } else {
                                this.visitorContext.getCurrentParticipant().addChildDiagramElements(actionStatement);
                            }
                        }
                    }
                }
            }
    }

    private void findInteractions(NameReferenceNode nameNode, Symbol methodSymbol, boolean isHidden, ExpressionNode expression) {
        if (isNodeVisited(nameNode, expression)) {
            if (semanticModel.symbol(nameNode).isPresent() && semanticModel.symbol(nameNode).get().getModule().isPresent()) {
                String functionName = null;
                if (nameNode instanceof QualifiedNameReferenceNode) {
                    functionName = ((QualifiedNameReferenceNode) nameNode).identifier().text().trim();
                } else if (nameNode instanceof SimpleNameReferenceNode) {
                    functionName = ((SimpleNameReferenceNode) nameNode).name().text().trim();
                }
                Optional<Symbol> symbol = semanticModel.symbol(nameNode);
                if (symbol.isPresent()) {
                    String referenceNodeID;
                    if (expression != null && semanticModel.typeOf(expression).isPresent() && functionName != null) {
                        referenceNodeID = ModelGeneratorUtils.generateReferenceIDForMethods(semanticModel.typeOf(expression).get().signature(), functionName);
                    } else {
                        referenceNodeID = ModelGeneratorUtils.generateReferenceID(symbol.get(), functionName);
                    }
//                    String referenceNodeID = ModelGeneratorUtils.generateReferenceID(symbol.get(),functionName);
                    if (referenceNodeID != null) {
                        Participant participant = getParticipantByID(referenceNodeID);
                        if (participant != null) {
                            Interaction interaction;
                            if (expression != null) {
                                interaction = new MethodActionStatement(visitorContext.getCurrentParticipant().getId().trim(),
                                        participant.getId().trim(), functionName, expression.toSourceCode().trim(), isHidden, nameNode.lineRange());
                            } else {
                                interaction = new FunctionActionStatement(visitorContext.getCurrentParticipant().getId().trim(),
                                        participant.getId().trim(), functionName, isHidden, nameNode.lineRange());
                            }
                            // Todo: make functionName include module prefix
//                            FunctionActionStatement functionActionStatement = new FunctionActionStatement(visitorContext.getCurrentParticipant().getId().trim(),
//                                    participant.getId().trim(), functionName, isHidden);
                            if (this.visitorContext.getDiagramElementWithChildren() != null) {
                                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(interaction);
                            } else {
                                visitorContext.getCurrentParticipant().addChildDiagramElements(interaction);
                            }
                        }
                    }

                }
            }
        } else {
            Optional<Location> location = methodSymbol.getLocation();
            Optional<ModuleSymbol> optionalModuleSymbol = methodSymbol.getModule();
            if (optionalModuleSymbol.isPresent() && location.isPresent()) {
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


                                SemanticModel nextSemanticModel = currentPackage.getCompilation().getSemanticModel(module.moduleId());
                                ActionVisitor actionVisitor = new ActionVisitor(nextSemanticModel, currentPackage, visitorContext);
                                node.accept(actionVisitor);



                                    String functionName = null;
                                    if (nameNode.kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
                                        functionName = ((QualifiedNameReferenceNode) nameNode).identifier().text().trim();

                                    } else if (nameNode.kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE)) {
                                        functionName = ((SimpleNameReferenceNode) nameNode).name().text().trim();
                                    }

                                    Participant targetParticipant = null;
                                    String referenceModuleID;
                                    if (semanticModel.symbol(nameNode).isPresent() && semanticModel.symbol(nameNode).get().getModule().isPresent()) {
                                        if (expression != null && semanticModel.typeOf(expression).isPresent() && functionName != null) {
                                            referenceModuleID = ModelGeneratorUtils.generateReferenceIDForMethods(semanticModel.typeOf(expression).get().signature(), functionName);
                                        } else {
                                            referenceModuleID = ModelGeneratorUtils.generateReferenceID(semanticModel.symbol(nameNode).get(), functionName);
                                        }
                                        if (referenceModuleID != null) {
                                            visitorContext.addToVisitedFunctionNames(referenceModuleID);
                                            targetParticipant = getParticipantByID(referenceModuleID);
                                        }
                                    }

                                    if (targetParticipant != null) {
                                        Interaction interaction;
                                        if (expression != null) {
                                            interaction = new MethodActionStatement(visitorContext.getCurrentParticipant().getId().trim(),
                                                    targetParticipant.getId().trim(), functionName, expression.toSourceCode().trim(), isHidden, nameNode.lineRange());
                                        } else {
                                            interaction = new FunctionActionStatement(visitorContext.getCurrentParticipant().getId().trim(),
                                                    targetParticipant.getId().trim(), functionName, isHidden, nameNode.lineRange());
                                        }
//                                        FunctionActionStatement functionActionStatement = new FunctionActionStatement(visitorContext.getCurrentParticipant().getId().trim(),
//                                                targetParticipant.getId().trim(), functionName, isHidden);

                                        if (this.visitorContext.getDiagramElementWithChildren() != null) {
                                            this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(interaction);
                                        } else {
                                            visitorContext.getCurrentParticipant().addChildDiagramElements(interaction);
                                        }
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
            if (typeSymbol.isPresent() && typeSymbol.get().getModule().isPresent()) {
                String functionID = null;
                String packageName = typeSymbol.get().getModule().get().id().packageName().trim();
                if (typeSymbol.get().kind().equals(SymbolKind.METHOD)) {
                    if (functionDefinitionNode.parent().kind().equals(SyntaxKind.CLASS_DEFINITION)) {
                        ClassDefinitionNode classDefinitionNode = (ClassDefinitionNode) functionDefinitionNode.parent();
                        functionID = ModelGeneratorUtils.generateMethodID(typeSymbol.get(), classDefinitionNode.className().text().trim(), functionDefinitionNode);

                    }
                } else {
                    functionID = ModelGeneratorUtils.generateFunctionID(typeSymbol.get(), functionDefinitionNode);
                }
                if (functionID != null && !ModelGeneratorUtils.isInParticipantList(functionID, visitorContext.getParticipants())) {
                    Participant participant = new Participant(functionID,
                            functionDefinitionNode.functionName().text().trim(), ParticipantKind.WORKER, packageName, functionDefinitionNode.lineRange());
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
            if (ModelGeneratorUtils.isStatementBlockCommentPresent(ifElseStatementNode)) {
                generateStatementBlock(ifElseStatementNode);
            }
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(ifElseStatementNode);
            if (!ifElseStatementNode.condition().isMissing()) {
                ifElseStatementNode.condition().accept(this);
            }
            IfStatement ifStatement = new IfStatement(ifElseStatementNode.condition().toString(), isHidden, ifElseStatementNode.lineRange());
            VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), ifStatement, this.visitorContext.getVisitedFunctionNames());
            ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
            ifElseStatementNode.ifBody().accept(actionVisitor);

            if (ifElseStatementNode.elseBody().isPresent()) {
                ElseStatement elseStatement = new ElseStatement(isHidden, ifElseStatementNode.lineRange());
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
                    if (ifStatement.getElementBody() != null || (ifStatement.getElseStatement() != null && ifStatement.getElseStatement().getElementBody() != null)) {
                        this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(ifStatement);
                    }
                }
            } else {
                if (ifStatement.getElementBody() != null || (ifStatement.getElseStatement() != null && ifStatement.getElseStatement().getElementBody() != null)) {
                    this.visitorContext.getCurrentParticipant().addChildDiagramElements(ifStatement);
                }
            }
    }

    @Override
    public void visit(WhileStatementNode whileStatementNode) {
            if (ModelGeneratorUtils.isStatementBlockCommentPresent(whileStatementNode)) {
                generateStatementBlock(whileStatementNode);
            }
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(whileStatementNode);
            if (!whileStatementNode.condition().isMissing()) {
                whileStatementNode.condition().accept(this);
            }
            WhileStatement whileStatement = new WhileStatement(whileStatementNode.condition().toString(), isHidden, whileStatementNode.lineRange());
            VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), whileStatement, this.visitorContext.getVisitedFunctionNames());
            ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
            whileStatementNode.whileBody().accept(actionVisitor);

        if (whileStatementNode.onFailClause().isPresent()) {
            OnFailClauseNode onFailClauseNode = whileStatementNode.onFailClause().get();
            OnFailStatement onFailStatement = new OnFailStatement(
                    onFailClauseNode.typeDescriptor().isPresent() ?
                            onFailClauseNode.typeDescriptor().get().toString() : "",
                    onFailClauseNode.failErrorName().isPresent() ?
                            onFailClauseNode.failErrorName().get().toString() : "" , isHidden, whileStatementNode.onFailClause().get().lineRange());
            VisitorContext visitorContext1 = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), onFailStatement, this.visitorContext.getVisitedFunctionNames());
            ActionVisitor actionVisitor1 = new ActionVisitor(semanticModel, currentPackage, visitorContext1);
            whileStatementNode.onFailClause().get().blockStatement().accept(actionVisitor1);
            whileStatement.setOnFailStatement(onFailStatement);
        }

            if (this.visitorContext.getDiagramElementWithChildren() != null) {
                // if the while statement is inside another statement block
                if (whileStatement.getElementBody() != null) {
                    this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(whileStatement);
                }
            } else if (whileStatement.getElementBody() != null) {
                // Above check is to avoid adding statements without interactions to the participant
                this.visitorContext.getCurrentParticipant().addChildDiagramElements(whileStatement);
            }
    }

    @Override
    public void visit(ForEachStatementNode forEachStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(forEachStatementNode)) {
            generateStatementBlock(forEachStatementNode);
        }
        boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(forEachStatementNode);
        if (!forEachStatementNode.actionOrExpressionNode().isMissing()) {
            forEachStatementNode.actionOrExpressionNode().accept(this);
        }
        ForEachStatement forEachStatement = new ForEachStatement(forEachStatementNode.actionOrExpressionNode().toString(), isHidden, forEachStatementNode.lineRange());
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), forEachStatement, this.visitorContext.getVisitedFunctionNames());
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        forEachStatementNode.blockStatement().accept(actionVisitor);

        if (forEachStatementNode.onFailClause().isPresent()) {
            OnFailClauseNode onFailClauseNode = forEachStatementNode.onFailClause().get();
            OnFailStatement onFailStatement = new OnFailStatement(
                    onFailClauseNode.typeDescriptor().isPresent() ?
                            onFailClauseNode.typeDescriptor().get().toString() : "",
                    onFailClauseNode.failErrorName().isPresent() ?
                            onFailClauseNode.failErrorName().get().toString() : "" , isHidden, forEachStatementNode.onFailClause().get().lineRange());
            VisitorContext visitorContext1 = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), onFailStatement, this.visitorContext.getVisitedFunctionNames());
            ActionVisitor actionVisitor1 = new ActionVisitor(semanticModel, currentPackage, visitorContext1);
            forEachStatementNode.onFailClause().get().blockStatement().accept(actionVisitor1);
            forEachStatement.setOnFailStatement(onFailStatement);
        }

        if (this.visitorContext.getDiagramElementWithChildren() != null) {
            if (forEachStatement.getElementBody() != null) {
                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(forEachStatement);
            }
        } else if (forEachStatement.getElementBody() != null) {
            this.visitorContext.getCurrentParticipant().addChildDiagramElements(forEachStatement);
        }
    }

    @Override
    public void visit(LockStatementNode lockStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(lockStatementNode)) {
            generateStatementBlock(lockStatementNode);
        }
        boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(lockStatementNode);
        LockStatement lockStatement = new LockStatement(isHidden, lockStatementNode.lineRange());
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), lockStatement, this.visitorContext.getVisitedFunctionNames());
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        lockStatementNode.blockStatement().accept(actionVisitor);

        if (lockStatementNode.onFailClause().isPresent()) {
            OnFailClauseNode onFailClauseNode = lockStatementNode.onFailClause().get();
            OnFailStatement onFailStatement = new OnFailStatement(
                    onFailClauseNode.typeDescriptor().isPresent() ?
                            onFailClauseNode.typeDescriptor().get().toString() : "",
                    onFailClauseNode.failErrorName().isPresent() ?
                            onFailClauseNode.failErrorName().get().toString() : "" , isHidden, lockStatementNode.onFailClause().get().lineRange());
            VisitorContext visitorContext1 = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), onFailStatement, this.visitorContext.getVisitedFunctionNames());
            ActionVisitor actionVisitor1 = new ActionVisitor(semanticModel, currentPackage, visitorContext1);
            lockStatementNode.onFailClause().get().blockStatement().accept(actionVisitor1);
            lockStatement.setOnFailStatement(onFailStatement);
        }

        if (this.visitorContext.getDiagramElementWithChildren() != null) {
            if (lockStatement.getElementBody() != null || (lockStatement.getOnFailStatement() != null && lockStatement.getOnFailStatement().getElementBody() != null)) {
                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(lockStatement);
            }
        } else if (lockStatement.getElementBody() != null || (lockStatement.getOnFailStatement() != null && lockStatement.getOnFailStatement().getElementBody() != null)) {
            this.visitorContext.getCurrentParticipant().addChildDiagramElements(lockStatement);
        }
    }

    @Override
    public void visit(DoStatementNode doStatementNode) {
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(doStatementNode)) {
            generateStatementBlock(doStatementNode);
        }
        boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(doStatementNode);
        DoStatement doStatement = new DoStatement(isHidden, doStatementNode.lineRange());
        VisitorContext visitorContext = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                this.visitorContext.getParticipants(), doStatement, this.visitorContext.getVisitedFunctionNames());
        ActionVisitor actionVisitor = new ActionVisitor(semanticModel, currentPackage, visitorContext);
        doStatementNode.blockStatement().accept(actionVisitor);

        if (doStatementNode.onFailClause().isPresent()) {
            OnFailStatement onFailStatement = new OnFailStatement(
                    doStatementNode.onFailClause().get().typeDescriptor().isPresent() ? doStatementNode.onFailClause().get().typeDescriptor().get().toString() : "",
                    doStatementNode.onFailClause().get().failErrorName().isPresent() ? doStatementNode.onFailClause().get().failErrorName().get().toString() : "",
                    isHidden, doStatementNode.onFailClause().get().lineRange());
            VisitorContext visitorContext1 = new VisitorContext(this.visitorContext.getRootParticipant(), this.visitorContext.getCurrentParticipant(),
                    this.visitorContext.getParticipants(), onFailStatement, this.visitorContext.getVisitedFunctionNames());
            ActionVisitor actionVisitor1 = new ActionVisitor(semanticModel, currentPackage, visitorContext1);
            doStatementNode.onFailClause().get().blockStatement().accept(actionVisitor1);
            doStatement.setOnFailStatement(onFailStatement);
        }

        if (this.visitorContext.getDiagramElementWithChildren() != null) {
            if (doStatement.getElementBody() != null || (doStatement.getOnFailStatement() != null && doStatement.getOnFailStatement().getElementBody() != null)) {
                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(doStatement);
            }
        } else if (doStatement.getElementBody() != null || (doStatement.getOnFailStatement() != null && doStatement.getOnFailStatement().getElementBody() != null)) {
            this.visitorContext.getCurrentParticipant().addChildDiagramElements(doStatement);
        }
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
                            StatementBlock statementBlock = new StatementBlock(minutiae.lineRange());
                            statementBlock.setStatementBlockText(ModelGeneratorUtils.extractBlockComment(line));
                            if (this.visitorContext.getDiagramElementWithChildren() != null) {
                                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(statementBlock);
                            } else {
                                this.visitorContext.getCurrentParticipant().addChildDiagramElements(statementBlock);
                            }
                        }
                    }
                }
            });
        }
    }

    private boolean isNodeVisited(NameReferenceNode functionReferenceNode, ExpressionNode expression) {
        if (semanticModel.symbol(functionReferenceNode).isPresent() && semanticModel.symbol(functionReferenceNode).get().getModule().isPresent()) {
            String functionName = null;
            if (functionReferenceNode instanceof QualifiedNameReferenceNode) {
                functionName = ((QualifiedNameReferenceNode) functionReferenceNode).identifier().text().trim();
            } else if (functionReferenceNode instanceof SimpleNameReferenceNode) {
                functionName = ((SimpleNameReferenceNode) functionReferenceNode).name().text().trim();
            }
            String nameNodeModuleID;
            if (expression != null && semanticModel.typeOf(expression).isPresent() && functionName != null) {
                nameNodeModuleID = ModelGeneratorUtils.generateReferenceIDForMethods(semanticModel.typeOf(expression).get().signature(), functionName);
            } else {
                nameNodeModuleID = ModelGeneratorUtils.generateReferenceID(semanticModel.symbol(functionReferenceNode).get(), functionName);
            }
//            String nameNodeModuleID = semanticModel.symbol(functionReferenceNode).get().getModule().get().id().toString().trim().replace(":", "_") + "_" + functionName;
            if (nameNodeModuleID != null) {
                return this.visitorContext.getVisitedFunctionNames().contains(nameNodeModuleID);
            } else {
                return false;
            }

//            return this.visitorContext.getVisitedFunctionNames().contains(nameNodeModuleID);
        }
        return false;
    }

    private Participant getParticipantByID(String participantID) {
        for (Participant item : visitorContext.getParticipants()) {
            if (item.getId().trim().equals(participantID.trim())) {
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
}
