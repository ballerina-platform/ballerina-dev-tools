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
 * Represents the properties of a default expression node.
 *
 * @since 1.4.0
 */
public class ExpressionBuilder extends NodeBuilder {

    public static final String LABEL = "Custom Expression";
    public static final String DESCRIPTION = "Represents a custom Ballerina expression";

    public static final String STATEMENT_KEY = "statement";
    public static final String STATEMENT_LABEL = "Statement";
    public static final String STATEMENT_DOC = "Ballerina statement";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.EXPRESSION);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();

        Optional<Property> expression = sourceBuilder.flowNode.getProperty(Property.EXPRESSION_KEY);
        if (expression.isPresent()) {
            sourceBuilder.token()
                    .expression(expression.get())
                    .endOfStatement();
            return sourceBuilder.textEdit(false).build();
        }

        Optional<Property> statement = sourceBuilder.flowNode.getProperty(STATEMENT_KEY);
        if (statement.isEmpty()) {
            throw new IllegalStateException(
                    "One of from the following properties is required: variable, expression, statement");
        }
        sourceBuilder.token()
                .expression(statement.get())
                .endOfStatement();
        return sourceBuilder.textEdit(false).build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties().statement(null);
    }
}
