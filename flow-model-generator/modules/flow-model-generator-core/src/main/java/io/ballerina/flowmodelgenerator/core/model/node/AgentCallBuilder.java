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
import io.ballerina.flowmodelgenerator.core.db.model.Function;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.db.model.Parameter;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.*;
import io.ballerina.flowmodelgenerator.core.utils.FlowNodeUtil;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.*;

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
        FunctionResult function = new FunctionResult(-1, "run", "Run agent", "error?", "ai.agent", "wso2",
                "1.0.0", "", Function.Kind.FUNCTION, true, false);
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

        List<ParameterResult> parameterResults = List.of(
                new ParameterResult(-1, "agent", "BaseAgent", Parameter.Kind.REQUIRED, "", "", false, ""),
                new ParameterResult(-2, "query", "string", Parameter.Kind.REQUIRED, "", "", false, ""),
                new ParameterResult(-3, "maxIter", "int", Parameter.Kind.DEFAULTABLE, "5", "", false, ""),
                new ParameterResult(-4, "context", "string|map<json>", Parameter.Kind.DEFAULTABLE, "{}", "", false,
                        ""),
                new ParameterResult(-5, "verbose", "boolean", Parameter.Kind.DEFAULTABLE, "true", "", false, "")
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

    private void setCustomProperties(Collection<ParameterResult> functionParameters) {
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
