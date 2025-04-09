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
 * Represents configurable variable.
 *
 * @since 2.0.0
 */
public class ConfigVariableBuilder extends NodeBuilder {

    public static final String DEFAULTABLE = "defaultable";
    public static final String LABEL = "Config";
    public static final String DESCRIPTION = "Create a configurable variable";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.CONFIG_VARIABLE);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        Optional<Property> variable = sourceBuilder.getProperty(Property.VARIABLE_KEY);
        if (variable.isEmpty()) {
            throw new RuntimeException("Variable is not set for the Assign node");
        }
        sourceBuilder.token()
                .keyword(SyntaxKind.CONFIGURABLE_KEYWORD)
                .whiteSpace()
                .expression(variable.get())
                .whiteSpace()
                .keyword(SyntaxKind.EQUAL_TOKEN)
                .whiteSpace();

        Optional<Property> expressionOpt = sourceBuilder.getProperty(DEFAULTABLE);
        if (expressionOpt.isEmpty()) {
            throw new RuntimeException("Expression is not set for configurable variable");
        }

        String value = expressionOpt.get().toSourceCode();
        if (value.isEmpty()) {
            value = "?";
        }
        sourceBuilder.token().expression(value).endOfStatement();

        return sourceBuilder.textEdit().build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties().defaultableName("").expression("", Property.EXPRESSION_DOC);
    }
}
