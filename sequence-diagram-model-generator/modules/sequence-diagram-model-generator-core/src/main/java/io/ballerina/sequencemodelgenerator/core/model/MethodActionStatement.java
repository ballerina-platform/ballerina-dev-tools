package io.ballerina.sequencemodelgenerator.core.model;

public class MethodActionStatement extends Interaction {

    private String methodName;
    private String expression;

public MethodActionStatement(String sourceId, String targetId, String methodName, String expression, boolean isHiddenInSequenceDiagram) {
        super(sourceId, targetId, "MethodInteraction", isHiddenInSequenceDiagram);
        this.methodName = methodName;
        this.expression = expression;
    }

}
