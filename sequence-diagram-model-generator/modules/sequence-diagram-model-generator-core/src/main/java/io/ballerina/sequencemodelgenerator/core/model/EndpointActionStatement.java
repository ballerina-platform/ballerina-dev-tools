package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.sequencemodelgenerator.core.model.Constants.ActionType;
import io.ballerina.sequencemodelgenerator.core.model.Constants.InteractionType;
import io.ballerina.tools.text.LineRange;

/**
 * Represents the statement with connector interactions.
 * example :  json response = check self.httpEp->/users/[name];
 *
 * @since 2201.8.0
 */
public class EndpointActionStatement extends Interaction {
    private final String actionName;
    private final String actionPath;
    private final String methodName;
    private final ActionType actionType;

    public EndpointActionStatement(String sourceId, String targetId, String actionName, String methodName,
                                   String actionPath, boolean isHiddenInSequenceDiagram,
                                   ActionType actionType, LineRange location) {
        super(sourceId, targetId, InteractionType.ENDPOINT_INTERACTION, isHiddenInSequenceDiagram, location);
        this.actionName = actionName;
        this.methodName = methodName;
        this.actionPath = actionPath;
        this.actionType = actionType;
    }

    public String getActionName() {
        return actionName;
    }

    public String getActionPath() {
        return actionPath;
    }

    public String getMethodName() {
        return methodName;
    }

    public ActionType getActionType() {
        return actionType;
    }
}
