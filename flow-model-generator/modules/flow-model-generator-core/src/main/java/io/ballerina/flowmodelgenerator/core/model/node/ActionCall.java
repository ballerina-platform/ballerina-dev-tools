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

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.ExpressionAttributes;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeAttributes;
import io.ballerina.tools.text.LineRange;

import java.util.List;
import java.util.Map;

/**
 * Represents the generalized action invocation node in the flow model.
 *
 * @since 1.4.0
 */
public class ActionCall extends FlowNode {

    @Override
    protected void setConstData() {
    }

    @Override
    public String toSource() {
        SourceBuilder sourceBuilder = new SourceBuilder();

        Expression variable = getProperty(PropertiesBuilder.VARIABLE_KEY);
        if (variable != null) {
            sourceBuilder
                    .expressionWithType(variable)
                    .keyword(SyntaxKind.EQUAL_TOKEN);
        }

        if (returning()) {
            sourceBuilder.keyword(SyntaxKind.RETURN_KEYWORD);
        }

        if (hasFlag(NODE_FLAG_CHECKED)) {
            sourceBuilder.keyword(SyntaxKind.CHECK_KEYWORD);
        }

        NodeAttributes.Info info = NodeAttributes.get(kind());
        Expression client = getProperty(info.callExpression().key());

        sourceBuilder.expression(client)
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(info.key())
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN);

        List<ExpressionAttributes.Info> parameterExpressions = info.parameterExpressions();

        if (!parameterExpressions.isEmpty()) {
            Expression firstParameter = getProperty(parameterExpressions.get(0).key());
            if (firstParameter != null) {
                sourceBuilder.expression(firstParameter);
            }

            boolean hasEmptyParam = false;
            for (int i = 1; i < parameterExpressions.size(); i++) {
                String parameterKey = parameterExpressions.get(i).key();
                Expression parameter = getProperty(parameterKey);

                if (parameter == null || parameter.value() == null) {
                    hasEmptyParam = true;
                    continue;
                }

                sourceBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                if (hasEmptyParam) {
                    sourceBuilder
                            .name(parameterKey)
                            .keyword(SyntaxKind.EQUAL_TOKEN);
                    hasEmptyParam = false;
                }
                sourceBuilder.expression(parameter);
            }
        }

        sourceBuilder
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();

        return sourceBuilder.build(false);
    }
}
