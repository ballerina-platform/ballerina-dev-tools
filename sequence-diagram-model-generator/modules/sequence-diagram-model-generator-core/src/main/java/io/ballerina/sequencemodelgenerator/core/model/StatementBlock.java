package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class StatementBlock extends DElement {

    private String statementBlockText;

    public void setStatementBlockText(String statementBlockText) {
        this.statementBlockText = statementBlockText;
    }

    public StatementBlock(LineRange location) {
        super("StatementBlock", false, location);
    }

}
