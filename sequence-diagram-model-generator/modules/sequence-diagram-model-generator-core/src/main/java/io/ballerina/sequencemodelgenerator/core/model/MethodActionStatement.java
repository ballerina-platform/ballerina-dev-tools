package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class MethodActionStatement extends Interaction {

    private String methodName;
    private String expression;

public MethodActionStatement(String sourceId, String targetId, String methodName, String expression, boolean isHiddenInSequenceDiagram, LineRange location) {
        super(sourceId, targetId, "MethodInteraction", isHiddenInSequenceDiagram, location);
        this.methodName = methodName;
        this.expression = expression;
    }

}
