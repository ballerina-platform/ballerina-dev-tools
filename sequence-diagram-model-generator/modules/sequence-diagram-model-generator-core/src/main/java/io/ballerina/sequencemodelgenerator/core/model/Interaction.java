package io.ballerina.sequencemodelgenerator.core.model;

public class Interaction {
    private String sourceId;
    private String targetId;

    public Interaction(String sourceId, String targetId) {
        this.sourceId = sourceId;
        this.targetId = targetId;
    }
}
