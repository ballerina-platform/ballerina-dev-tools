/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.tools.text.LineRange;

import java.util.Map;

/**
 * Represents an interaction in the sequence diagram.
 *
 * @since 2.0.0
 */
public class Interaction extends SequenceNode {

    public static final String PARAMS_LABEL = "params";
    public static final String NAME_LABEL = "name";
    public static final String VALUE_LABEL = "value";
    public static final String EXPRESSION_LABEL = "expr";
    public static final String RESOURCE_PATH = "resourcePath";

    private final InteractionType interactionType;
    private final String targetId;

    public Interaction(Map<String, Object> properties, LineRange location, InteractionType interactionType,
                       String targetId) {
        super(NodeKind.INTERACTION, null, properties, location);
        this.interactionType = interactionType;
        this.targetId = targetId;
    }

    public InteractionType interactionType() {
        return interactionType;
    }

    public String targetId() {
        return targetId;
    }

    public enum InteractionType {
        ENDPOINT_CALL,
        FUNCTION_CALL,
        METHOD_CALL,
        WORKER_CALL
    }

    /**
     * Represents the builder for the {@link Interaction}. Provides the extended methods for building a
     * {@link Interaction} node.
     *
     * @since 2.0.0
     */
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
