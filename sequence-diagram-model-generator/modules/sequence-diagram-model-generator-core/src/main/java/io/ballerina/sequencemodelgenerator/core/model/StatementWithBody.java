//package io.ballerina.sequencemodelgenerator.core.model;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class StatementWithBody extends Statement{
//    private List<Statement> statements;
//
//    public StatementWithBody(String kind, List<Statement> statements) {
//        super(kind);
//        this.statements = statements;
//    }
//
//    public List<Statement> getStatements() {
//        return statements;
//    }
//
//    public void addToConditionalStatements(Statement statement) {
//        if (this.statements == null) {
//            this.statements = new ArrayList<>();
//            this.statements.add(statement);
//        } else {
//            this.statements.add(statement);
//        }
//    }
//
//    @Override
//    public String toString() {
//        return "StatementWithBody{" +
//                "statements=" + statements +
//                '}';
//    }
//}
