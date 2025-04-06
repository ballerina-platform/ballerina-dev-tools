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
 * Represents the properties of a JSON payload node.
 *
 * @since 2.0.0
 */
public class JsonPayloadBuilder extends NodeBuilder {

    public static final String LABEL = "Assign JSON";
    public static final String DESCRIPTION = LABEL;
    public static final String JSON_PAYLOAD_DOC = "Create new JSON payload";
    private static final String DUMMY_JSON_PAYLOAD = "{\"value\": \"Dummy JSON value\"}";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL);
        codedata().node(NodeKind.JSON_PAYLOAD);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties().payload(null, "json")
                .expression(DUMMY_JSON_PAYLOAD, JSON_PAYLOAD_DOC);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();

        Optional<Property> exprProperty = sourceBuilder.getProperty(Property.EXPRESSION_KEY);
        exprProperty.ifPresent(value -> sourceBuilder.token().expression(value).endOfStatement());

        return sourceBuilder.textEdit().build();
    }
}
