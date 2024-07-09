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
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

/**
 * Represents the properties of a default expression node.
 *
 * @since 1.4.0
 */
public class DefaultExpression extends FlowNode {

    public static final String LABEL = "Custom Expression";
    public static final String DESCRIPTION = "Represents a custom Ballerina expression";

    @Override
    public void setConstData() {
        this.label = LABEL;
        this.kind = Kind.EXPRESSION;
        this.description = DESCRIPTION;
    }

    @Override
    public String toSource() {
        Expression variable = getProperty(FlowNode.PropertiesBuilder.VARIABLE_KEY);
        Expression expression = getProperty(FlowNode.PropertiesBuilder.EXPRESSION_RHS_KEY);

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

    @Override
    public void setTemplateData() {

    }
}
