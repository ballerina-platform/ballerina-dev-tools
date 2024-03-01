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
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

import java.util.Map;

/**
 * Represents the properties of a default expression node.
 */
public class DefaultExpression extends FlowNode {

    public final static String EXPRESSION_LABEL = "Custom Expression";

    protected DefaultExpression(Map<String, Expression> nodeProperties) {
        super(EXPRESSION_LABEL, Kind.EXPRESSION, false, nodeProperties);
    }

    @Override
    public String toSource() {
        Expression variable = getProperty(FlowNode.NodePropertiesBuilder.VARIABLE_KEY);
        Expression expression = getProperty(FlowNode.NodePropertiesBuilder.EXPRESSION_RHS_KEY);

        SourceBuilder sourceBuilder = new SourceBuilder();
        sourceBuilder
                .expressionWithType(variable)
                .whiteSpace()
                .keyword(SyntaxKind.EQUAL_TOKEN)
                .whiteSpace()
                .expression(expression)
                .endOfStatement();
        return sourceBuilder.build(false);
    }

    /**
     * Represents the builder for default expression node properties.
     *
     * @since 2201.9.0
     */
    public static class Builder extends FlowNode.NodePropertiesBuilder {

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        @Override
        public FlowNode build() {
            addProperty(FlowNode.NodePropertiesBuilder.VARIABLE_KEY, this.variable);
            addProperty(FlowNode.NodePropertiesBuilder.EXPRESSION_RHS_KEY, this.expression);
            return new DefaultExpression(nodeProperties);
        }
    }

}
