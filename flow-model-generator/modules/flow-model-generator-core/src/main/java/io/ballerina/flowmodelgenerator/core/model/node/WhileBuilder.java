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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the properties of a while node in the flow model.
 *
 * @since 2.0.0
 */
public class WhileBuilder extends NodeBuilder {

    public static final String LABEL = "While";
    public static final String DESCRIPTION = "Loop over a block of code.";
    private static final String WHILE_CONDITION_DOC = "Boolean Condition";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.WHILE);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        Optional<Property> condition = sourceBuilder.flowNode.getProperty(Property.CONDITION_KEY);
        Optional<Branch> body = sourceBuilder.flowNode.getBranch(Branch.BODY_LABEL);

        sourceBuilder.token().keyword(SyntaxKind.WHILE_KEYWORD);
        condition.ifPresent(expression -> sourceBuilder.token().expression(expression));
        sourceBuilder.body(body.isPresent() ? body.get().children() : Collections.emptyList());

        // Handle the on fail branch
        sourceBuilder.onFailure();
        return sourceBuilder.textEdit().build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties().condition(null);
    }
}
