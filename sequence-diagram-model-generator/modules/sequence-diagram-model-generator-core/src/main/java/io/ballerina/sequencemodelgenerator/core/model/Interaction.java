package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.sequencemodelgenerator.core.model.Constants.InteractionType;
import io.ballerina.tools.text.LineRange;

import static io.ballerina.sequencemodelgenerator.core.model.Constants.INTERACTION;

/**
 * Represents base interaction model.
 *
 * @since 2201.8.0
 */
public class Interaction extends DNode {
    private final String sourceId;
    private final String targetId;
    private final InteractionType interactionType;

    public Interaction(String sourceId, String targetId, InteractionType interactionType, boolean isHidden,
                       LineRange location) {
        super(INTERACTION, isHidden, location);
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

    public InteractionType getInteractionType() {
        return interactionType;
    }

}
