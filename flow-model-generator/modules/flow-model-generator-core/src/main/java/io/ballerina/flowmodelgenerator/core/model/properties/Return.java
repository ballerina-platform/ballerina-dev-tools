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
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.flowmodelgenerator.core.model.Expression;

/**
 * Represents the properties of a return node.
 *
 * @param expr The expression of the return node
 * @since 2201.9.0
 */
public record Return(Expression expr) implements NodeProperties {

    public static final String RETURN_KEY = "Return";
    private static final String RETURN_EXPRESSION = "Expression";
    private static final String RETURN_EXPRESSION_DOC = "Return value";

    /**
     * Represents the builder for return node properties.
     *
     * @since 2201.9.0
     */
    public static class Builder extends NodePropertiesBuilder {

        private Expression expr;

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        public void setExpression(ExpressionNode expressionNode) {
            expressionBuilder.key(RETURN_EXPRESSION);
            expressionBuilder.value(expressionNode.toSourceCode());
            expressionBuilder.setDocumentation(RETURN_EXPRESSION_DOC);
            expressionBuilder.typeKind(Expression.ExpressionTypeKind.BTYPE);
            expressionBuilder.setEditable();
            semanticModel.typeOf(expressionNode).ifPresent(expressionBuilder::type);
            expr = expressionBuilder.build();
        }

        @Override
        public NodeProperties build() {
            return new Return(expr);
        }
    }
}
