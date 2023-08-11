package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;

public class WhileStatement extends StatementWithBody{
    private String condition;

    public WhileStatement(List<Statement> statements, String condition) {
        super("WHILE", statements);
        this.condition = condition;
    }
}
