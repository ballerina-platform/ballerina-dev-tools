package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;

public class ForEachStatement extends DiagramElementWithChildren{
    private String condition;

    public ForEachStatement(String condition, boolean isHidden) {
        super("ForEachStatement", isHidden);
        this.condition = condition;
    }
}
