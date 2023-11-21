package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;
import io.ballerina.sequencemodelgenerator.core.model.Constants.ActionType;
import io.ballerina.sequencemodelgenerator.core.model.Constants.InteractionType;

public class EndpointActionStatement extends Interaction {
    private final String actionName;
    private final String actionPath;
    private final String methodName;

    private final ActionType actionType;

    // TODO: add the query params

    public EndpointActionStatement(String sourceId, String targetId, String actionName, String methodName,
                                   String actionPath, boolean isHiddenInSequenceDiagram,
                                   ActionType actionType, LineRange location) {
        super(sourceId, targetId, InteractionType.ENDPOINT_INTERACTION, isHiddenInSequenceDiagram, location);
        this.actionName = actionName;
        this.methodName = methodName;
        this.actionPath = actionPath;
        this.actionType = actionType;
    }

}
