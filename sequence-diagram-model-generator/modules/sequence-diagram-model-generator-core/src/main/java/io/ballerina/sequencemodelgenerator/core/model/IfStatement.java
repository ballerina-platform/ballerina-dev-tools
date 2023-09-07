package io.ballerina.sequencemodelgenerator.core.model;

import java.util.ArrayList;
import java.util.List;

public class IfStatement extends DiagramElementWithChildren {
    private String condition;
    private DiagramElementWithChildren elseStatement; // this can be either else or elseiF
//    private List<ElseIfStatement> elseIfStatements;

    public IfStatement(String condition, boolean isHidden) {
        super("IfStatement", isHidden);
        this.condition = condition;
    }

    public DiagramElementWithChildren getElseStatement() {
        return elseStatement;
    }

    public void setElseStatement(DiagramElementWithChildren elseStatement) {
        this.elseStatement = elseStatement;
    }

//    public void setElseStatement(ElseStatement elseStatement) {
//        this.elseStatement = elseStatement;
//    }




//    public void setElseIfStatements(List<ElseIfStatement> elseIfStatements) {
//        this.elseIfStatements = elseIfStatements;
//    }
//
//    public void appendToElseIfStatement(StatementWithBody elseStatement) {
//        if (this.elseStatement == null) {
//            this.elseStatement = new ArrayList<>();
//            this.elseStatement.add(elseStatement);
//        } else {
//            this.elseIfStatements.add(elseIfStatement);
//        }
//    }
}
