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

import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.TypeUtils;
import io.ballerina.flowmodelgenerator.core.db.DatabaseManager;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
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
 * Represents a new connection node in the flow model.
 *
 * @since 1.4.0
 */
public class NewConnection extends NodeBuilder {

    private static final String NEW_CONNECTION_LABEL = "New Connection";

    public static final String INIT_SYMBOL = "init";
    public static final String CLIENT_SYMBOL = "Client";
    public static final String CHECK_ERROR_DOC = "Terminate on error";
    public static final String CONNECTION_NAME_LABEL = "Connection Name";
    public static final String CONNECTION_TYPE_LABEL = "Connection Type";

    @Override
    public void setConcreteConstData() {
        metadata().label(NEW_CONNECTION_LABEL);
        codedata().node(NodeKind.NEW_CONNECTION).symbol("init");
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();

        sourceBuilder.token()
                .keyword(SyntaxKind.CHECK_KEYWORD)
                .keyword(SyntaxKind.NEW_KEYWORD)
                .stepOut()
                .functionParameters(sourceBuilder.flowNode,
                        Set.of(Property.VARIABLE_KEY, Property.DATA_TYPE_KEY, Property.SCOPE_KEY,
                                Property.CHECK_ERROR_KEY));

        Optional<Property> scope = sourceBuilder.flowNode.getProperty(Property.SCOPE_KEY);
        if (scope.isEmpty()) {
            throw new IllegalStateException("Scope is not defined for the new connection node");
        }
        return switch (scope.get().value().toString()) {
            case Property.LOCAL_SCOPE -> sourceBuilder.textEdit(false).build();
            case Property.GLOBAL_SCOPE -> sourceBuilder.textEdit(false, "connections.bal", true).build();
            default -> throw new IllegalStateException("Invalid scope for the new connection node");
        };
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        DatabaseManager dbManager = DatabaseManager.getInstance();
        Optional<FunctionResult> functionResult = codedata.id() != null ? dbManager.getFunction(codedata.id()) :
                dbManager.getFunction(codedata.org(), codedata.module(), codedata.symbol(),
                        DatabaseManager.FunctionKind.CONNECTOR);
        if (functionResult.isEmpty()) {
            return;
        }

        FunctionResult function = functionResult.get();
        metadata()
                .label(function.packageName())
                .description(function.description())
                .icon(CommonUtils.generateIcon(function.org(), function.packageName(), function.version()));
        codedata()
                .node(NodeKind.NEW_CONNECTION)
                .org(function.org())
                .module(function.packageName())
                .object(CLIENT_SYMBOL)
                .symbol(INIT_SYMBOL)
                .id(function.functionId());

        List<ParameterResult> functionParameters = dbManager.getFunctionParameters(function.functionId());
        for (ParameterResult paramResult : functionParameters) {
            boolean optional = paramResult.kind() == ParameterKind.DEFAULTABLE;
            properties().custom()
                    .metadata()
                            .label(paramResult.name())
                            .description(paramResult.description())
                            .stepOut()
                    .type(Property.ValueType.EXPRESSION)
                    .typeConstraint(paramResult.type())
                    .value("")
                    .editable()
                    .defaultable(optional)
                    .stepOut()
                    .addProperty(paramResult.name());
        }

        if (TypeUtils.hasReturn(function.returnType())) {
            properties().type(function.returnType()).data(null);
        }
        properties()
                .scope(Property.GLOBAL_SCOPE)
                .checkError(true, CHECK_ERROR_DOC, false);
    }
}
