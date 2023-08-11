package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;

public class ForEachStatement extends StatementWithBody{
    private String condition;

    public ForEachStatement(List<Statement> statements, String condition) {
        super("FOR", statements);
        this.condition = condition;
    }
}
