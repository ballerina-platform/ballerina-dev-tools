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
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.PropertyCodedata;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.FlowNodeUtil;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.FunctionDataBuilder;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.modelgenerator.commons.ParameterData;
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
public class ResourceActionCallBuilder extends CallBuilder {

    public static final String TARGET_TYPE_KEY = "targetType";

    @Override
    public void setConcreteConstData() {
        codedata().node(NodeKind.RESOURCE_ACTION_CALL);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        if (codedata.org().equals("$anon")) {
            return;
        }

        FunctionDataBuilder functionDataBuilder = new FunctionDataBuilder()
                .name(codedata.symbol())
                .moduleInfo(new ModuleInfo(codedata.org(), codedata.module(), codedata.module(), codedata.version()))
                .lsClientLogger(context.lsClientLogger())
                .parentSymbolType(codedata.object())
                .resourcePath(codedata.resourcePath())
                .project(PackageUtil.loadProject(context.workspaceManager(), context.filePath()))
                .functionResultKind(FunctionData.Kind.RESOURCE);

        FunctionData functionData = functionDataBuilder.build();

        metadata()
                .label(functionData.name())
                .description(functionData.description())
                .icon(CommonUtils.generateIcon(functionData.org(), functionData.packageName(),
                        functionData.version()));
        codedata()
                .org(functionData.org())
                .module(functionData.packageName())
                .object(codedata.object())
                .symbol(functionData.name());

        setExpressionProperty(codedata);

        String resourcePath = functionData.resourcePath();
        properties().resourcePath(resourcePath, resourcePath.equals(ParamUtils.REST_RESOURCE_PATH));

        setParameterProperties(functionData);

        String returnTypeName = functionData.returnType();
        if (CommonUtils.hasReturn(returnTypeName)) {
            setReturnTypeProperties(functionData, context, Property.VARIABLE_NAME);
        }

        if (functionData.returnError()) {
            properties().checkError(true);
        }
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariableWithInferredType();
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

        String resourcePath = flowNode.properties().get(Property.RESOURCE_PATH_KEY)
                .codedata().originalName();

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
            PropertyCodedata propCodedata = property.get()
                    .codedata();
            if (propCodedata == null) {
                continue;
            }
            if (propCodedata.kind().equals(ParameterData.Kind.PATH_PARAM.name())) {
                String pathParamSubString = "[" + key + "]";
                String replacement = "[" + property.get().value().toString() + "]";
                resourcePath = resourcePath.replace(pathParamSubString, replacement);
                ignoredKeys.add(key);
            } else if (propCodedata.kind().equals(ParameterData.Kind.PATH_REST_PARAM.name())) {
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
                .acceptImportWithVariableType()
                .build();
    }

    @Override
    protected NodeKind getFunctionNodeKind() {
        return NodeKind.RESOURCE_ACTION_CALL;
    }

    @Override
    protected FunctionData.Kind getFunctionResultKind() {
        return FunctionData.Kind.RESOURCE;
    }
}
