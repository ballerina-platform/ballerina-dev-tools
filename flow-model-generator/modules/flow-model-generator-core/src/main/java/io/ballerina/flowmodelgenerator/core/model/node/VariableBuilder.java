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
 * Represents the properties of a variable declaration node.
 *
 * @since 2.0.0
 */
public class VariableBuilder extends NodeBuilder {

    public static final String LABEL = "Declare Variable";
    public static final String DESCRIPTION = "New variable with type";
    public static final String EXPRESSION_DOC = "Initialize with value";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL);
        codedata().node(NodeKind.VARIABLE);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        Optional<Property> type = sourceBuilder.flowNode.getProperty(Property.TYPE_KEY);
        Optional<Property> variable = sourceBuilder.flowNode.getProperty(Property.VARIABLE_KEY);
        if (type.isPresent() && variable.isPresent()) {
            sourceBuilder.token().expressionWithType(type.get(), variable.get());
        }

        Optional<Property> exprProperty = sourceBuilder.flowNode.getProperty(Property.EXPRESSION_KEY);
        if (exprProperty.isPresent() && !exprProperty.get().toSourceCode().isEmpty()) {
            sourceBuilder.token()
                    .keyword(SyntaxKind.EQUAL_TOKEN)
                    .expression(exprProperty.get());
        }
        sourceBuilder.token().endOfStatement();
        return sourceBuilder.textEdit(false).build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        metadata().description(DESCRIPTION);
        properties()
                .dataVariable(null, true, context.getAllVisibleSymbolNames())
                .expression("", EXPRESSION_DOC, true, null);
    }
}
