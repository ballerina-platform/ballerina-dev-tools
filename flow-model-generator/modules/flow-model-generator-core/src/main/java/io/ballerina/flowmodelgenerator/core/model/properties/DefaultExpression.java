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

package io.ballerina.flowmodelgenerator.core.model.properties;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.model.Expression;

/**
 * Represents the properties of a default expression node.
 *
 * @param variable   The variable of the default expression node
 * @param expression The expression of the default expression node
 */
public record DefaultExpression(Expression variable, Expression expression) implements NodeProperties {

    public final static String EXPRESSION_LABEL = "Custom Expression";
    public final static String EXPRESSION_RHS_LABEL = "Expression";
    public final static String EXPRESSION_RHS_DOC = "Expression";

    /**
     * Represents the builder for default expression node properties.
     *
     * @since 2201.9.0
     */
    public static class Builder extends NodePropertiesBuilder {

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        public NodeProperties build() {
            return new DefaultExpression(this.variable, this.expression);
        }
    }

}
