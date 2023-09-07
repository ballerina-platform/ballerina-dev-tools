package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class StatementBlock extends DiagramElementWithChildren{
    private LineRange location;

    private String statementBlockText;

    public void setStatementBlockText(String statementBlockText) {
        this.statementBlockText = statementBlockText;
    }

    public StatementBlock() {
        super("StatementBlock", false);
    }

    public void setLocation(LineRange location) {
        this.location = location;
    }
}
