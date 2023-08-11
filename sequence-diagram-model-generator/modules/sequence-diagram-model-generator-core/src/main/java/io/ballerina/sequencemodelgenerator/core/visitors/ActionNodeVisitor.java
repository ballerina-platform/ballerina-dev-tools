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
import static io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils.getRawType;
import static io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils.getQualifiedNameRefNodeFuncNameText;

import java.util.*;

public class ActionNodeVisitor extends NodeVisitor {
    private final SemanticModel semanticModel;
    private final String workerId;
    private final Package currentPackage;
    private List<Participant> participants;
//    private List<Interaction> interactions;
    private Set<NameReferenceNode> visitedFunctionNames = new HashSet<>();
    private final String modulePrefix =null;



   private StatementWithBody currentStatement;


    public void setCurrentStatement(StatementWithBody currentStatement) {
        this.currentStatement = currentStatement;
    }

    public StatementWithBody getCurrentStatement() {
        return currentStatement;
    }

    public ActionNodeVisitor(SemanticModel semanticModel, String workerId, Package currentPackage, List<Participant> participants) {
        this.semanticModel = semanticModel;
        this.workerId = workerId;
        this.currentPackage = currentPackage;
        this.participants = participants;
//        this.interactions = interactions;

    }

    public Set<NameReferenceNode> getVisitedFunctionNames() {
        return visitedFunctionNames;
    }

    public void setVisitedFunctionNames(Set<NameReferenceNode> visitedFunctionNames) {
        this.visitedFunctionNames = visitedFunctionNames;
    }


    public ActionNodeVisitor(SemanticModel semanticModel, String workerId, Package currentPackage, List<Participant> participants, Set<NameReferenceNode> visitedFunctionNames, StatementWithBody currentStatement) {

        this.semanticModel = semanticModel;
        this.workerId = workerId;
        this.currentPackage = currentPackage;
        this.participants = participants;
//        this.interactions = interactions;
        this.currentStatement = currentStatement;


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
//                    Interaction interaction = new Interaction(this.workerId, clientID);
                    ActionStatement actionStatement = new ActionStatement(this.workerId, clientID, null, null);
                    Participant currentParticipant = ModelGeneratorUtils.getParticipantByID(this.workerId, participants);
//                    currentParticipant.getStatements().add(actionStatement);
                    if (currentParticipant != null) {
                        currentParticipant.addStatement(actionStatement);
                    }
//                    interactions.add(interaction);
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

                            if (this.currentStatement != null) {
                                try {
                                    SubParticipantNodeVisitor subParticipantNodeVisitor = new SubParticipantNodeVisitor(workerId, semanticModel, currentPackage, participants, visitedFunctionNames, this.currentStatement);
                                    node.accept(subParticipantNodeVisitor);
                                    visitedFunctionNames.addAll(subParticipantNodeVisitor.getVisitedFunctionNames());
                                }catch (Exception e){
                                    System.out.println("Error is visiting sub participant for currentStatement");
                                }

                            } else {
                                try {
                                    SubParticipantNodeVisitor subParticipantNodeVisitor = new SubParticipantNodeVisitor(workerId, semanticModel, currentPackage, participants, visitedFunctionNames);
                                    node.accept(subParticipantNodeVisitor);
                                    visitedFunctionNames.addAll(subParticipantNodeVisitor.getVisitedFunctionNames());
                                } catch (Exception e) {
                                    System.out.println("Error is visiting sub participant for no current stmt");
                                }

                            }



                        }
                    }
                }
            });
        }
    }


//    @Override
//    public void visit(IfElseStatementNode ifElseStatementNode) {
////        ifElseStatementNode.ifBody().accept(this);
//        // statements:
//        // condition: true
//        // statements: [action1, action2]
//        // ifElseStatement[]: [ifElseStatement1, ifElseStatement2]
//        // elseStatement: [action3, action4]
//
//        if (this.currentStatement == null) {
//            IfStatement ifStatement = new IfStatement(null, ifElseStatementNode.condition().toString());
//           setCurrentStatement(ifStatement);
//            ConditionalNodeVisitor conditionalNodeVisitor = new ConditionalNodeVisitor("IF",ifElseStatementNode.condition().toString(),
//                    this.workerId, semanticModel, currentPackage, participants, visitedFunctionNames, ifStatement);
//            ifElseStatementNode.ifBody().accept(conditionalNodeVisitor);
//
//        } else {
//            ConditionalNodeVisitor conditionalNodeVisitor = new ConditionalNodeVisitor("IF",ifElseStatementNode.condition().toString(),
//                    this.workerId, semanticModel, currentPackage, participants, visitedFunctionNames, this.currentStatement);
//            ifElseStatementNode.ifBody().accept(conditionalNodeVisitor);
//
//        }
//
//
//        Participant currentParticipant = ModelGeneratorUtils.getParticipantByID(this.workerId, participants);
//        System.out.println("currentParticipant = " + currentParticipant.toString());
////        System.out.println("currentStatement = " + currentStatement.toString());
//        if (currentParticipant != null) {
//            currentParticipant.addStatement(currentStatement);
//        }
//
//    }
//

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
//        if (this.currentStatement != null && this.currentStatement.getKind() == "ElseIfStatement") {
//            ElseIfStatement elseIfStatement = new ElseIfStatement(ifElseStatementNode.condition().toString(), null);
//            ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, this.workerId, currentPackage, participants, visitedFunctionNames, elseIfStatement);
//            ifElseStatementNode.ifBody().accept(actionNodeVisitor);
//            if (this.currentStatement != null) {
//                if (this.currentStatement instanceof ElseIfStatement) {
//                    ((ElseIfStatement) this.currentStatement).addToConditionalStatements(elseIfStatement);
//                }
//            }
//            if (ifElseStatementNode.elseBody().isPresent()) {
//                ActionNodeVisitor actionNodeVisitor2 = new ActionNodeVisitor(semanticModel, this.workerId, currentPackage, participants, visitedFunctionNames, elseIfStatement);
//                ifElseStatementNode.elseBody().get().accept(actionNodeVisitor2);
//
//            }
//
//        } else {
            IfStatement ifStatement = new IfStatement(null, ifElseStatementNode.condition().toString());
            ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, this.workerId, currentPackage, participants, visitedFunctionNames, ifStatement);
            ifElseStatementNode.ifBody().accept(actionNodeVisitor);
            if (this.currentStatement != null) {
                this.currentStatement.addToConditionalStatements(ifStatement);
            } else {
                Participant currentParticipant = ModelGeneratorUtils.getParticipantByID(this.workerId, participants);
                if (currentParticipant != null) {
                    currentParticipant.addStatement(ifStatement);
                }
            }


            if (ifElseStatementNode.elseBody().isPresent()) {
                ActionNodeVisitor actionNodeVisitor2 = new ActionNodeVisitor(semanticModel, this.workerId, currentPackage, participants, visitedFunctionNames, ifStatement);
                ifElseStatementNode.elseBody().get().accept(actionNodeVisitor2);

            }
