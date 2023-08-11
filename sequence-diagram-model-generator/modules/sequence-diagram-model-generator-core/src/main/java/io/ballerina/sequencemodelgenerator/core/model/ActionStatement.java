package io.ballerina.sequencemodelgenerator.core.model;

public class ActionStatement extends Statement{
    private String sourceId;
    private String targetId;

    private String actionName;
    private String varName;

    public ActionStatement(String sourceId, String targetId, String actionName, String varName) {
        super("ActionStatement");
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.actionName = actionName;
        this.varName = varName;
    }
}
