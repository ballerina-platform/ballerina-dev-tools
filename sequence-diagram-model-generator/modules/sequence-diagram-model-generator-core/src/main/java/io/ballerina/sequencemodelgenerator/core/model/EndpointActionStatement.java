package io.ballerina.sequencemodelgenerator.core.model;

public class EndpointActionStatement extends Interaction{
    private String actionName;
    private String actionPath;
    private String methodName;

    public EndpointActionStatement(String sourceId, String targetId, String actionName,String methodName, String actionPath, boolean isHiddenInSequenceDiagram) {
        super(sourceId, targetId, "EndpointInteraction", isHiddenInSequenceDiagram);
        this.actionName = actionName;
        this.methodName = methodName;
        this.actionPath = actionPath;
    }
}
