package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;

public class WhileStatement extends DiagramElementWithChildren{
    private final String condition;
    private OnFailStatement onFailStatement;

    private boolean hasInteractions;

    public WhileStatement(String condition, boolean isHidden) {
        super("WhileStatement", isHidden);
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

    public void setHasInteractions(boolean hasInteractions) {
        this.hasInteractions = hasInteractions;
    }
}
