package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.List;

public class IfStatement extends DiagramElementWithChildren {
    private String condition;
    private DiagramElementWithChildren elseStatement; // this can be either else or elseiF

    public IfStatement(String condition, boolean isHidden, LineRange location) {
        super("IfStatement", isHidden, location);
        this.condition = condition;
    }

    public DiagramElementWithChildren getElseStatement() {
        return elseStatement;
    }

    public void setElseStatement(DiagramElementWithChildren elseStatement) {
        this.elseStatement = elseStatement;
    }
}
