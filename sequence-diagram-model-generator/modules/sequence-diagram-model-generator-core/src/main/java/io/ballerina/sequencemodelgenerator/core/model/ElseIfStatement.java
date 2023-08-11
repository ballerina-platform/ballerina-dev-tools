package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;

public class ElseIfStatement extends StatementWithBody{
    private String condition;

    public ElseIfStatement(String condition, List<Statement> statements) {
        super("ElseIfStatement", statements);
        this.condition = condition;
    }
}
