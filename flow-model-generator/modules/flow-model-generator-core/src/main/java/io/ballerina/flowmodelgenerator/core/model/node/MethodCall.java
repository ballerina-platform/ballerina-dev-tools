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
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.db.DatabaseManager;
import io.ballerina.modelgenerator.commons.FunctionResult;
import io.ballerina.modelgenerator.commons.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a method call.
 *
 * @since 2.0.0
 */
public class MethodCall extends FunctionBuilder {

    @Override
    public void setConcreteConstData() {
        codedata().node(NodeKind.METHOD_CALL);
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
        Optional<FunctionResult> functionResult = getFunctionResult(codedata, DatabaseManager.FunctionKind.FUNCTION);

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
        setCustomProperties(function);

        String returnTypeName = function.returnType();
        if (CommonUtils.hasReturn(function.returnType())) {
            setReturnTypeProperties(returnTypeName, context, function.inferredReturnType());
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

        FunctionTypeSymbol functionTypeSymbol = methodSymbol.typeDescriptor();
        List<ParameterResult> parameters = ParamUtils.buildFunctionParamResultMap(methodSymbol, semanticModel).values()
                .stream().toList();

        String returnTypeName;
        Optional<TypeSymbol> returnType = functionTypeSymbol.returnTypeDescriptor();
        if (returnType.isPresent()) {
            returnTypeName = CommonUtils.getTypeSignature(semanticModel, returnType.get(), true, moduleInfo);
            setReturnTypeProperties(returnTypeName, context, false);
        } else {
            returnTypeName = null;
        }

        String packageName = project.currentPackage().packageName().toString();
        String org = project.currentPackage().packageOrg().toString();
        String version = project.currentPackage().packageVersion().toString();

        String description = methodSymbol.documentation()
                .flatMap(Documentation::description)
                .orElse("");

        FunctionResult function = new FunctionResult(
                0, // local methods don't have an ID
                codedata.symbol(),
                description,
                returnTypeName,
                packageName,
                org,
                version,
                "", // no resource path for local methods
                FunctionResult.Kind.FUNCTION,
                containsErrorInReturnType(semanticModel, functionTypeSymbol),
                false
        );
        function.setParameters(parameters);

        metadata().label(function.name());
        codedata()
                .node(NodeKind.METHOD_CALL)
                .symbol(function.name());

        setExpressionProperty(codedata);
        setCustomProperties(function);

        if (containsErrorInReturnType(semanticModel, functionTypeSymbol)
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

    @Override
    protected Map<Path, List<TextEdit>> buildFunctionCall(SourceBuilder sourceBuilder, FlowNode flowNode) {
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
