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
import io.ballerina.flowmodelgenerator.core.central.Central;
import io.ballerina.flowmodelgenerator.core.central.CentralProxy;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.Optional;
import java.util.Set;

/**
 * Represents the generalized action invocation node in the flow model.
 *
 * @since 1.4.0
 */
public class ActionCall extends NodeBuilder {

    private static final Central central = new CentralProxy();

    @Override
    public void setConcreteConstData() {
        codedata().node(FlowNode.Kind.ACTION_CALL);
    }

    @Override
    public String toSource(FlowNode node) {
        SourceBuilder sourceBuilder = new SourceBuilder();

        Optional<Property> variable = node.getProperty(Property.VARIABLE_KEY);
        Optional<Property> type = node.getProperty(PropertiesBuilder.DATA_TYPE_KEY);

        if (type.isPresent() && variable.isPresent()) {
            sourceBuilder.token().expressionWithType(type.get(), variable.get()).keyword(SyntaxKind.EQUAL_TOKEN);
        }

        if (node.returning()) {
            sourceBuilder.token().keyword(SyntaxKind.RETURN_KEYWORD);
        }

        if (node.hasFlag(FlowNode.NODE_FLAG_CHECKED)) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        FlowNode nodeTemplate = central.getNodeTemplate(node.codedata());
        Optional<Property> client = node.getProperty("connection");

        if (client.isEmpty()) {
            throw new IllegalStateException("Client must be defined for an action call node");
        }
        sourceBuilder.token().name(client.get().value())
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(nodeTemplate.metadata().label());

        sourceBuilder.addFunctionArguments(node, nodeTemplate, Set.of("connection", "variable", "type", "targetType"));
        return sourceBuilder.build(false);
    }

    @Override
    public void setConcreteTemplateData(Codedata codedata) {
        this.cachedFlowNode = central.getNodeTemplate(codedata);
    }
}