//        }




//        if (this.parentStatement == null) {
//            try {
//                System.out.println("====");
//                IfStatement ifStatement = new IfStatement(null, ifElseStatementNode.condition().toString());
////                setParentStatement(ifStatement);
//                ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, this.workerId, currentPackage, participants, visitedFunctionNames, ifStatement, ifStatement);
//                ifElseStatementNode.ifBody().accept(actionNodeVisitor);
//                Participant currentParticipant = ModelGeneratorUtils.getParticipantByID(this.workerId, participants);
//                if (currentParticipant != null) {
//                    currentParticipant.addStatement(ifStatement);
//                }
//
//            } catch (Exception e) {
//                System.out.println("Error is visiting action visitor for new current" + e);
//            }
//
//
//        } else {
//            try {
//                ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, this.workerId, currentPackage, participants, visitedFunctionNames, this.currentStatement, this.parentStatement);
//                ifElseStatementNode.ifBody().accept(actionNodeVisitor);
//                this.parentStatement.addToConditionalStatements(this.currentStatement);
//                System.out.println("currentStatement = " + currentStatement.toString());
//                System.out.println("currentStatement2 = " + actionNodeVisitor.currentStatement);
//            } catch (Exception e) {
//                System.out.println("Error is visiting action visitor for old current");
//            }
//
//        }

    }



    @Override
    public void visit(ElseBlockNode elseBlockNode) {
        System.out.println("===");


        elseBlockNode.children().iterator().forEachRemaining(node -> {
            if (node instanceof IfElseStatementNode) {
                IfElseStatementNode ifElseStatementNode = (IfElseStatementNode) node;
                ElseIfStatement elseIfStatement = new ElseIfStatement(ifElseStatementNode.condition().toString(), null);
                ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, this.workerId, currentPackage, participants, visitedFunctionNames, elseIfStatement);
                ifElseStatementNode.ifBody().accept(actionNodeVisitor);
                System.out.println("elseIfStatement = " + actionNodeVisitor.currentStatement);
                if (this.currentStatement != null) {
                    if (this.currentStatement instanceof IfStatement) {
                        ((IfStatement) this.currentStatement).appendToElseIfStatement(elseIfStatement);
                    }
                }
            }
        });

        if (!elseBlockNode.elseKeyword().isMissing()) {
            ElseStatement elseStatement = new ElseStatement(null);
            ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, this.workerId, currentPackage, participants, visitedFunctionNames, elseStatement);
            elseBlockNode.elseBody().children().iterator().forEachRemaining(node -> {
                if (node instanceof ElseBlockNode) {
                    ((ElseBlockNode) node).elseBody().accept(actionNodeVisitor);
                    System.out.println("elseStatement = " + actionNodeVisitor.currentStatement);
                }
            });

            if (this.currentStatement != null) {
                if (this.currentStatement instanceof IfStatement) {
                    ((IfStatement) this.currentStatement).setElseStatement(elseStatement);
                }
            }

        }




    }


    @Override
    public void visit(WhileStatementNode whileStatementNode) {
        WhileStatement whileStatement = new WhileStatement(null,whileStatementNode.condition().toString());
        ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, this.workerId, currentPackage, participants, visitedFunctionNames, whileStatement);
        whileStatementNode.whileBody().accept(actionNodeVisitor);
        if (this.currentStatement != null) {
            this.currentStatement.addToConditionalStatements(whileStatement);
        } else {
            Participant currentParticipant = ModelGeneratorUtils.getParticipantByID(this.workerId, participants);
            if (currentParticipant != null) {
                currentParticipant.addStatement(whileStatement);
            }
        }
    }


    @Override
    public void visit(ForEachStatementNode forEachStatementNode) {
        ForEachStatement forEachStatement = new ForEachStatement(null,forEachStatementNode.actionOrExpressionNode().toString());
        ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, this.workerId, currentPackage, participants, visitedFunctionNames, forEachStatement);
        forEachStatementNode.blockStatement().accept(actionNodeVisitor);
        if (this.currentStatement != null) {
            this.currentStatement.addToConditionalStatements(forEachStatement);
        } else {
            Participant currentParticipant = ModelGeneratorUtils.getParticipantByID(this.workerId, participants);
            if (currentParticipant != null) {
                currentParticipant.addStatement(forEachStatement);
            }
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
