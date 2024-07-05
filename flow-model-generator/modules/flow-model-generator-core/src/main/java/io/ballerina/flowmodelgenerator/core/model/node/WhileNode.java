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
 * Represents the properties of a while node in the flow model.
 *
 * @since 1.4.0
 */
public class WhileNode extends FlowNode {

    public static final String WHILE_LABEL = "While";
    public static final String WHILE_CONDITION = "Condition";
    public static final String WHILE_CONDITION_KEY = "condition";
    private static final String WHILE_CONDITION_DOC = "Boolean Condition";
    private static final Expression DEFAULT_CONDITION = Expression.Builder.getInstance()
            .label(WHILE_CONDITION)
            .value("")
            .documentation(WHILE_CONDITION_DOC)
            .typeKind(Expression.ExpressionTypeKind.BTYPE)
            .editable()
            .build();

    public WhileNode(String id, String label, Kind kind, boolean fixed, Map<String, Expression> nodeProperties,
                     LineRange lineRange, boolean returning, List<Branch> branches, int flags) {
        super(id, label, kind, fixed, nodeProperties, lineRange, returning, branches, flags);
    }

    public static final FlowNode DEFAULT_NODE = new WhileNode("0", WHILE_LABEL, Kind.WHILE, false,
            Map.of(WHILE_CONDITION_KEY, DEFAULT_CONDITION), null, false,
            List.of(new Branch(Branch.BODY_LABEL, Branch.BranchKind.BLOCK, List.of(), null)), 0);

    @Override
    public String toSource() {
        SourceBuilder sourceBuilder = new SourceBuilder();
        Expression condition = getProperty(WHILE_CONDITION_KEY);
        Branch body = getBranch(Branch.BODY_LABEL);

        sourceBuilder
                .keyword(SyntaxKind.WHILE_KEYWORD)
                .expression(condition)
                .openBrace()
                .addChildren(body.children())
                .closeBrace();

        // Handle the on fail branch
        Branch onFailBranch = getBranch(Branch.ON_FAIL_LABEL);
        if (onFailBranch != null) {
            // Build the keywords
            sourceBuilder
                    .keyword(SyntaxKind.ON_KEYWORD)
                    .keyword(SyntaxKind.FAIL_KEYWORD);

            // Build the parameters
            Expression variableProperty = getBranchProperty(onFailBranch, PropertiesBuilder.VARIABLE_KEY);
            if (variableProperty != null) {
                sourceBuilder.expressionWithType(variableProperty);
            }

            // Build the body
            sourceBuilder.openBrace()
                    .addChildren(onFailBranch.children())
                    .closeBrace();
        }

        return sourceBuilder.build(false);
    }
}
