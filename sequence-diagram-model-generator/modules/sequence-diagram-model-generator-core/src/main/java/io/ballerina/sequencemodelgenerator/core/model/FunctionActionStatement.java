package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class FunctionActionStatement extends Interaction {

    private String functionName;


    public FunctionActionStatement(String sourceId, String targetId, String functionName, boolean isHiddenInSequenceDiagram, LineRange location) {
        super(sourceId, targetId, "FunctionInteraction", isHiddenInSequenceDiagram, location);
        this.functionName = functionName;
    }

}
