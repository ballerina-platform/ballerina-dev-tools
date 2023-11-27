package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.sequencemodelgenerator.core.model.Constants.InteractionType;
import io.ballerina.tools.text.LineRange;

/**
 * Represents the MethodCall interactions.
 *
 * @since 2201.8.0
 */
public class MethodActionStatement extends Interaction {

    private final String methodName;
    private final String expression;

    public MethodActionStatement(String sourceId, String targetId, String methodName, String expression,
                                 boolean isHiddenInSequenceDiagram, LineRange location) {
        super(sourceId, targetId, InteractionType.METHOD_INTERACTION, isHiddenInSequenceDiagram, location);
        this.methodName = methodName;
        this.expression = expression;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getExpression() {
        return expression;
    }
}
