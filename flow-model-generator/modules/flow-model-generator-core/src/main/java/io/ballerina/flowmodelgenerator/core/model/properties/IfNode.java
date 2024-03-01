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

import java.util.Map;
import java.util.Optional;

/**
 * Represents the properties of an if node in the flow model.
 *
 * @since 2201.9.0
 */
public class IfNode extends FlowNode {

    public static final String IF_LABEL = "If";
    private static final String IF_CONDITION = "Condition";
    public static final String IF_CONDITION_KEY = "condition";
    private static final String IF_CONDITION_DOC = "Boolean Condition";

    protected IfNode(Map<String, Expression> nodeProperties) {
        super(IF_LABEL, Kind.IF, false, nodeProperties);
    }

    @Override
    public String toSource() {
        return null;
    }

    /**
     * Represents a builder for the if node properties.
     *
     * @since 2201.9.0
     */
    public static class Builder extends FlowNode.NodePropertiesBuilder {

        private Expression condition;

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        public void setConditionExpression(ExpressionNode expressionNode) {
            expressionBuilder.key(IF_CONDITION);
            expressionBuilder.value(expressionNode.toSourceCode());
            expressionBuilder.typeKind(Expression.ExpressionTypeKind.BTYPE);
            expressionBuilder.setDocumentation(IF_CONDITION_DOC);
            expressionBuilder.setEditable();

            Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(expressionNode);
            typeSymbol.ifPresent(expressionBuilder::type);

            this.condition = expressionBuilder.build();
        }

        @Override
        public FlowNode build() {
            addProperty(IF_CONDITION_KEY, this.condition);
            return new IfNode(nodeProperties);
        }
    }
}
