package io.ballerina.sequencemodelgenerator.core.model;

public class Interaction extends DiagramElement {
    private String sourceId;
    private String targetId;

    public Interaction(String sourceId, String targetId) {
        super("Interaction");
        this.sourceId = sourceId;
        this.targetId = targetId;
    }
}
