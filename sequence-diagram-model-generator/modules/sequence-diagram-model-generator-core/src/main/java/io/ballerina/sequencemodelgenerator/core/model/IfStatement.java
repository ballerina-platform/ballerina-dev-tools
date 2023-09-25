package io.ballerina.sequencemodelgenerator.core.model;

import java.util.ArrayList;
import java.util.List;

public class IfStatement extends DiagramElementWithChildren {
    private String condition;
    private DiagramElementWithChildren elseStatement; // this can be either else or elseiF
    private boolean hasInteractions;

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

    public void setHasInteractions(boolean hasInteractions) {
        this.hasInteractions = hasInteractions;
    }
}
