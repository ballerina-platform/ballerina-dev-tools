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
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

/**
 * Represents the properties of a while node in the flow model.
 *
 * @since 1.4.0
 */
public class While extends NodeBuilder {

    public static final String LABEL = "While";
    public static final String DESCRIPTION = "Loop over a block of code.";
    private static final String WHILE_CONDITION_DOC = "Boolean Condition";

    @Override
    public void setConcreteConstData() {
        this.label = LABEL;
        this.kind = FlowNode.Kind.WHILE;
        this.description = DESCRIPTION;
    }

    @Override
    public String toSource(FlowNode node) {
        SourceBuilder sourceBuilder = new SourceBuilder();
        Property condition = node.getProperty(Property.CONDITION_KEY);
        Branch body = node.getBranch(Branch.BODY_LABEL);

        sourceBuilder
                .keyword(SyntaxKind.WHILE_KEYWORD)
                .expression(condition)
                .openBrace();

        if (body != null) {
            sourceBuilder.addChildren(body.children());
        }
        sourceBuilder.closeBrace();

        // Handle the on fail branch
        Branch onFailBranch = node.getBranch(Branch.ON_FAIL_LABEL);
        if (onFailBranch != null) {
            // Build the keywords
            sourceBuilder
                    .keyword(SyntaxKind.ON_KEYWORD)
                    .keyword(SyntaxKind.FAIL_KEYWORD);

            // Build the parameters
            Property variableProperty = onFailBranch.getProperty(NodeBuilder.PropertiesBuilder.VARIABLE_KEY);
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

    @Override
    public void setConcreteTemplateData() {
//        this.properties =
//                Map.of(Property.CONDITION_KEY, Property.getDefaultConditionExpression(WHILE_CONDITION_DOC));
    }
}
