package io.ballerina.sequencemodelgenerator.core.model;

import java.util.ArrayList;
import java.util.List;

public class IfStatement extends StatementWithBody {
    private String condition;
    private ElseStatement elseStatement;
    private List<ElseIfStatement> elseIfStatements;

    public IfStatement(List<Statement> statements, String condition) {
        super("IF", statements);
        this.condition = condition;
    }

    public void setElseStatement(ElseStatement elseStatement) {
        this.elseStatement = elseStatement;
    }

    public void setElseIfStatements(List<ElseIfStatement> elseIfStatements) {
        this.elseIfStatements = elseIfStatements;
    }

    public void appendToElseIfStatement(ElseIfStatement elseIfStatement) {
        if (this.elseIfStatements == null) {
            this.elseIfStatements = new ArrayList<>();
            this.elseIfStatements.add(elseIfStatement);
        } else {
            this.elseIfStatements.add(elseIfStatement);
        }
    }
}
