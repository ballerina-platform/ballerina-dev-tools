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
 * Represents a new connection node in the flow model.
 *
 * @since 1.4.0
 */
public class NewConnection extends NodeBuilder {

    private static final String NEW_CONNECTION_LABEL = "New Connection";

    public static final String INIT_SYMBOL = "init";
    public static final String CLIENT_SYMBOL = "Client";

    @Override
    public void setConcreteConstData() {
        metadata().label(NEW_CONNECTION_LABEL);
        codedata().node(FlowNode.Kind.NEW_CONNECTION).symbol("init");
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        if (context.codedata().id() != null) {
            ConnectorResponse connector = RemoteCentral.getInstance().connector(context.codedata().id());
            Optional<ConnectorResponse.Function> initFunction = connector.functions().stream()
                    .filter(function -> function.name().equals(INIT_SYMBOL))
                    .findFirst();
            metadata()
                    .label(connector.moduleName())
                    .keywords(connector.packageInfo().keywords())
                    .icon(connector.icon())
                    .description(connector.documentation());
            codedata()
                    .node(FlowNode.Kind.NEW_CONNECTION)
                    .org(connector.packageInfo().organization())
                    .module(connector.moduleName())
                    .object(connector.name())
                    .symbol(INIT_SYMBOL);

            if (initFunction.isPresent()) {
                for (ConnectorResponse.Parameter param : initFunction.get().parameters()) {
                    properties().custom(param.name(), param.name(), param.documentation(),
                            Property.valueTypeFrom(param.typeName()), getTypeConstraint(param, param.typeName()),
                            CommonUtils.getDefaultValueForType(param.typeName()), param.optional());
                }
            }
        }
        //TODO: Obtain the connector from the codedata information if id doesn't exist.

        properties()
                .dataVariable(null)
                .scope(Property.GLOBAL_SCOPE);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();

        FlowNode nodeTemplate = LocalIndexCentral.getInstance().getNodeTemplate(sourceBuilder.flowNode.codedata());
        sourceBuilder.token()
                .keyword(SyntaxKind.CHECK_KEYWORD)
                .keyword(SyntaxKind.NEW_KEYWORD)
                .stepOut()
                .functionParameters(nodeTemplate,
                        Set.of(Property.VARIABLE_KEY, Property.DATA_TYPE_KEY, Property.SCOPE_KEY));

        Optional<Property> scope = sourceBuilder.flowNode.getProperty(Property.SCOPE_KEY);
        if (scope.isEmpty()) {
            throw new IllegalStateException("Scope is not defined for the new connection node");
        }
        return switch (scope.get().value().toString()) {
            case Property.LOCAL_SCOPE -> sourceBuilder.textEdit(false).build();
            case Property.GLOBAL_SCOPE -> sourceBuilder.textEdit(false, "connections.bal").build();
            default -> throw new IllegalStateException("Invalid scope for the new connection node");
        };
    }

    private static Object getTypeConstraint(ConnectorResponse.Parameter param, String typeName) {
        return switch (typeName) {
            case "inclusion" -> param.inclusionType();
            default -> typeName;
        };
    }
}
