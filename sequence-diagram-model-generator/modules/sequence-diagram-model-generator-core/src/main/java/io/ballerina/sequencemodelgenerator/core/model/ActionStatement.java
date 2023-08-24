package io.ballerina.sequencemodelgenerator.core.model;

public class ActionStatement extends Interaction {

    private String actionName;


    public ActionStatement(String sourceId, String targetId, String actionName) {
        super(sourceId, targetId);
        this.actionName = actionName;
    }
}
