package io.ballerina.sequencemodelgenerator.core.model;

public class FunctionActionStatement extends Interaction {

    private String functionName;


    public FunctionActionStatement(String sourceId, String targetId, String functionName, boolean isHiddenInSequenceDiagram) {
        super(sourceId, targetId, "FunctionInteraction", isHiddenInSequenceDiagram);
        this.functionName = functionName;
    }

}
