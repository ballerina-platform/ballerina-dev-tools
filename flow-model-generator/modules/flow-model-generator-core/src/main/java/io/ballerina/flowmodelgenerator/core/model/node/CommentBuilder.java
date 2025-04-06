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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents the properties of a comment node.
 *
 * @since 2.0.0
 */
public class CommentBuilder extends NodeBuilder {

    public static final String LABEL = "Comment";
    public static final String DESCRIPTION = "Comment of a node";
    private static final String DOUBLE_SLASH = "// ";
    private static final String NEW_LINE = System.lineSeparator();

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL);
        codedata().node(NodeKind.COMMENT);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        Optional<Property> property = sourceBuilder.getProperty(Property.COMMENT_KEY);
        if (property.isEmpty()) {
            throw new IllegalStateException("Comment must be defined for a comment node");
        }
        String formattedComment = Arrays.stream(property.get().toSourceCode().split("\n"))
                .map(line -> DOUBLE_SLASH + line + NEW_LINE)
                .collect(Collectors.joining());
        return sourceBuilder
                .token().name(NEW_LINE).name(formattedComment).stepOut()
                .comment()
                .build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties().comment("");
    }
}
