package io.ballerina.sequencemodelgenerator.core.model;

public class Interaction extends DiagramElement {
    private String sourceId;
    private String targetId;

    private String interactionType;

    public Interaction(String sourceId, String targetId, String interactionType) {
        super("Interaction");
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.interactionType = interactionType;
    }
}
