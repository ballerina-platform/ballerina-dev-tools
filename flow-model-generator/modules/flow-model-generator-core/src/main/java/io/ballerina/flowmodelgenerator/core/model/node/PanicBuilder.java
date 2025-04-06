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
import io.ballerina.flowmodelgenerator.core.TypesGenerator;
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
 * Represents the properties of a panic node.
 *
 * @since 2.0.0
 */
public class PanicBuilder extends NodeBuilder {

    public static final String LABEL = "Panic";
    public static final String DESCRIPTION = "Panic and stop the execution";
    public static final String PANIC_EXPRESSION_DOC = "Panic value";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.PANIC);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.token().keyword(SyntaxKind.PANIC_KEYWORD);
        Optional<Property> property = sourceBuilder.getProperty(Property.EXPRESSION_KEY);
        property.ifPresent(value -> sourceBuilder.token()
                .whiteSpace()
                .expression(value));
        sourceBuilder.token().endOfStatement();
        return sourceBuilder.textEdit().build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties().expression("", PANIC_EXPRESSION_DOC, false, TypesGenerator.TYPE_ERROR);
    }
}
