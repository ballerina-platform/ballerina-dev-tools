//package io.ballerina.sequencemodelgenerator.core.visitors;
//
//import io.ballerina.compiler.api.SemanticModel;
//import io.ballerina.compiler.api.symbols.Symbol;
//import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
//import io.ballerina.compiler.syntax.tree.NameReferenceNode;
//import io.ballerina.compiler.syntax.tree.NodeVisitor;
//import io.ballerina.projects.Package;
//import io.ballerina.sequencemodelgenerator.core.model.*;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//import java.util.UUID;
//
//public class ConditionalSubParticipantNodeVisitor extends NodeVisitor {
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
//    private final StatementWithBody currentStatement;
//
//
//    private final String modulePrefix =null;
//
//    public Set<NameReferenceNode> getVisitedFunctionNames() {
//        return visitedFunctionNames;
//    }
//
//    public void setCurrentParticipant(String currentParticipant) {
//        this.currentParticipant = currentParticipant;
//    }
//
//    public ConditionalSubParticipantNodeVisitor(String statementType, String condition, String sourceId, SemanticModel semanticModel, Package currentPackage, List<Participant> participants, Set<NameReferenceNode> visitedFunctionNames, String currentParticipant, StatementWithBody currentStatement) {
//        this.statementType = statementType;
//        this.condition = condition;
//        this.sourceId = sourceId;
//        this.semanticModel = semanticModel;
//        this.currentPackage = currentPackage;
//        this.participants = participants;
//        this.visitedFunctionNames = visitedFunctionNames;
//        this.currentParticipant = currentParticipant;
//        this.currentStatement = currentStatement;
//    }
//
//
//    @Override
//    public void visit(FunctionDefinitionNode functionDefinitionNode) {
//        Optional<Symbol> typeSymbol = semanticModel.symbol(functionDefinitionNode);
//        if (typeSymbol.isPresent()) {
//
//            String packageName = typeSymbol.get().getModule().get().id().packageName();
//
//            if (!isParticipantInList(functionDefinitionNode.functionName().text().trim(), packageName)) {
//                String functionID = UUID.randomUUID().toString();
//                Participant participant = new Participant(functionID,
//                        functionDefinitionNode.functionName().text().trim(), ParticipantKind.WORKER, packageName);
//                participants.add(participant);
//                setCurrentParticipant(functionID);
//
//                //trying to fix interaction order
//                ActionStatement actionStatement = new ActionStatement(this.sourceId, participant.getId(), null, null);
//                this.currentStatement.addToConditionalStatements(actionStatement);
//
//
//
//                ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(semanticModel, functionID, currentPackage, participants, visitedFunctionNames, currentStatement);
//                functionDefinitionNode.accept(actionNodeVisitor);
//            } else {
//                Participant participant = getParticipantInList(functionDefinitionNode.functionName().text().trim(), packageName);
//                setCurrentParticipant(participant.getId());
//                ActionStatement actionStatement = new ActionStatement(this.sourceId, participant.getId(), null, null);
//                this.currentStatement.addToConditionalStatements(actionStatement);
//            }
//
//        }
//    }
//
//
//    private boolean isParticipantInList(String name, String pkgName) {
//        for (Participant item : participants) {
//            if (item.getName().trim().equals(name.trim()) &&
//                    item.getPackageName().trim().equals(pkgName.trim())) {
//                return true; // Return true when the conditions are met
//            }
//        }
//        return false; // Return false if no match is found
//    }
//
//    private Participant getParticipantInList(String name, String pkgName) {
//        for (Participant item : participants) {
//            if (item.getName().trim().equals(name.trim()) &&
//                    item.getPackageName().trim().equals(pkgName.trim())) {
//                return item; // Return true when the conditions are met
//            }
//        }
//        return null; // Return false if no match is found
//    }
//}
