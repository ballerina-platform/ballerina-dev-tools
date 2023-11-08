package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class WhileStatement extends DElement {
    private final String condition;
    private OnFailStatement onFailStatement;

    public WhileStatement(String condition, boolean isHidden, LineRange location) {
        super("WhileStatement", isHidden, location);
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }

    public OnFailStatement getOnFailStatement() {
        return onFailStatement;
    }

    public void setOnFailStatement(OnFailStatement onFailStatement) {
        this.onFailStatement = onFailStatement;
    }
}
