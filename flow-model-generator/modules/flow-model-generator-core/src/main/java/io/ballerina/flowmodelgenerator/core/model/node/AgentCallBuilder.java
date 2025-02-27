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
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.FlowNodeUtil;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a function call node.
 *
 * @since 2.0.0
 */
public class AgentCallBuilder extends NodeBuilder {

    @Override
    public void setConcreteConstData() {
        codedata().node(NodeKind.AGENT_CALL);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        handleAgentRunFunction(context, context.codedata());
    }

    private void handleAgentRunFunction(TemplateContext context, Codedata codedata) {
        FunctionData function = new FunctionData(-1, "run", "Run agent", "error?", "ai.agent", "wso2",
                "1.0.0", "", FunctionData.Kind.FUNCTION, true, false);
        metadata()
                .label(function.name())
                .description(function.description());
        codedata()
                .node(NodeKind.AGENT_CALL)
                .org(codedata.org())
                .module(codedata.module())
                .object(codedata.object())
                .version(codedata.version())
                .symbol(codedata.symbol());

        List<ParameterData> parameterResults = List.of(
                ParameterData.from("agent", "BaseAgent", ParameterData.Kind.REQUIRED, "",  "", false),
                ParameterData.from("query", "string", ParameterData.Kind.REQUIRED, "",  "", false),
                ParameterData.from("maxIter", "int", ParameterData.Kind.DEFAULTABLE, "5",  "", false),
                ParameterData.from("context", "string|map<json>", ParameterData.Kind.DEFAULTABLE, "{}",  "", false),
                ParameterData.from("verbose", "boolean", ParameterData.Kind.DEFAULTABLE, "true",  "", false)
        );
        setCustomProperties(parameterResults);

        String returnTypeName = function.returnType();
        if (CommonUtils.hasReturn(function.returnType())) {
            setReturnTypeProperties(returnTypeName, context);
        }

        if (function.returnError()) {
            properties().checkError(true);
        }
    }

    private void setCustomProperties(Collection<ParameterData> functionParameters) {
        boolean hasOnlyRestParams = functionParameters.size() == 1;
        for (ParameterData paramResult : functionParameters) {
            if (paramResult.kind().equals(ParameterData.Kind.PARAM_FOR_TYPE_INFER)
                    || paramResult.kind().equals(ParameterData.Kind.INCLUDED_RECORD)) {
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

            switch (paramResult.kind()) {
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
                    .addProperty(unescapedParamName);
        }
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();
        FlowNode flowNode = sourceBuilder.flowNode;

        if (FlowNodeUtil.hasCheckKeyFlagSet(flowNode)) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        String module = flowNode.codedata().module();
        String methodCallPrefix = (module != null) ? module.substring(module.lastIndexOf('.') + 1) + ":" : "";
        String methodCall = methodCallPrefix + flowNode.metadata().label();

        return sourceBuilder.token()
                .name(methodCall)
                .stepOut()
                .functionParameters(flowNode, Set.of("variable", "type", "view", "checkError"))
                .textEdit(false)
                .acceptImport(sourceBuilder.filePath)
                .build();
    }

    private void setReturnTypeProperties(String returnTypeName, TemplateContext context) {
        boolean editable = false;
        String updatedReturnType = returnTypeName;
        if (returnTypeName.contains(RemoteActionCallBuilder.TARGET_TYPE_KEY)) {
            updatedReturnType = returnTypeName.replace(RemoteActionCallBuilder.TARGET_TYPE_KEY, "json");
            editable = true;
        }
        properties()
                .type(updatedReturnType, editable)
                .data(updatedReturnType, context.getAllVisibleSymbolNames(), Property.VARIABLE_NAME);
    }
}
