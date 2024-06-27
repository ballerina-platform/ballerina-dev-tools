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
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

import java.util.Map;

/**
 * Represents the properties of a while node in the flow model.
 *
 * @since 1.4.0
 */
public class WhileNode extends FlowNode {

    public static final String WHILE_LABEL = "While";
    public static final String WHILE_CONDITION = "Condition";
    public static final String WHILE_CONDITION_KEY = "condition";
    private static final String WHILE_CONDITION_DOC = "Boolean Condition";

    protected WhileNode(Map<String, Expression> nodeProperties) {
        super(WHILE_LABEL, Kind.WHILE, false, nodeProperties);
    }

    @Override
    public String toSource() {
        return null;
    }

    public static class Builder extends FlowNode.NodePropertiesBuilder {

        private Expression condition;

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        public Builder setConditionExpression(ExpressionNode expressionNode) {
            semanticModel.typeOf(expressionNode).ifPresent(expressionBuilder::type);
            this.condition = expressionBuilder
                    .label(WHILE_CONDITION)
                    .value(expressionNode.toSourceCode())
                    .typeKind(Expression.ExpressionTypeKind.BTYPE)
                    .documentation(WHILE_CONDITION_DOC)
                    .editable()
                    .build();
            return this;
        }

        @Override
        public FlowNode build() {
            addProperty(WHILE_CONDITION_KEY, this.condition);
            return new WhileNode(nodeProperties);
        }
    }
}
