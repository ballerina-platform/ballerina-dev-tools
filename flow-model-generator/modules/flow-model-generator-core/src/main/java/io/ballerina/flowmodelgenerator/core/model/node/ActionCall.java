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
import io.ballerina.flowmodelgenerator.core.central.CentralApiFactory;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents the generalized action invocation node in the flow model.
 *
 * @since 1.4.0
 */
public class ActionCall extends NodeBuilder {

    public static final String TARGET_TYPE_KEY = "targetType";

    @Override
    public void setConcreteConstData() {
        codedata().node(FlowNode.Kind.ACTION_CALL);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();

        if (sourceBuilder.flowNode.returning()) {
            sourceBuilder.token().keyword(SyntaxKind.RETURN_KEYWORD);
        }

        if (sourceBuilder.flowNode.hasFlag(FlowNode.NODE_FLAG_CHECKED)) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        FlowNode nodeTemplate = CentralApiFactory.getInstance().getNodeTemplate(sourceBuilder.flowNode.codedata());

        Optional<Property> connection = sourceBuilder.flowNode.getProperty(Property.CONNECTION_KEY);
        if (connection.isEmpty()) {
            throw new IllegalStateException("Client must be defined for an action call node");
        }
        return sourceBuilder.token()
                .name(connection.get().value().toString())
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(nodeTemplate.metadata().label())
                .stepOut()
                .functionParameters(nodeTemplate,
                        Set.of(Property.CONNECTION_KEY, Property.VARIABLE_KEY, Property.DATA_TYPE_KEY, TARGET_TYPE_KEY))
                .textEdit(false)
                .acceptImport()
                .build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        this.cachedFlowNode = CentralApiFactory.getInstance().getNodeTemplate(context.codedata());
    }
}
