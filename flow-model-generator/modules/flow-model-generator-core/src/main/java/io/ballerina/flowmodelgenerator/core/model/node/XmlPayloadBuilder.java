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
 * Represents the properties of a xml payload node.
 *
 * @since 1.4.0
 */
public class XmlPayloadBuilder extends NodeBuilder {

    public static final String LABEL = "Assign XML";
    public static final String DESCRIPTION = LABEL;
    public static final String XML_PAYLOAD_DOC = "Create new XML payload";
    private static final String DUMMY_XML_PAYLOAD = "xml `<dummy>Dummy XML value</dummy>`";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL);
        codedata().node(NodeKind.XML_PAYLOAD);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties().payload(null, "xml")
                .expression(DUMMY_XML_PAYLOAD, XML_PAYLOAD_DOC);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();

        Optional<Property> exprProperty = sourceBuilder.flowNode.getProperty(Property.EXPRESSION_KEY);
        exprProperty.ifPresent(value -> sourceBuilder.token().expression(value).endOfStatement());

        return sourceBuilder.textEdit(false).build();
    }
}
