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
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

import java.util.Map;

/**
 * Represents the properties of a panic node.
 *
 * @since 1.4.0
 */
public class PanicNode extends FlowNode {
    public static final String PANIC_LABEL = "Panic";
    private static final String PANIC_EXPRESSION = "Expression";
    private static final String PANIC_EXPRESSION_KEY = "expression";
    private static final String PANIC_EXPRESSION_DOC = "Panic value";

    protected PanicNode(Map<String, Expression> nodeProperties) {
        super(PANIC_LABEL, FlowNode.Kind.PANIC, false, nodeProperties);
    }

    public static final PanicNode DEFAULT_NODE = new PanicNode(Map.of(
            PANIC_EXPRESSION_KEY,
            Expression.Builder.getInstance()
                    .label(PANIC_EXPRESSION)
                    .value("")
                    .documentation(PANIC_EXPRESSION_DOC)
                    .typeKind(Expression.ExpressionTypeKind.BTYPE)
                    .editable()
                    .build()
    ));

    @Override
    public String toSource() {
        SourceBuilder sourceBuilder = new SourceBuilder();

        sourceBuilder.keyword(SyntaxKind.PANIC_KEYWORD);
        Expression expression = getProperty(PANIC_EXPRESSION_KEY);
        if (expression != null) {
            sourceBuilder
                    .whiteSpace()
                    .expression(expression);
        }
        sourceBuilder.endOfStatement();
        return sourceBuilder.build(false);
    }

    /**
     * Represents the builder for panic node properties.
     *
     * @since 1.4.0
     */
    public static class Builder extends FlowNode.NodePropertiesBuilder {

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        public Builder setExpressionNode(ExpressionNode expressionNode) {
            semanticModel.typeOf(expressionNode).ifPresent(expressionBuilder::type);
            this.expression = expressionBuilder
                    .label(PANIC_EXPRESSION)
                    .value(expressionNode.toSourceCode())
                    .documentation(PANIC_EXPRESSION_DOC)
                    .typeKind(Expression.ExpressionTypeKind.BTYPE)
                    .editable()
                    .build();
            return this;
        }

        @Override
        public FlowNode build() {
            nodeProperties.put(PANIC_EXPRESSION_KEY, expression);
            return new PanicNode(nodeProperties);
        }
    }
}
