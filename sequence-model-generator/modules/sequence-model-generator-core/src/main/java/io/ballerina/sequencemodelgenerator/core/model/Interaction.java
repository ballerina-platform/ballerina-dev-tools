package io.ballerina.sequencemodelgenerator.core.model;

public class Interaction extends SequenceNode {
    private InteractionType interactionType;
    private String targetId;

    public enum InteractionType {
        ENDPOINT_CALL,
        FUNCTION_CALL,
        METHOD_CALL,
        WORKER_CALL,
        RETURN_CALL
    }
}
