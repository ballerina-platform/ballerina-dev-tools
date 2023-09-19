package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ClientResourceAccessActionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.DoStatementNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Package;
import io.ballerina.sequencemodelgenerator.core.model.DoStatement;
import io.ballerina.sequencemodelgenerator.core.model.ElseStatement;
import io.ballerina.sequencemodelgenerator.core.model.EndpointActionStatement;
import io.ballerina.sequencemodelgenerator.core.model.ForEachStatement;
import io.ballerina.sequencemodelgenerator.core.model.FunctionActionStatement;
import io.ballerina.sequencemodelgenerator.core.model.IfStatement;
import io.ballerina.sequencemodelgenerator.core.model.LockStatement;
import io.ballerina.sequencemodelgenerator.core.model.OnFailStatement;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.ParticipantKind;
import io.ballerina.sequencemodelgenerator.core.model.StatementBlock;
import io.ballerina.sequencemodelgenerator.core.model.WhileStatement;
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

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        if ((functionCallExpressionNode.functionName() instanceof SimpleNameReferenceNode ||
                functionCallExpressionNode.functionName() instanceof QualifiedNameReferenceNode)) {

            Optional<Symbol> symbol = semanticModel.symbol(functionCallExpressionNode.functionName());
            boolean isHidden = this.isHiddenVariableStmt();
                symbol.ifPresent(value -> findInteractions(functionCallExpressionNode.functionName(), value, isHidden));
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
                            String resourceName = "";

                            for (Node node : resourceAccessPath) {
                                if (node.kind().equals(SyntaxKind.IDENTIFIER_TOKEN)) {
                                    if (!resourceName.isEmpty()) {
                                        resourceName = resourceName.concat("/");
                                    }
                                    resourceName = resourceName.concat(((Token) node).text()).trim();
                                }
                            }

                            String clientPkgName = ModelGeneratorUtils.generateModuleIDFromSymbol(objectTypeSymbol);

                            if (!isEndpointPresent(clientID) && clientPkgName != null) {
                                Participant participant = new Participant(clientID, clientNode.toString().trim(),
                                        ParticipantKind.ENDPOINT, clientPkgName, objectTypeSymbol.signature().trim());
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
    }


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
                                    remoteMethodCallActionNode.methodName().toString().trim(), ModelGeneratorUtils.removeDoubleQuotes(resourceName), isHidden);
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

    private void findInteractions(NameReferenceNode nameNode, Symbol methodSymbol, boolean isHidden) {
        if (isNodeVisited(nameNode)) {
            if (semanticModel.symbol(nameNode).isPresent() && semanticModel.symbol(nameNode).get().getModule().isPresent()) {
                String functionName = "";
                if (nameNode instanceof QualifiedNameReferenceNode) {
                    functionName = ((QualifiedNameReferenceNode) nameNode).identifier().text().trim();
                } else if (nameNode instanceof SimpleNameReferenceNode) {
                    functionName = ((SimpleNameReferenceNode) nameNode).name().text().trim();
                }
                Optional<Symbol> symbol = semanticModel.symbol(nameNode);
                if (symbol.isPresent()) {
                    String referenceNodeID = ModelGeneratorUtils.generateReferenceID(symbol.get(),functionName);
                    if (referenceNodeID != null) {
                        Participant participant = getParticipantByID(referenceNodeID);
                        if (participant != null) {
                            // Todo: maeke functioNAme include module prefix
                            FunctionActionStatement functionActionStatement = new FunctionActionStatement(visitorContext.getCurrentParticipant().getId().trim(),
                                    participant.getId().trim(), functionName, isHidden);
                            if (this.visitorContext.getDiagramElementWithChildren() != null) {
                                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(functionActionStatement);
                            } else {
                                visitorContext.getCurrentParticipant().addChildDiagramElements(functionActionStatement);
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
                                        referenceModuleID = ModelGeneratorUtils.generateReferenceID(semanticModel.symbol(nameNode).get(), functionName);
                                        if (referenceModuleID != null) {
                                            visitorContext.addToVisitedFunctionNames(referenceModuleID);
                                            targetParticipant = getParticipantByID(referenceModuleID);
                                        }
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
                String packageName = typeSymbol.get().getModule().get().id().packageName().trim();
                String functionID = ModelGeneratorUtils.generateFunctionID(typeSymbol.get(), functionDefinitionNode);

                if (functionID != null && !ModelGeneratorUtils.isInParticipantList(functionID, visitorContext.getParticipants())) {
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
            if (ModelGeneratorUtils.isStatementBlockCommentPresent(ifElseStatementNode)) {
                generateStatementBlock(ifElseStatementNode);
            }
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(ifElseStatementNode);
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
            if (ModelGeneratorUtils.isStatementBlockCommentPresent(whileStatementNode)) {
                generateStatementBlock(whileStatementNode);
            }
            boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(whileStatementNode);
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
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(forEachStatementNode)) {
            generateStatementBlock(forEachStatementNode);
        }
        boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(forEachStatementNode);
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
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(lockStatementNode)) {
            generateStatementBlock(lockStatementNode);
        }
        boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(lockStatementNode);
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
        if (ModelGeneratorUtils.isStatementBlockCommentPresent(doStatementNode)) {
            generateStatementBlock(doStatementNode);
        }
        boolean isHidden = ModelGeneratorUtils.isHiddenInSequenceFlagPresent(doStatementNode);
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
            if (doStatement.getChildElements() != null || (doStatement.getOnFailStatement() != null && doStatement.getOnFailStatement().getChildElements() != null)) {
                this.visitorContext.getDiagramElementWithChildren().addChildDiagramElements(doStatement);
            }
        } else if (doStatement.getChildElements() != null || (doStatement.getOnFailStatement() != null && doStatement.getOnFailStatement().getChildElements() != null)) {
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
                            StatementBlock statementBlock = new StatementBlock();
                            statementBlock.setLocation(minutiae.parentToken().location().lineRange()); // TODO : Fix the correct location
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

    private boolean isNodeVisited(NameReferenceNode functionReferenceNode) {
        if (semanticModel.symbol(functionReferenceNode).isPresent() && semanticModel.symbol(functionReferenceNode).get().getModule().isPresent()) {
            String functionName = "";
            if (functionReferenceNode instanceof QualifiedNameReferenceNode) {
                functionName = ((QualifiedNameReferenceNode) functionReferenceNode).identifier().text().trim();
            } else if (functionReferenceNode instanceof SimpleNameReferenceNode) {
                functionName = ((SimpleNameReferenceNode) functionReferenceNode).name().text().trim();
            }
            String nameNodeModuleID = semanticModel.symbol(functionReferenceNode).get().getModule().get().id().toString().trim().replace(":", "_") + "_" + functionName;


            return this.visitorContext.getVisitedFunctionNames().contains(nameNodeModuleID);
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
