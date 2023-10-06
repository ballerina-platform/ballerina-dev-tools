package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class Interaction extends DiagramElement {
    private final String sourceId;
    private final String targetId;

    private final String interactionType;

    public Interaction(String sourceId, String targetId, String interactionType,boolean isHidden, LineRange location) {
        super("Interaction", isHidden, location);
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.interactionType = interactionType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getInteractionType() {
        return interactionType;
    }

}
