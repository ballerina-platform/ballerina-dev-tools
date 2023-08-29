package io.ballerina.sequencemodelgenerator.core.model;

public class FunctionActionStatement extends Interaction {

    private String functionName;


    public FunctionActionStatement(String sourceId, String targetId, String functionName) {
        super(sourceId, targetId, "FunctionInteraction");
        this.functionName = functionName;
    }

}
