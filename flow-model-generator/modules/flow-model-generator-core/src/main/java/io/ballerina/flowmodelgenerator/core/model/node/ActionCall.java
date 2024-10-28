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
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.central.ConnectorResponse;
import io.ballerina.flowmodelgenerator.core.central.LocalIndexCentral;
import io.ballerina.flowmodelgenerator.core.central.RemoteCentral;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
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
        codedata().node(NodeKind.ACTION_CALL);
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

        FlowNode nodeTemplate = LocalIndexCentral.getInstance().getNodeTemplate(sourceBuilder.flowNode.codedata());
        if (nodeTemplate == null) {
            nodeTemplate = fetchNodeTemplate(NodeBuilder.getNodeFromKind(NodeKind.ACTION_CALL),
                    sourceBuilder.flowNode.codedata());
        }
        if (nodeTemplate == null) {
            throw new IllegalStateException("Action call node template not found");
        }

        Optional<Property> connection = sourceBuilder.flowNode.getProperty(Property.CONNECTION_KEY);
        if (connection.isEmpty()) {
            throw new IllegalStateException("Client must be defined for an action call node");
        }
        return sourceBuilder.token()
                .name(connection.get().toSourceCode())
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(nodeTemplate.metadata().label())
                .stepOut()
                .functionParameters(nodeTemplate,
                        Set.of(Property.CONNECTION_KEY, Property.VARIABLE_KEY, Property.DATA_TYPE_KEY, TARGET_TYPE_KEY))
                .textEdit(false)
                .acceptImport()
                .build();
    }

    private static FlowNode fetchNodeTemplate(NodeBuilder nodeBuilder, Codedata codedata) {
        if (codedata.org().equals("$anon")) {
            return null;
        }

        ConnectorResponse connector = codedata.id() != null ? RemoteCentral.getInstance().connector(codedata.id()) :
                RemoteCentral.getInstance()
                        .connector(codedata.org(), codedata.module(), codedata.version(), codedata.object());

        if (connector == null) {
            return null;
        }

        Optional<ConnectorResponse.Function> optFunction = connector.functions().stream()
                .filter(f -> f.name().equals(codedata.symbol()))
                .findFirst();
        if (optFunction.isEmpty()) {
            return null;
        }
        nodeBuilder
                .metadata()
                    .label(optFunction.get().name())
                    .icon(connector.icon())
                    .description(optFunction.get().documentation())
                    .stepOut()
                .codedata()
                    .org(codedata.org())
                    .module(codedata.module())
                    .object(codedata.object())
                    .id(codedata.id())
                    .symbol(codedata.symbol());

        for (ConnectorResponse.Parameter param : optFunction.get().parameters()) {
            nodeBuilder.properties().custom(param.name(), param.name(), param.documentation(),
                    Property.valueTypeFrom(param.typeName()),
                    CommonUtils.getTypeConstraint(param, param.typeName()),
                    CommonUtils.getDefaultValueForType(param.typeName()), param.optional(), param.optional());
        }

        String returnType = optFunction.get().returnType().typeName();
        if (returnType != null) {
            nodeBuilder.properties().type(returnType).data(null);
        }

        nodeBuilder.properties().custom(Property.CONNECTION_KEY, connector.name(), connector.documentation(),
                Property.ValueType.EXPRESSION, connector.moduleName() + ":" + connector.name(), connector.name(),
                false);
        return nodeBuilder.build();
    }

    public static FlowNode getNodeTemplate(Codedata codedata) {
        FlowNode nodeTemplate = LocalIndexCentral.getInstance().getNodeTemplate(codedata);
        if (nodeTemplate == null) {
            return fetchNodeTemplate(NodeBuilder.getNodeFromKind(NodeKind.ACTION_CALL), codedata);
        }
        return nodeTemplate;
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        FlowNode nodeTemplate = LocalIndexCentral.getInstance().getNodeTemplate(codedata);
        if (nodeTemplate != null) {
            this.cachedFlowNode = nodeTemplate;
        } else {
            fetchNodeTemplate(this, codedata);
        }
    }
}
