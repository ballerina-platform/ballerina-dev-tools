package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import java.util.List;

public class ForEachStatement extends DiagramElementWithChildren{
    private String condition;
    private OnFailStatement onFailStatement;

    public ForEachStatement(String condition, boolean isHidden, LineRange location) {
        super("ForEachStatement", isHidden, location);
        this.condition = condition;
    }

    public OnFailStatement getOnFailStatement() {
        return onFailStatement;
    }

    public void setOnFailStatement(OnFailStatement onFailStatement) {
        this.onFailStatement = onFailStatement;
    }

}
