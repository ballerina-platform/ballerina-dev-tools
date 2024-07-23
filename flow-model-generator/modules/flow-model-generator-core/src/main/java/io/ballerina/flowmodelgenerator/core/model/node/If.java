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
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

import java.util.List;
import java.util.Map;

/**
 * Represents the properties of an if node in the flow model.
 *
 * @since 1.4.0
 */
public class If extends FlowNode {

    public static final String LABEL = "If";
    public static final String DESCRIPTION = "Add conditional branch to the integration flow.";
    public static final String IF_THEN_LABEL = "Then";
    public static final String IF_ELSE_LABEL = "Else";
    private static final String IF_CONDITION_DOC = "Boolean Condition";

    @Override
    public void setConstData() {
        this.label = LABEL;
        this.description = DESCRIPTION;
        this.kind = Kind.IF;
    }

    @Override
    public String toSource() {
        SourceBuilder sourceBuilder = new SourceBuilder();
        Property condition = getProperty(Property.CONDITION_KEY);

        sourceBuilder
                .keyword(SyntaxKind.IF_KEYWORD)
                .expression(condition)
                .openBrace();

        Branch ifBranch = getBranch(IF_THEN_LABEL);
        sourceBuilder.addChildren(ifBranch.children());

        Branch elseBranch = getBranch(IF_ELSE_LABEL);
        if (elseBranch != null) {
            List<FlowNode> children = elseBranch.children();
            sourceBuilder
                    .closeBrace()
                    .whiteSpace()
                    .keyword(SyntaxKind.ELSE_KEYWORD);

            // If there is only one child, and if that is an if node, generate an `else if` statement`
            if (children.size() != 1 || children.get(0).kind() != Kind.IF) {
                sourceBuilder.openBrace();
            }
            sourceBuilder.addChildren(children);
        }

        sourceBuilder.closeBrace();
        return sourceBuilder.build(false);
    }

    @Override
    public void setConcreteTemplateData() {
        this.nodeProperties =
                Map.of(Property.CONDITION_KEY, Property.getDefaultConditionExpression(IF_CONDITION_DOC));
        this.branches = List.of(Branch.getEmptyBranch(IF_THEN_LABEL), Branch.getEmptyBranch(IF_ELSE_LABEL));
    }
}
