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
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

import java.util.Map;

/**
 * Represents the properties of an error handler node in the flow model.
 *
 * @since 1.4.0
 */
public class ErrorHandlerNode extends FlowNode {

    public static final String ERROR_HANDLER_LABEL = "ErrorHandler";
    public static final String ERROR_HANDLER_BODY = "Body";

    protected ErrorHandlerNode(Map<String, Expression> nodeProperties) {
        super(ERROR_HANDLER_LABEL, Kind.ERROR_HANDLER, false, nodeProperties);
    }

    @Override
    public String toSource() {
        return null;
    }

    /**
     * Represents the builder for error handler node properties.
     *
     * @since 1.4.0
     */
    public static class Builder extends FlowNode.NodePropertiesBuilder {

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        @Override
        public FlowNode build() {
            return new ErrorHandlerNode(nodeProperties);
        }
    }
}