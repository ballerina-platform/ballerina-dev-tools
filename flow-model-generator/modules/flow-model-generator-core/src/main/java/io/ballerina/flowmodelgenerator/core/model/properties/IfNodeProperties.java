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
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

import java.util.Optional;

/**
 * Represents the properties of an if node in the flow model.
 *
 * @param condition  condition of the if node
 * @param thenBranch then branch of the if node
 * @param elseBranch else branch of the if node
 * @since 2201.9.0
 */
public record IfNodeProperties(Expression condition, FlowNode thenBranch, FlowNode elseBranch)
        implements NodeProperties {

    /**
     * Represents a builder for the if node properties.
     *
     * @since 2201.9.0
     */
    public static class Builder extends NodePropertiesBuilder {

        private Expression condition;
        private FlowNode thenBranch;
        private FlowNode elseBranch;

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        public void setConditionExpression(ExpressionNode expressionNode) {
            expressionBuilder.key("condition");
            expressionBuilder.value(expressionNode.toSourceCode());
            expressionBuilder.typeKind(Expression.ExpressionTypeKind.BTYPE);

            Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(expressionNode);
            typeSymbol.ifPresent(expressionBuilder::type);

            this.condition = expressionBuilder.build();
        }

        public void setThenBranchNode(FlowNode flowNode) {
            this.thenBranch = flowNode;
        }

        public void setElseBranchNode(FlowNode flowNode) {
            this.elseBranch = flowNode;
        }

        @Override
        public NodeProperties build() {
            return new IfNodeProperties(condition, thenBranch, elseBranch);
        }
    }
}
