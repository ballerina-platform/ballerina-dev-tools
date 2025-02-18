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
import io.ballerina.flowmodelgenerator.core.db.DatabaseManager;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.db.model.Parameter;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.PropertyCodedata;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.FlowNodeUtil;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents the resource action invocation node in the flow model.
 *
 * @since 2.0.0
 */
public class ResourceActionCallBuilder extends NodeBuilder {

    public static final String TARGET_TYPE_KEY = "targetType";

    @Override
    public void setConcreteConstData() {
        codedata().node(NodeKind.RESOURCE_ACTION_CALL);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();
        FlowNode flowNode = sourceBuilder.flowNode;

        if (FlowNodeUtil.hasCheckKeyFlagSet(flowNode)) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        Optional<Property> connection = flowNode.getProperty(Property.CONNECTION_KEY);
        if (connection.isEmpty()) {
            throw new IllegalStateException("Client must be defined for an action call node");
        }

        Set<String> ignoredKeys = new HashSet<>(List.of(Property.CONNECTION_KEY, Property.VARIABLE_KEY,
                Property.TYPE_KEY, TARGET_TYPE_KEY, Property.RESOURCE_PATH_KEY,
                Property.CHECK_ERROR_KEY));

        String resourcePath = flowNode.properties().get(Property.RESOURCE_PATH_KEY).codedata().originalName();

        if (resourcePath.equals(ParamUtils.REST_RESOURCE_PATH)) {
            resourcePath = flowNode.properties().get(Property.RESOURCE_PATH_KEY).value().toString();
        }

        Set<String> keys = new LinkedHashSet<>(flowNode.properties().keySet());
        keys.removeAll(ignoredKeys);

        for (String key : keys) {
            Optional<Property> property = flowNode.getProperty(key);
            if (property.isEmpty()) {
                continue;
            }
            PropertyCodedata propCodedata = property.get().codedata();
            if (propCodedata == null) {
                continue;
            }
            if (propCodedata.kind().equals(Parameter.Kind.PATH_PARAM.name())) {
                String pathParamSubString = "[" + key + "]";
                String replacement = "[" + property.get().value().toString() + "]";
                resourcePath = resourcePath.replace(pathParamSubString, replacement);
                ignoredKeys.add(key);
            } else if (propCodedata.kind().equals(Parameter.Kind.PATH_REST_PARAM.name())) {
                String replacement = property.get().value().toString();
                resourcePath = resourcePath.replace(ParamUtils.REST_PARAM_PATH, replacement);
                ignoredKeys.add(key);
            }
        }


        return sourceBuilder.token()
                .name(connection.get().toSourceCode())
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .resourcePath(resourcePath)
                .keyword(SyntaxKind.DOT_TOKEN)
                .name(sourceBuilder.flowNode.codedata().symbol())
                .stepOut()
                .functionParameters(flowNode, ignoredKeys)
                .textEdit(false)
                .acceptImport()
                .build();
    }

    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        if (codedata.org().equals("$anon")) {
            return;
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();
        Optional<FunctionResult> functionResult = codedata.id() != null ? dbManager.getFunction(codedata.id()) :
                dbManager.getAction(codedata.org(), codedata.module(), codedata.symbol(), codedata.resourcePath(),
                        DatabaseManager.FunctionKind.RESOURCE);
        if (functionResult.isEmpty()) {
            return;
        }

        FunctionResult function = functionResult.get();
        metadata()
                .label(function.name())
                .description(function.description())
                .icon(CommonUtils.generateIcon(function.org(), function.packageName(), function.version()));
        codedata()
                .org(function.org())
                .module(function.packageName())
                .object(NewConnectionBuilder.CLIENT_SYMBOL)
                .id(function.functionId())
                .symbol(function.name());

        properties().custom()
                .metadata()
                .label(Property.CONNECTION_LABEL)
                .description(Property.CONNECTION_DOC)
                .stepOut()
                .typeConstraint(function.packageName() + ":" + NewConnectionBuilder.CLIENT_SYMBOL)
                .value(codedata.parentSymbol())
                .type(Property.ValueType.IDENTIFIER)
                .stepOut()
                .addProperty(Property.CONNECTION_KEY);

        String resourcePath = function.resourcePath();
        properties().resourcePath(resourcePath, resourcePath.equals(ParamUtils.REST_RESOURCE_PATH));

        List<ParameterResult> functionParameters = dbManager.getFunctionParameters(function.functionId());
        boolean hasOnlyRestParams = functionParameters.size() == 1;
        for (ParameterResult paramResult : functionParameters) {
            if (paramResult.kind().equals(Parameter.Kind.PARAM_FOR_TYPE_INFER)
                    || paramResult.kind().equals(Parameter.Kind.INCLUDED_RECORD)) {
                continue;
            }

            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder = properties().custom();
            customPropBuilder
                    .metadata()
                        .label(unescapedParamName)
                        .description(paramResult.description())
                        .stepOut()
                    .codedata()
                        .kind(paramResult.kind().name())
                        .originalName(paramResult.name())
                        .importStatements(paramResult.importStatements())
                        .stepOut()
                    .placeholder(paramResult.defaultValue())
                    .typeConstraint(paramResult.type())
                    .editable()
                    .defaultable(paramResult.optional());

            if (paramResult.kind() == Parameter.Kind.INCLUDED_RECORD_REST) {
                if (hasOnlyRestParams) {
                    customPropBuilder.defaultable(false);
                }
                unescapedParamName = "additionalValues";
                customPropBuilder.type(Property.ValueType.MAPPING_EXPRESSION_SET);
            } else if (paramResult.kind() == Parameter.Kind.REST_PARAMETER) {
                if (hasOnlyRestParams) {
                    customPropBuilder.defaultable(false);
                }
                customPropBuilder.type(Property.ValueType.EXPRESSION_SET);
            } else {
                customPropBuilder.type(Property.ValueType.EXPRESSION);
            }
            customPropBuilder
                    .stepOut()
                    .addProperty(unescapedParamName);
        }

        String returnTypeName = function.returnType();
        if (CommonUtils.hasReturn(function.returnType())) {
            properties()
                    .type(returnTypeName, function.inferredReturnType())
                    .data(function.returnType(), context.getAllVisibleSymbolNames(), Property.VARIABLE_NAME);
        }

        if (function.returnError()) {
            properties().checkError(true);
        }
    }
}
