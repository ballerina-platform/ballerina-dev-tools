package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;

public class ForEachStatement extends DiagramElementWithChildren{
    private String condition;
    private OnFailStatement onFailStatement;

    public ForEachStatement(String condition, boolean isHidden) {
        super("ForEachStatement", isHidden);
        this.condition = condition;
    }

    public OnFailStatement getOnFailStatement() {
        return onFailStatement;
    }

    public void setOnFailStatement(OnFailStatement onFailStatement) {
        this.onFailStatement = onFailStatement;
    }
}
