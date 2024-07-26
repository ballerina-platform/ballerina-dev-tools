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
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.ExpressionAttributes;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeAttributes;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Represents the generalized action invocation node in the flow model.
 *
 * @since 1.4.0
 */
public class ActionCall extends NodeBuilder {

    @Override
    public void setConcreteConstData() {
        codedata().node(FlowNode.Kind.ACTION_CALL);
    }

    @Override
    public String toSource(FlowNode node) {
        SourceBuilder sourceBuilder = new SourceBuilder();

        Optional<Property> variable = node.getProperty(PropertiesBuilder.VARIABLE_KEY);
        variable.ifPresent(property -> sourceBuilder
                .expressionWithType(property)
                .keyword(SyntaxKind.EQUAL_TOKEN));

        if (node.returning()) {
            sourceBuilder.keyword(SyntaxKind.RETURN_KEYWORD);
        }

        if (node.hasFlag(FlowNode.NODE_FLAG_CHECKED)) {
            sourceBuilder.keyword(SyntaxKind.CHECK_KEYWORD);
        }

        NodeAttributes.Info info = NodeAttributes.getByLabel(node.metadata().label());
        Optional<Property> client = node.getProperty(info.callExpression().key());

        if (client.isEmpty()) {
            throw new IllegalStateException("Client must be defined for an action call node");
        }
        sourceBuilder.expression(client.get())
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(info.method())
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN);

        List<ExpressionAttributes.Info> parameterExpressions = info.parameterExpressions();

        if (!parameterExpressions.isEmpty()) {
            Optional<Property> firstParameter = node.getProperty(parameterExpressions.get(0).key());
            firstParameter.ifPresent(sourceBuilder::expression);

            boolean hasEmptyParam = false;
            for (int i = 1; i < parameterExpressions.size(); i++) {
                String parameterKey = parameterExpressions.get(i).key();
                Optional<Property> parameter = node.getProperty(parameterKey);

                if (parameter.isEmpty() || parameter.get().value() == null) {
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
                sourceBuilder.expression(parameter.get());
            }
        }

        sourceBuilder
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();

        return sourceBuilder.build(false);
    }

    @Override
    public void setConcreteTemplateData(Codedata codedata) {
        NodeAttributes.Info info = NodeAttributes.getByKey(codedata.module(), codedata.symbol());
        metadata().label(info.label()).stepOut()
                .properties()
                .defaultExpression(info.callExpression())
                .defaultVariable();

        info.parameterExpressions()
                .forEach(expressionInfo -> properties().defaultExpression(expressionInfo));
    }
}
