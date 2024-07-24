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

import java.util.List;
import java.util.Optional;

/**
 * Represents the properties of an error handler node in the flow model.
 *
 * @since 1.4.0
 */
public class ErrorHandler extends NodeBuilder {

    public static final String LABEL = "ErrorHandler";
    public static final String DESCRIPTION = "Catch and handle errors";
    public static final String ERROR_HANDLER_BODY = "Body";

    @Override
    public void setConcreteConstData() {
        this.label = LABEL;
        this.description = DESCRIPTION;
        codedata().node(FlowNode.Kind.ERROR_HANDLER);
    }

    @Override
    public String toSource(FlowNode node) {
        SourceBuilder sourceBuilder = new SourceBuilder();
        Optional<Branch> body = node.getBranch(ERROR_HANDLER_BODY);

        sourceBuilder
                .keyword(SyntaxKind.DO_KEYWORD)
                .openBrace();
        body.ifPresent(branch -> sourceBuilder.addChildren(branch.children()));
        sourceBuilder.closeBrace();

        Optional<Branch> onFailBranch = node.getBranch(Branch.ON_FAIL_LABEL);
        if (onFailBranch.isPresent()) {
            // Build the keywords
            sourceBuilder
                    .keyword(SyntaxKind.ON_KEYWORD)
                    .keyword(SyntaxKind.FAIL_KEYWORD);

            // Build the parameters
            Optional<Property> variableProperty = onFailBranch.get().getProperty(PropertiesBuilder.VARIABLE_KEY);
            variableProperty.ifPresent(sourceBuilder::expressionWithType);

            // Build the body
            sourceBuilder.openBrace()
                    .addChildren(onFailBranch.get().children())
                    .closeBrace();
        }

        return sourceBuilder.build(false);
    }

    @Override
    public void setConcreteTemplateData() {
        this.branches = List.of(Branch.DEFAULT_BODY_BRANCH, Branch.DEFAULT_ON_FAIL_BRANCH);
    }
}
