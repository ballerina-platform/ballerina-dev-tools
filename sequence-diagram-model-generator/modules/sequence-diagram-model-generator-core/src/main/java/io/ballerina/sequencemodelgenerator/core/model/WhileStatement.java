package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;

public class WhileStatement extends DiagramElementWithChildren{
    private String condition;

    public WhileStatement(String condition) {
        super("WhileStatement");
        this.condition = condition;
    }
}
