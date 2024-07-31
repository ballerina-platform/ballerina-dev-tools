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
 * Represents the properties of foreach node in the flow model.
 *
 * @since 1.4.0
 */
public class Foreach extends NodeBuilder {

    public static final String LABEL = "Foreach";
    public static final String DESCRIPTION = "Iterate over a block of code.";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(FlowNode.Kind.FOREACH);
    }

    @Override
    public String toSource(FlowNode node) {
        SourceBuilder sourceBuilder = new SourceBuilder(node);
        sourceBuilder.token().keyword(SyntaxKind.FOREACH_KEYWORD)
                .stepOut()
                .typedBindingPattern()
                .token().keyword(SyntaxKind.IN_KEYWORD);


        Optional<Property> exprProperty = node.getProperty(Property.COLLECTION_KEY);
        exprProperty.ifPresent(property -> sourceBuilder.token().expression(property));

        Optional<Branch> body = node.getBranch(Branch.BODY_LABEL);
        body.ifPresent(branch -> sourceBuilder.body(branch.children()));

        return sourceBuilder
                .onFailure()
                .build(false);
    }

    @Override
    public void setConcreteTemplateData(Codedata codedata) {
        properties().defaultDataVariable().defaultCollection();
        this.branches = List.of(Branch.DEFAULT_BODY_BRANCH, Branch.DEFAULT_ON_FAIL_BRANCH);
    }
}
