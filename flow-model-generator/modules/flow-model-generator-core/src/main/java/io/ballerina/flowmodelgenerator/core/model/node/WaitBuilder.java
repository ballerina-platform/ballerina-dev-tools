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

import com.google.gson.Gson;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the properties of a wait node.
 *
 * @since 2.0.0
 */
public class WaitBuilder extends NodeBuilder {

    public static final String LABEL = "Wait";
    public static final String DESCRIPTION = "Wait for a set of futures to complete";

    public static final String WAIT_ALL_KEY = "waitAll";
    public static final String WAIT_ALL_LABEL = "Wait All";
    public static final String WAIT_ALL_DOC = "Wait for all tasks to complete";

    public static final String FUTURES_KEY = "futures";
    public static final String FUTURES_LABEL = "Futures";
    public static final String FUTURES_DOC = "The futures to wait for";

    public static final String FUTURE_KEY = "future";
    public static final String FUTURE_LABEL = "Future";
    public static final String FUTURE_DOC = "The worker/async function to wait for";

    private static final Gson gson = new Gson();

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.WAIT);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties()
                .dataVariable(null, context.getAllVisibleSymbolNames())
                .waitAll(false)
                .nestedProperty()
                    .nestedProperty()
                        .waitField(null)
                        .expression(null)
                    .endNestedProperty(Property.ValueType.FIXED_PROPERTY, WaitBuilder.FUTURE_KEY + 1,
                        WaitBuilder.FUTURE_LABEL, WaitBuilder.FUTURE_DOC)
                .endNestedProperty(Property.ValueType.REPEATABLE_PROPERTY, WaitBuilder.FUTURES_KEY,
                        WaitBuilder.FUTURES_LABEL, WaitBuilder.FUTURES_DOC);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();
        sourceBuilder.token().keyword(SyntaxKind.WAIT_KEYWORD);

        boolean waitAll = sourceBuilder.flowNode.properties().containsKey(WAIT_ALL_KEY) &&
                sourceBuilder.flowNode.properties().get(WAIT_ALL_KEY).value().equals(true);

        if (waitAll) {
            sourceBuilder.token().keyword(SyntaxKind.OPEN_BRACE_TOKEN);
        }

        Optional<Property> futures = sourceBuilder.flowNode.getProperty(FUTURES_KEY);
        if (futures.isEmpty() || !(futures.get().value() instanceof Map<?, ?> futureMap)) {
            throw new IllegalStateException("Wait node does not have futures to wait for");
        }

        List<String> expressions = new ArrayList<>();
        for (Object obj : futureMap.values()) {
            Property futureProperty = gson.fromJson(gson.toJsonTree(obj), Property.class);
            if (!(futureProperty.value() instanceof Map<?, ?> futureChildMap)) {
                continue;
            }

            Map<String, Property> futureChildProperties = gson.fromJson(gson.toJsonTree(futureChildMap),
                    FormBuilder.NODE_PROPERTIES_TYPE);

            String waitField;
            Property variableProperty = futureChildProperties.get(Property.VARIABLE_KEY);
            if (waitAll && variableProperty != null && !variableProperty.value().toString().isEmpty()) {
                waitField = variableProperty.value() + ":";
            } else {
                waitField = "";
            }

            Property expressionProperty = futureChildProperties.get(Property.EXPRESSION_KEY);
            if (expressionProperty == null) {
                continue;
            }
            expressions.add(waitField + expressionProperty.value().toString());
        }
        String delimiter = waitAll ? "," : "|";
        sourceBuilder.token().name(String.join(delimiter, expressions));

        if (waitAll) {
            sourceBuilder.token().keyword(SyntaxKind.CLOSE_BRACE_TOKEN);
        }

        return sourceBuilder.token().endOfStatement().stepOut().textEdit().build();
    }
}
