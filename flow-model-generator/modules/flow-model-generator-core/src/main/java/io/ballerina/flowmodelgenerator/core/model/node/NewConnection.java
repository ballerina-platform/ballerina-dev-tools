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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a new connection node in the flow model.
 *
 * @since 1.4.0
 */
public class NewConnection extends NodeBuilder {

    private static final String NEW_CONNECTION_LABEL = "New Connection";
    private static final Central central = new CentralProxy();

    @Override
    public void setConcreteConstData() {
        metadata().label(NEW_CONNECTION_LABEL);
        codedata().node(FlowNode.Kind.NEW_CONNECTION).symbol("init");
    }

    @Override
    public void setConcreteTemplateData(Codedata codedata) {
        this.cachedFlowNode = central.getNodeTemplate(codedata.node(), codedata.module(), codedata.symbol());
    }

    @Override
    public String toSource(FlowNode flowNode) {
        SourceBuilder sourceBuilder = new SourceBuilder();

        Optional<Property> variable = flowNode.getProperty(Property.VARIABLE_KEY);
        Optional<Property> type = flowNode.getProperty(PropertiesBuilder.DATA_TYPE_KEY);

        if (type.isPresent() && variable.isPresent()) {
            sourceBuilder
                    .keyword(SyntaxKind.FINAL_KEYWORD)
                    .expressionWithType(type.get(), variable.get())
                    .keyword(SyntaxKind.EQUAL_TOKEN)
                    .keyword(SyntaxKind.CHECK_KEYWORD)
                    .keyword(SyntaxKind.NEW_KEYWORD)
                    .keyword(SyntaxKind.OPEN_PAREN_TOKEN);
        }

        FlowNode nodeTemplate = central.getNodeTemplate(flowNode.codedata().node(), flowNode.codedata().module(),
                flowNode.codedata().symbol());
        Set<String> keys = new LinkedHashSet<>(nodeTemplate.properties().keySet());
        keys.remove("variable");
        keys.remove("type");
        keys.remove("scope");
        Iterator<String> iterator = keys.iterator();

        if (!keys.isEmpty()) {
            String firstKey = iterator.next();
            Optional<Property> firstParameter = flowNode.getProperty(firstKey);
            Optional<Property> firstTemplateParameter = nodeTemplate.getProperty(firstKey);
            if (firstParameter.isPresent() && firstTemplateParameter.isPresent() &&
                    !isDefaultValue(firstParameter.get(), firstTemplateParameter.get())) {
                sourceBuilder.expression(firstParameter.get());
            }

            boolean hasEmptyParam = false;
            while (iterator.hasNext()) {
                String parameterKey = iterator.next();
                Optional<Property> parameter = flowNode.getProperty(parameterKey);
                Optional<Property> templateParameter = nodeTemplate.getProperty(parameterKey);

                if (parameter.isEmpty() || templateParameter.isEmpty() || parameter.get().value() == null ||
                        isDefaultValue(parameter.get(), templateParameter.get())) {
                    hasEmptyParam = true;
                    continue;
                }

                sourceBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                if (hasEmptyParam) {
                    sourceBuilder
                            .name(parameterKey)
                            .keyword(SyntaxKind.EQUAL_TOKEN);
                    hasEmptyParam = false;
                }
                sourceBuilder.expression(parameter.get());
            }
        }

        sourceBuilder
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();

        return sourceBuilder.build(false);
    }

    private boolean isDefaultValue(Property node, Property templateNode) {
        return node.optional() && node.value().equals(templateNode.value());
    }
}
