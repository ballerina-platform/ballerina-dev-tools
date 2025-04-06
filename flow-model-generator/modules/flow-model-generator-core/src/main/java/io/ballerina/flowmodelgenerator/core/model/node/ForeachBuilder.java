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
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the properties of foreach node in the flow model.
 *
 * @since 2.0.0
 */
public class ForeachBuilder extends NodeBuilder {

    public static final String LABEL = "Foreach";
    public static final String DESCRIPTION = "Iterate over a block of code.";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.FOREACH);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.token().keyword(SyntaxKind.FOREACH_KEYWORD)
                .stepOut()
                .typedBindingPattern()
                .token().keyword(SyntaxKind.IN_KEYWORD);

        Optional<Property> exprProperty = sourceBuilder.getProperty(Property.COLLECTION_KEY);
        exprProperty.ifPresent(property -> sourceBuilder.token().expression(property));

        Optional<Branch> body = sourceBuilder.flowNode.getBranch(Branch.BODY_LABEL);
        body.ifPresent(branch -> sourceBuilder.body(branch.children()));

        return sourceBuilder
                .onFailure()
                .textEdit()
                .build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties().dataVariable(null, context.getAllVisibleSymbolNames()).collection(null);
        this.branches = List.of(Branch.DEFAULT_BODY_BRANCH, Branch.getDefaultOnFailBranch(true));
    }
}
