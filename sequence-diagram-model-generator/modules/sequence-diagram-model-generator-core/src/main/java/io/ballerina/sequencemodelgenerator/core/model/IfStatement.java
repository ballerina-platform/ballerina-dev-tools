package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class IfStatement extends DElement {
    private String condition;
    private DElement elseStatement; // this can be either else or elseiF

    public IfStatement(String condition, boolean isHidden, LineRange location) {
        super("IfStatement", isHidden, location);
        this.condition = condition;
    }

    public DElement getElseStatement() {
        return elseStatement;
    }

    public void setElseStatement(DElement elseStatement) {
        this.elseStatement = elseStatement;
    }
}
