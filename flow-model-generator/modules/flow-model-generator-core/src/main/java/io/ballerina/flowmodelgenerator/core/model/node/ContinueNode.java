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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

/**
 * Represents the properties of a continue node.
 *
 * @since 1.4.0
 */
public class ContinueNode extends FlowNode {

    public static final String CONTINUE_LABEL = "Continue";

    protected ContinueNode() {
        super(CONTINUE_LABEL, Kind.CONTINUE, false, null);
    }

    @Override
    public String toSource() {
        return null;
    }

    /**
     * Represents the builder for continue node properties.
     *
     * @since 1.4.0
     */
    public static class Builder extends FlowNode.NodePropertiesBuilder {

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        @Override
        public FlowNode build() {
            return new ContinueNode();
        }
    }

}
