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
 * Represents the properties of a start node in the flow model.
 *
 * @since 1.4.0
 */
public class Start extends FlowNode {
    public static final String START_LABEL = "Start";
    private static final String START_EXPRESSION = "Expression";
    private static final String START_EXPRESSION_KEY = "expression";
    public static final String START_EXPRESSION_DOC = "Call action or expression";

    public static final FlowNode DEFAULT_NODE = new Return("0", START_LABEL, Kind.START, false,
            Map.of(START_EXPRESSION_KEY,
                    Expression.Builder.getInstance()
                            .label(START_EXPRESSION)
                            .value("")
                            .documentation(START_EXPRESSION_DOC)
                            .typeKind(Expression.ExpressionTypeKind.BTYPE)
                            .editable()
                            .build()
            ), null, false, List.of(), 0);

    public Start(String id, String label, Kind kind, boolean fixed, Map<String, Expression> nodeProperties,
                 LineRange lineRange, boolean returning, List<Branch> branches, int flags) {
        super(id, label, kind, fixed, nodeProperties, lineRange, returning, branches, flags);
    }

    @Override
    public String toSource() {
        SourceBuilder sourceBuilder = new SourceBuilder();

        sourceBuilder.keyword(SyntaxKind.START_KEYWORD);
        Expression expression = getProperty(START_EXPRESSION_KEY);
        if (expression != null) {
            sourceBuilder
                    .whiteSpace()
                    .expression(expression);
        }
        sourceBuilder.endOfStatement();
        return sourceBuilder.build(false);
    }
}
