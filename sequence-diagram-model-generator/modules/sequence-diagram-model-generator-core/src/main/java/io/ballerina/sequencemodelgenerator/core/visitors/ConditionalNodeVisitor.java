//package io.ballerina.sequencemodelgenerator.core.visitors;
//
//import io.ballerina.compiler.api.ModuleID;
//import io.ballerina.compiler.api.SemanticModel;
//import io.ballerina.compiler.api.symbols.*;
//import io.ballerina.compiler.syntax.tree.*;
//import io.ballerina.projects.DocumentId;
//import io.ballerina.projects.Package;
//import io.ballerina.sequencemodelgenerator.core.model.*;
//import io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils;
//import io.ballerina.tools.diagnostics.Location;
//
//import java.util.*;
//
//import static io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils.getQualifiedNameRefNodeFuncNameText;
//import static io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils.getRawType;
//
//public class ConditionalNodeVisitor extends NodeVisitor {
//    private String statementType;
//    private String condition;
//
//    private final String sourceId;
//    private final SemanticModel semanticModel;
//    private final Package currentPackage;
//    private List<Participant> participants;
//
//    private Set<NameReferenceNode> visitedFunctionNames;
//
//    private String currentParticipant;
//
//    private StatementWithBody currentStatement;
//
//    public StatementWithBody getCurrentStatement() {
//        return currentStatement;
//    }
//
//    public void setCurrentStatement(StatementWithBody currentStatement) {
//        this.currentStatement = currentStatement;
//    }
//
//    private final String modulePrefix =null;
//
//    public ConditionalNodeVisitor(String statementType, String condition, String sourceId, SemanticModel semanticModel,
//                                  Package currentPackage, List<Participant> participants, Set<NameReferenceNode> visitedFunctionNames, StatementWithBody currentStatement) {
//        this.statementType = statementType;
//        this.condition = condition;
//        this.sourceId = sourceId;
//        this.semanticModel = semanticModel;
//        this.currentPackage = currentPackage;
//        this.participants = participants;
//        this.visitedFunctionNames = visitedFunctionNames;
//        this.currentStatement = currentStatement;
//    }
//
//
//
//    @Override
//    public void visit(ClientResourceAccessActionNode clientResourceAccessActionNode) {
//        NameReferenceNode clientNode = null;
//
//        String resourceMethod = null;
//        String resourcePath = null;
//        String serviceId = null;
//        String serviceLabel = null;
//
//        try {
//            if (clientResourceAccessActionNode.expression().kind().equals(SyntaxKind.FIELD_ACCESS)) {
//                NameReferenceNode fieldName = ((FieldAccessExpressionNode)
//                        clientResourceAccessActionNode.expression()).fieldName();
//                if (fieldName.kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE)) {
//                    clientNode = fieldName;
//                }
//            } else if (clientResourceAccessActionNode.expression().kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE)) {
//                clientNode = (SimpleNameReferenceNode) clientResourceAccessActionNode.expression();
//            } else if (clientResourceAccessActionNode.expression().kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
//                clientNode = (QualifiedNameReferenceNode) clientResourceAccessActionNode.expression();
//            }
//
//
//        } catch (Exception e) {
//            // Diagnostic message is logged in the visit() method
//        }
//
//        Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(clientNode);
//
//        if (typeSymbol.isPresent()) {
//            TypeSymbol rawType = getRawType(typeSymbol.get());
//            if (rawType.typeKind() == TypeDescKind.OBJECT) {
//                ObjectTypeSymbol objectTypeSymbol = (ObjectTypeSymbol) rawType;
//                boolean isEndpoint = objectTypeSymbol.qualifiers()
//                        .contains(Qualifier.CLIENT);
//                if (isEndpoint) {
//                    String clientID = UUID.randomUUID().toString();
//                    Participant participant = new Participant(clientID, clientNode.toString(),
//                            ParticipantKind.ENDPOINT,objectTypeSymbol.getModule().get().id().toString(),objectTypeSymbol.signature());
//                    participants.add(participant);
//
//                    if (this.statementType == "IF") {
//                        ActionStatement actionStatement = new ActionStatement(this.sourceId, clientID, null, null);
//                        IfStatement ifStatement = new IfStatement(null, this.condition);
//                        setCurrentStatement(ifStatement);
//                        ifStatement.addToConditionalStatements(actionStatement);
//
//                        Participant currentParticipant = ModelGeneratorUtils.getParticipantByID(this.sourceId, participants);
//                        if (currentParticipant != null) {
//                            currentParticipant.addStatement(ifStatement);
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//
//    @Override
//    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
//        if ((functionCallExpressionNode.functionName() instanceof SimpleNameReferenceNode ||
//                functionCallExpressionNode.functionName() instanceof QualifiedNameReferenceNode) &&
//                !isNodeVisited(functionCallExpressionNode.functionName())) {
//
//            // this is for the visitedFunction definitions based on functionCalls
//            visitedFunctionNames.add(functionCallExpressionNode.functionName());
//            Optional<Symbol> symbol = semanticModel.symbol(functionCallExpressionNode.functionName());
//            symbol.ifPresent(value -> findInteractions(functionCallExpressionNode.functionName(), value));
//
//        }
//        // todo : Other combinations
//    }
//
//
//    private void findInteractions(NameReferenceNode nameNode, Symbol methodSymbol) {
//
//        Optional<Location> location = methodSymbol.getLocation();
//        Optional<ModuleSymbol> optionalModuleSymbol = methodSymbol.getModule();
//        if (optionalModuleSymbol.isPresent()) {
//            ModuleID moduleID = optionalModuleSymbol.get().id();
//            // get ST of the selected methodSymbol
//            currentPackage.modules().forEach(module -> {
//                if (Objects.equals(moduleID.moduleName(), module.moduleName().toString())) {
//                    Collection<DocumentId> documentIds = module.documentIds();
//                    for (DocumentId documentId : documentIds) {
//                        if (module.document(documentId).syntaxTree().filePath().equals(location.get().lineRange().fileName())) {
//                            SyntaxTree syntaxTree = module.document(documentId).syntaxTree();
//                            NonTerminalNode node = ((ModulePartNode) syntaxTree.rootNode())
//                                    .findNode(location.get().textRange());
//
//
//
//                                if (this.currentStatement != null) {
////                                    this.currentStatement.addToConditionalStatements(this.currentStatement);
//                                    ConditionalSubParticipantNodeVisitor subParticipantNodeVisitor = new ConditionalSubParticipantNodeVisitor("IF","condition",
//                                            this.sourceId, semanticModel, currentPackage, participants, visitedFunctionNames, currentParticipant, this.currentStatement);
//                                    node.accept(subParticipantNodeVisitor);
//
//                                    visitedFunctionNames.addAll(subParticipantNodeVisitor.getVisitedFunctionNames());
//                                } else {
////                                    IfStatement ifStatement = new IfStatement( null, this.condition);
////                                    setCurrentStatement(ifStatement);
////                                    ConditionalSubParticipantNodeVisitor subParticipantNodeVisitor = new ConditionalSubParticipantNodeVisitor("IF","condition",
////                                            this.sourceId, semanticModel, currentPackage, participants, visitedFunctionNames, currentParticipant, this.currentStatement);
////
////                                    node.accept(subParticipantNodeVisitor);
////                                    visitedFunctionNames.addAll(subParticipantNodeVisitor.getVisitedFunctionNames());
//                                }
//
//
//
//
//
//                        }
//                    }
//                }
//            });
//        }
//    }
//
//
//    @Override
//    public void visit(IfElseStatementNode ifElseStatementNode) {
////    ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, this.sourceId, currentPackage, participants, visitedFunctionNames, this.currentStatement);
//        IfStatement ifStatement = new IfStatement(null, ifElseStatementNode.condition().toString());
//        ConditionalNodeVisitor conditionalNodeVisitor = new ConditionalNodeVisitor("IF", ifElseStatementNode.condition().toString(), this.sourceId, semanticModel, currentPackage, participants, visitedFunctionNames, ifStatement);
////        ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, this.sourceId, currentPackage, participants, visitedFunctionNames, ifStatement);
////        ifElseStatementNode.accept(actionNodeVisitor);
//        ifElseStatementNode.ifBody().accept(conditionalNodeVisitor);
////        System.out.println("currentSTMT: " + this.currentStatement);
////        System.out.println("currentSTMT-OF-VISITOR: " + conditionalNodeVisitor.getCurrentStatement());
//        this.currentStatement.addToConditionalStatements(conditionalNodeVisitor.getCurrentStatement());
//
//    }
//
//
//
//    private boolean isNodeVisited(NameReferenceNode functionName) {
//        return visitedFunctionNames.stream().anyMatch(nameNode -> {
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
//}
