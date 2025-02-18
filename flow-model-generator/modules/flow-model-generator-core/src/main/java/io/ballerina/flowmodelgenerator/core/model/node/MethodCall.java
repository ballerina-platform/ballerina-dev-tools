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
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
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
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.FlowNodeUtil;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.LinePosition;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a method call.
 *
 * @since 2.0.0
 */
public class MethodCall extends NodeBuilder {

    @Override
    public void setConcreteConstData() {
        codedata().node(NodeKind.METHOD_CALL);
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
            throw new IllegalStateException("Object must be defined for a method call node");
        }

        return sourceBuilder.token()
                .name(connection.get().toSourceCode())
                .keyword(SyntaxKind.DOT_TOKEN)
                .name(flowNode.metadata().label())
                .stepOut()
                .functionParameters(flowNode, Set.of(Property.CONNECTION_KEY, Property.VARIABLE_KEY, Property.TYPE_KEY,
                        Property.CHECK_ERROR_KEY, "view"))
                .textEdit(false)
                .acceptImport(sourceBuilder.filePath)
                .build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        if (FunctionCall.isLocalFunction(context.workspaceManager(), context.filePath(), codedata)) {
            handleLocalObjMethods(context, codedata);
        } else {
            handleImportedModuleObjMethods(context, codedata);
        }
    }

    private void handleImportedModuleObjMethods(TemplateContext context, Codedata codedata) {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        Optional<FunctionResult> functionResult = dbManager.getFunction(codedata.org(), codedata.module(),
                codedata.symbol(), DatabaseManager.FunctionKind.FUNCTION);

        if (functionResult.isEmpty()) {
            throw new RuntimeException("Method not found: " + codedata.symbol());
        }

        FunctionResult function = functionResult.get();
        metadata()
                .label(function.name())
                .description(function.description());
        codedata()
                .node(NodeKind.METHOD_CALL)
                .org(codedata.org())
                .module(codedata.module())
                .object(codedata.object())
                .version(codedata.version())
                .id(function.functionId())
                .symbol(codedata.symbol());
        setExpressionProperty(codedata);
        setCustomProperties(dbManager.getFunctionParameters(function.functionId()));

        String returnTypeName = function.returnType();
        if (CommonUtils.hasReturn(function.returnType())) {
            setReturnTypeProperties(returnTypeName, context);
        }

        if (function.returnError()) {
            properties().checkError(true);
        }
    }

    private void handleLocalObjMethods(TemplateContext context, Codedata codedata) {
        WorkspaceManager workspaceManager = context.workspaceManager();
        Project project = PackageUtil.loadProject(workspaceManager, context.filePath());
        this.moduleInfo = ModuleInfo.from(project.currentPackage().getDefaultModule().descriptor());

        SemanticModel semanticModel = workspaceManager.semanticModel(context.filePath()).orElseThrow();
        Document document = workspaceManager.document(context.filePath()).orElseThrow();

        VariableSymbol varSymbol = findVariableSymbol(semanticModel, document, context.position(),
                codedata.parentSymbol());
        ObjectTypeSymbol objectTypeSymbol = getObjectTypeSymbol(varSymbol);

        MethodSymbol methodSymbol = objectTypeSymbol.methods().get(codedata.symbol());
        if (methodSymbol == null) {
            throw new RuntimeException("Method not found: " + codedata.symbol());
        }

        metadata().label(codedata.symbol());
        codedata()
                .node(NodeKind.METHOD_CALL)
                .symbol(codedata.symbol());
        setExpressionProperty(codedata);
        setCustomProperties(ParamUtils.buildFunctionParamResultMap(methodSymbol, semanticModel).values());
        FunctionTypeSymbol functionTypeSymbol = methodSymbol.typeDescriptor();

        functionTypeSymbol.returnTypeDescriptor().ifPresent(returnType -> {
            String returnTypeName = CommonUtils.getTypeSignature(semanticModel, returnType, true, moduleInfo);
            setReturnTypeProperties(returnTypeName, context);
        });

        if (FunctionCall.containsErrorInReturnType(semanticModel, functionTypeSymbol)
                && FlowNodeUtil.withinDoClause(context)) {
            properties().checkError(true);
        }
    }

    private void setExpressionProperty(Codedata codedata) {
        properties()
                .custom()
                .metadata()
                .label(Property.CONNECTION_LABEL)
                .description(Property.CONNECTION_KEY)
                .stepOut()
                .value(codedata.parentSymbol())
                .type(Property.ValueType.IDENTIFIER)
                .stepOut()
                .addProperty(Property.CONNECTION_KEY);
    }

    private void setCustomProperties(Collection<ParameterResult> functionParameters) {
        boolean hasOnlyRestParams = functionParameters.size() == 1;
        for (ParameterResult paramResult :functionParameters) {
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
                case REQUIRED -> customPropBuilder.type(Property.ValueType.EXPRESSION)
                        .value(paramResult.defaultValue());
                default -> customPropBuilder.type(Property.ValueType.EXPRESSION);
            }

            customPropBuilder
                    .stepOut()
                    .addProperty(unescapedParamName);
        }
    }

    private void setReturnTypeProperties(String returnTypeName, TemplateContext context) {
        boolean editable = false;
        if (returnTypeName.contains(RemoteActionCallBuilder.TARGET_TYPE_KEY)) {
            returnTypeName = returnTypeName.replace(RemoteActionCallBuilder.TARGET_TYPE_KEY, "json");
            editable = true;
        }
        properties()
                .type(returnTypeName, editable)
                .data(returnTypeName, context.getAllVisibleSymbolNames(), Property.VARIABLE_NAME);
    }

    private ObjectTypeSymbol getObjectTypeSymbol(VariableSymbol varSymbol) {
        TypeSymbol rawType = CommonUtil.getRawType(varSymbol.typeDescriptor());
        if (!(rawType instanceof ObjectTypeSymbol objectTypeSymbol)) {
            throw new RuntimeException("Invalid object type: " + rawType);
        }
        return objectTypeSymbol;
    }

    private VariableSymbol findVariableSymbol(SemanticModel semanticModel, Document document,
                                              LinePosition position, String exprSymbol) {

        Optional<VariableSymbol> varSymbol = semanticModel.visibleSymbols(document, position)
                .stream()
                .filter(symbol -> symbol.getName().orElse("").equals(exprSymbol))
                .filter(symbol -> symbol.kind() == SymbolKind.VARIABLE)
                .map(symbol -> (VariableSymbol) symbol)
                .findFirst();

        if (varSymbol.isEmpty()) {
            throw new RuntimeException("Variable not found: " + exprSymbol);
        }
        return varSymbol.get();
    }
}
