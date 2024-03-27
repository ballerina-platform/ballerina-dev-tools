package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.tools.text.LineRange;

import java.util.Map;

public class Interaction extends SequenceNode {

    public static final String PARAMS_LABEL = "params";
    public static final String NAME_LABEL = "name";
    public static final String VALUE_LABEL = "value";
    public static final String EXPRESSION_LABEL = "expr";

    private InteractionType interactionType;
    private String targetId;

    public Interaction(Map<String, Object> properties, LineRange location, InteractionType interactionType,
                       String targetId) {
        super(NodeKind.INTERACTION, null, properties, location);
        this.interactionType = interactionType;
        this.targetId = targetId;
    }

    public enum InteractionType {
        ENDPOINT_CALL,
        FUNCTION_CALL,
        METHOD_CALL,
        WORKER_CALL
    }

    public static class Builder extends SequenceNode.Builder {

        private InteractionType interactionType;
        private String targetId;

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        public Builder interactionType(InteractionType interactionType) {
            this.interactionType = interactionType;
            return this;
        }

        public Builder targetId(String targetId) {
            this.targetId = targetId;
            return this;
        }

        public Interaction build() {
            return new Interaction(properties, location, interactionType, targetId);
        }
    }
}
