package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class ForEachStatement extends DElement {
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
