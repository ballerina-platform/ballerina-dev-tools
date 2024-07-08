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
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.tools.text.LineRange;

import java.util.List;
import java.util.Map;

/**
 * Represents the properties of a default expression node.
 *
 * @since 1.4.0
 */
public class DefaultExpression extends FlowNode {

    public static final String EXPRESSION_LABEL = "Custom Expression";

    public static final FlowNode DEFAULT_NODE =
            new DefaultExpression(DEFAULT_ID, EXPRESSION_LABEL, Kind.EXPRESSION, false,
                    Map.of(), null, false, List.of(), 0);

    public DefaultExpression(String id, String label, Kind kind, boolean fixed,
                             Map<String, Expression> nodeProperties,
                             LineRange lineRange, boolean returning,
                             List<Branch> branches, int flags) {
        super(id, label, kind, fixed, nodeProperties, lineRange, returning, branches, flags);
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
}
