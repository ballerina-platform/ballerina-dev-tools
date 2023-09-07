package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;

public class WhileStatement extends DiagramElementWithChildren{
    private final String condition;

    public WhileStatement(String condition, boolean isHidden) {
        super("WhileStatement", isHidden);
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }
}
