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
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.Optional;

/**
 * Represents the properties of a default expression node.
 *
 * @since 1.4.0
 */
public class DefaultExpression extends NodeBuilder {

    public static final String LABEL = "Custom Expression";
    public static final String DESCRIPTION = "Represents a custom Ballerina expression";

    public static final String STATEMENT_KEY = "statement";
    public static final String STATEMENT_LABEL = "Statement";
    public static final String STATEMENT_DOC = "Ballerina statement";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(FlowNode.Kind.EXPRESSION);
    }

    @Override
    public String toSource(FlowNode node) {
        SourceBuilder sourceBuilder = new SourceBuilder();
        Optional<Property> variable = node.getProperty(Property.VARIABLE_KEY);
        Optional<Property> expression = node.getProperty(Property.EXPRESSION_KEY);

        if (variable.isPresent() && expression.isPresent()) {
            sourceBuilder.token()
                    .expressionWithType(variable.get())
                    .whiteSpace()
                    .keyword(SyntaxKind.EQUAL_TOKEN)
                    .whiteSpace()
                    .expression(expression.get())
                    .endOfStatement();
            return sourceBuilder.build(false);
        }

        Optional<Property> statement = node.getProperty(STATEMENT_KEY);
        if (statement.isEmpty()) {
            throw new IllegalStateException(
                    "One of from the following properties is required: variable, expression, statement");
        }
        sourceBuilder.token()
                .expression(statement.get())
                .endOfStatement();
        return sourceBuilder.build(false);
    }

    @Override
    public void setConcreteTemplateData(Codedata codedata) {
        properties().statement(null);
    }
}
