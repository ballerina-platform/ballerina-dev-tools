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
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Represents the properties of an if node in the flow model.
 *
 * @since 1.4.0
 */
public class If extends NodeBuilder {

    public static final String LABEL = "If";
    public static final String DESCRIPTION = "Add conditional branch to the integration flow.";
    public static final String IF_THEN_LABEL = "Then";
    public static final String IF_ELSE_LABEL = "Else";
    private static final String IF_CONDITION_DOC = "Boolean Condition";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(FlowNode.Kind.IF);
    }

    @Override
    public String toSource(FlowNode flowNode) {
        SourceBuilder sourceBuilder = new SourceBuilder(flowNode);

        Optional<Branch> ifBranch = flowNode.getBranch(IF_THEN_LABEL);
        if (ifBranch.isEmpty()) {
            throw new IllegalStateException("If node does not have a then branch");
        }
        Optional<Property> condition = ifBranch.get().getProperty(Property.CONDITION_KEY);
        if (condition.isEmpty()) {
            throw new IllegalStateException("If node does not have a condition");
        }
        sourceBuilder.token().keyword(SyntaxKind.IF_KEYWORD)
                .expression(condition.get())
                .openBrace()
                .addChildren(ifBranch.get().children());

        Optional<Branch> elseBranch = flowNode.getBranch(IF_ELSE_LABEL);
        if (elseBranch.isPresent()) {
            List<FlowNode> children = elseBranch.get().children();
            sourceBuilder.token()
                    .closeBrace()
                    .whiteSpace()
                    .keyword(SyntaxKind.ELSE_KEYWORD);

            // If there is only one child, and if that is an if node, generate an `else if` statement`
            if (children.size() != 1 || children.get(0).codedata().node() != FlowNode.Kind.IF) {
                sourceBuilder.token().openBrace();
            }
            sourceBuilder.token().addChildren(children);
        }

        sourceBuilder.token().closeBrace();
        return sourceBuilder.build(false);
    }

    @Override
    public void setConcreteTemplateData(Codedata codedata) {
        Branch.Builder thenBranchBuilder = new Branch.Builder()
                .label(IF_THEN_LABEL)
                .kind(Branch.BranchKind.BLOCK)
                .repeatable(Branch.Repeatable.ONE_OR_MORE)
                .codedata().node(FlowNode.Kind.CONDITIONAL).stepOut();
        thenBranchBuilder.properties().defaultCondition(IF_CONDITION_DOC);

        this.branches = List.of(thenBranchBuilder.build(), Branch.getEmptyBranch(IF_ELSE_LABEL, FlowNode.Kind.ELSE));
    }
}
