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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.db.DatabaseManager;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.db.model.Parameter;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.ModuleInfo;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FunctionCall extends NodeBuilder {

    @Override
    public void setConcreteConstData() {
        codedata().node(NodeKind.FUNCTION_CALL);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();

        if (isLocalFunction(context.workspaceManager(), context.filePath(), codedata)) {
            WorkspaceManager workspaceManager = context.workspaceManager();

            try {
                Project project = workspaceManager.loadProject(context.filePath());
                this.moduleInfo = ModuleInfo.from(project.currentPackage().getDefaultModule().descriptor());
            } catch (WorkspaceDocumentException | EventSyncException e) {
                throw new RuntimeException("Error loading project: " + e.getMessage());
            }
            SemanticModel semanticModel = workspaceManager.semanticModel(context.filePath()).orElseThrow();
            Optional<Symbol> outSymbol = semanticModel.moduleSymbols().stream()
                    .filter(symbol -> symbol.kind() == SymbolKind.FUNCTION && symbol.nameEquals(codedata.symbol()))
                    .findFirst();
            if (outSymbol.isEmpty()) {
                throw new RuntimeException("Function not found: " + codedata.symbol());
            }

            FunctionSymbol functionSymbol = (FunctionSymbol) outSymbol.get();
            FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();

            metadata().label(codedata.symbol());
            codedata()
                    .node(NodeKind.FUNCTION_CALL)
                    .symbol(codedata.symbol());

            LinkedHashMap<String, ParameterResult> stringParameterResultLinkedHashMap =
                    ParamUtils.buildFunctionParamResultMap(functionSymbol, semanticModel);
            boolean hasOnlyRestParams = stringParameterResultLinkedHashMap.size() == 1;
            for (ParameterResult paramResult : stringParameterResultLinkedHashMap.values()) {
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
                            .stepOut()
                        .placeholder(paramResult.defaultValue())
                        .typeConstraint(paramResult.type())
                        .editable()
                        .defaultable(paramResult.optional() == 1);

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
                } else if (paramResult.kind() == Parameter.Kind.REQUIRED) {
                    customPropBuilder.type(Property.ValueType.EXPRESSION).value(paramResult.defaultValue());
                } else {
                    customPropBuilder.type(Property.ValueType.EXPRESSION);
                }
                customPropBuilder
                        .stepOut()
                        .addProperty(unescapedParamName);
            }

            functionTypeSymbol.returnTypeDescriptor().ifPresent(returnType -> {
                String returnTypeName = CommonUtils.getTypeSignature(semanticModel, returnType, true, moduleInfo);
                boolean editable = true;
                if (returnTypeName.contains(RemoteActionCallBuilder.TARGET_TYPE_KEY)) {
                    returnTypeName = returnTypeName.replace(RemoteActionCallBuilder.TARGET_TYPE_KEY, "json");
                    editable = true;
                }
                properties()
                        .type(returnTypeName, editable)
                        .data(returnTypeName, context.getAllVisibleSymbolNames(), Property.VARIABLE_NAME);
            });
            TypeSymbol errorTypeSymbol = semanticModel.types().ERROR;
            int returnError = functionTypeSymbol.returnTypeDescriptor()
                    .map(returnTypeDesc ->
                            CommonUtils.subTypeOf(returnTypeDesc, errorTypeSymbol) ? 1 : 0).orElse(0);
            if (returnError == 1 && CommonUtils.withinDoClause(context.workspaceManager(),
                    context.filePath(), context.codedata().lineRange())) {
                properties().checkError(true);
            }
            return;
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();
        Optional<FunctionResult> functionResult =
                dbManager.getFunction(codedata.org(), codedata.module(), codedata.symbol(),
                        DatabaseManager.FunctionKind.FUNCTION);

        if (functionResult.isEmpty()) {
            throw new RuntimeException("Function not found: " + codedata.symbol());
        }

        FunctionResult function = functionResult.get();
        metadata()
                .label(function.name())
                .description(function.description());
        codedata()
                .node(NodeKind.FUNCTION_CALL)
                .org(codedata.org())
                .module(codedata.module())
                .object(codedata.object())
                .version(codedata.version())
                .symbol(codedata.symbol());

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
                        .stepOut()
                    .placeholder(paramResult.defaultValue())
                    .typeConstraint(paramResult.type())
                    .editable()
                    .defaultable(paramResult.optional() == 1);

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
            } else if (paramResult.kind() == Parameter.Kind.REQUIRED) {
                customPropBuilder.type(Property.ValueType.EXPRESSION).value(paramResult.defaultValue());
            } else {
                customPropBuilder.type(Property.ValueType.EXPRESSION);
            }
            customPropBuilder
                    .stepOut()
                    .addProperty(unescapedParamName);
        }

        String returnTypeName = function.returnType();
        if (CommonUtils.hasReturn(function.returnType())) {
            boolean editable = false;
            if (returnTypeName.contains(RemoteActionCallBuilder.TARGET_TYPE_KEY)) {
                returnTypeName = returnTypeName.replace(RemoteActionCallBuilder.TARGET_TYPE_KEY, "json");
                editable = true;
            }
            properties()
                    .type(returnTypeName, editable)
                    .data(function.returnType(), context.getAllVisibleSymbolNames(), Property.VARIABLE_NAME);
        }

        if (function.returnError() == 1) {
            properties().checkError(true);
        }
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();
        FlowNode flowNode = sourceBuilder.flowNode;

        if (flowNode.properties().containsKey(Property.CHECK_ERROR_KEY) &&
                flowNode.properties().get(Property.CHECK_ERROR_KEY).value().equals(true)) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        Codedata codedata = flowNode.codedata();
        if (isLocalFunction(sourceBuilder.workspaceManager, sourceBuilder.filePath, codedata)) {
            return sourceBuilder.token()
                    .name(codedata.symbol())
                    .stepOut()
                    .functionParameters(flowNode,
                            Set.of(Property.VARIABLE_KEY, Property.TYPE_KEY, Property.CHECK_ERROR_KEY, "view"))
                    .textEdit(false)
                    .acceptImport()
                    .build();
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

    public boolean isLocalFunction(WorkspaceManager workspaceManager, Path filePath, Codedata codedata) {
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
