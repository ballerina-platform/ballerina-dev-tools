/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.utils.FlowNodeUtil;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.FunctionDataBuilder;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract base class for function-like builders (functions, methods, resource actions).
 *
 * @since 2.0.0
 */
public abstract class CallBuilder extends NodeBuilder {

    protected abstract NodeKind getFunctionNodeKind();

    protected abstract FunctionData.Kind getFunctionResultKind();

    @Override
    public void setConcreteConstData() {
        codedata().node(getFunctionNodeKind());
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();

        FunctionDataBuilder functionDataBuilder = new FunctionDataBuilder()
                .name(codedata.symbol())
                .moduleInfo(new ModuleInfo(codedata.org(), codedata.module(), codedata.module(), codedata.version()))
                .lsClientLogger(context.lsClientLogger())
                .functionResultKind(getFunctionResultKind())
                .userModuleInfo(moduleInfo);

        NodeKind functionNodeKind = getFunctionNodeKind();
        if (functionNodeKind != NodeKind.FUNCTION_CALL) {
            functionDataBuilder.parentSymbolType(codedata.object());
        }

        // Set the semantic model if the function is local
        boolean isLocalFunction = isLocalFunction(context.workspaceManager(), context.filePath(), codedata);
        if (isLocalFunction) {
            WorkspaceManager workspaceManager = context.workspaceManager();
            PackageUtil.loadProject(context.workspaceManager(), context.filePath());
            context.workspaceManager().module(context.filePath())
                    .map(module -> ModuleInfo.from(module.descriptor()))
                    .ifPresent(functionDataBuilder::userModuleInfo);
            SemanticModel semanticModel = workspaceManager.semanticModel(context.filePath()).orElseThrow();
            functionDataBuilder.semanticModel(semanticModel);
        }
        FunctionData functionData = functionDataBuilder.build();

        metadata()
                .label(functionData.name())
                .icon(CommonUtils.generateIcon(functionData.org(), functionData.packageName(),
                        functionData.version()))
                .description(functionData.description());
        codedata()
                .node(functionNodeKind)
                .org(codedata.org())
                .module(codedata.module())
                .object(codedata.object())
                .version(codedata.version())
                .symbol(codedata.symbol())
                .inferredReturnType(functionData.inferredReturnType() ? functionData.returnType() : null);

        if (functionNodeKind != NodeKind.FUNCTION_CALL && functionNodeKind != NodeKind.AGENT &&
                functionNodeKind != NodeKind.CLASS_INIT) {
            properties().custom()
                    .metadata()
                    .label(Property.CONNECTION_LABEL)
                    .description(Property.CONNECTION_DOC)
                    .stepOut()
                    .typeConstraint(isLocalFunction ? codedata.object() :
                            CommonUtils.getClassType(codedata.module(), codedata.object()))
                    .value(codedata.parentSymbol())
                    .type(Property.ValueType.EXPRESSION)
                    .stepOut()
                    .addProperty(Property.CONNECTION_KEY);
        }
        setParameterProperties(functionData);

        if (CommonUtils.hasReturn(functionData.returnType())) {
            setReturnTypeProperties(functionData, context, Property.VARIABLE_NAME);
        }

        if (functionData.returnError()) {
            properties().checkError(true);
        }
    }

    public static void buildInferredTypeProperty(NodeBuilder nodeBuilder, ParameterData paramData, String value) {
        String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramData.name());
        nodeBuilder.properties().custom()
                .metadata()
                    .label(unescapedParamName)
                    .description(paramData.description())
                    .stepOut()
                .codedata()
                    .kind(paramData.kind().name())
                    .originalName(paramData.name())
                    .stepOut()
                .value(value)
                .placeholder(paramData.defaultValue())
                .type(Property.ValueType.TYPE)
                .typeConstraint(paramData.type())
                .imports(paramData.importStatements())
                .editable()
                .stepOut()
                .addProperty(unescapedParamName);
    }

    protected void setParameterProperties(FunctionData function) {
        boolean hasOnlyRestParams = function.parameters().size() == 1;

        // Build the inferred type property at the top if exists
        Map<String, ParameterData> paramMap = new LinkedHashMap<>();
        function.parameters().forEach((key, paramData) -> {
            if (paramData.kind() != ParameterData.Kind.PARAM_FOR_TYPE_INFER) {
                paramMap.put(key, paramData);
                return;
            }
            buildInferredTypeProperty(this, paramData, null);
        });

        for (ParameterData paramResult : paramMap.values()) {
            if (paramResult.kind().equals(ParameterData.Kind.INCLUDED_RECORD)) {
                continue;
            }

            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder = properties().custom();
            String label = paramResult.label();
            customPropBuilder
                    .metadata()
                        .label(label == null || label.isEmpty() ? unescapedParamName : label)
                        .description(paramResult.description())
                        .stepOut()
                    .codedata()
                        .kind(paramResult.kind().name())
                        .originalName(paramResult.name())
                        .stepOut()
                    .placeholder(paramResult.defaultValue())
                    .typeConstraint(paramResult.type())
                    .typeMembers(paramResult.typeMembers())
                    .imports(paramResult.importStatements())
                    .editable()
                    .defaultable(paramResult.optional());

            switch (paramResult.kind()) {
                case PARAM_FOR_TYPE_INFER -> {
                    customPropBuilder.advanced(false);
                    customPropBuilder.optional(false);
                    customPropBuilder.type(Property.ValueType.TYPE);
                }
                case INCLUDED_RECORD_REST -> {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    unescapedParamName = "additionalValues";
                    customPropBuilder.type(Property.ValueType.MAPPING_EXPRESSION_SET);
                }
                case REST_PARAMETER -> {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    customPropBuilder.type(Property.ValueType.EXPRESSION_SET);
                }
                default -> customPropBuilder.type(Property.ValueType.EXPRESSION);
            }

            customPropBuilder
                    .stepOut()
                    .addProperty(FlowNodeUtil.getPropertyKey(unescapedParamName));
        }
    }

    protected void setReturnTypeProperties(FunctionData functionData, TemplateContext context, String label) {
        properties()
                .type(functionData.returnType(), false, functionData.importStatements())
                .data(functionData.returnType(), context.getAllVisibleSymbolNames(), label);
    }

    protected void setExpressionProperty(Codedata codedata) {
        properties().custom()
                .metadata()
                    .label(Property.CONNECTION_LABEL)
                    .description(Property.CONNECTION_DOC)
                    .stepOut()
                .typeConstraint(CommonUtils.getClassType(codedata.module(), codedata.object()))
                .value(codedata.parentSymbol())
                .type(Property.ValueType.EXPRESSION)
                .stepOut()
                .addProperty(Property.CONNECTION_KEY);
    }

    protected static boolean isLocalFunction(WorkspaceManager workspaceManager, Path filePath, Codedata codedata) {
        if (codedata.org() == null || codedata.module() == null || codedata.version() == null) {
            return true;
        }
        try {
            Project project = workspaceManager.loadProject(filePath);
            PackageDescriptor descriptor = project.currentPackage().descriptor();
            String packageOrg = descriptor.org().value();
            String packageName = descriptor.name().value();
            String packageVersion = descriptor.version().value().toString();

            return packageOrg.equals(codedata.org())
                    && packageName.equals(codedata.module())
                    && packageVersion.equals(codedata.version());
        } catch (WorkspaceDocumentException | EventSyncException e) {
            return false;
        }
    }
}
