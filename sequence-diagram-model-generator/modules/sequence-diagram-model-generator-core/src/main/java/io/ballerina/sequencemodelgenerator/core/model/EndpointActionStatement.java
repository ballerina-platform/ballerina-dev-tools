package io.ballerina.sequencemodelgenerator.core.model;


import io.ballerina.tools.text.LineRange;

public class EndpointActionStatement extends Interaction{
    private String actionName;
    private String actionPath;
    private String methodName;

    private Constants.ActionType actionType;

    // TODO: add the query params

    public EndpointActionStatement(String sourceId, String targetId, String actionName, String methodName,
                                   String actionPath, boolean isHiddenInSequenceDiagram,
                                   Constants.ActionType actionType, LineRange location) {
        super(sourceId, targetId, "EndpointInteraction", isHiddenInSequenceDiagram, location);
        this.actionName = actionName;
        this.methodName = methodName;
        this.actionPath = actionPath;
        this.actionType = actionType;
    }
}
