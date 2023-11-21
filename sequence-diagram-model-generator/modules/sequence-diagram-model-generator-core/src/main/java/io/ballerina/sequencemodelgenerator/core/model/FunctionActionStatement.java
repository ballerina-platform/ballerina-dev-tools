package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;
import io.ballerina.sequencemodelgenerator.core.model.Constants.InteractionType;

public class FunctionActionStatement extends Interaction {
    private final String functionName;

    public FunctionActionStatement(String sourceId, String targetId, String functionName,
                                   boolean isHiddenInSequenceDiagram, LineRange location) {
        super(sourceId, targetId, InteractionType.FUNCTION_INTERACTION, isHiddenInSequenceDiagram, location);
        this.functionName = functionName;
    }
}
