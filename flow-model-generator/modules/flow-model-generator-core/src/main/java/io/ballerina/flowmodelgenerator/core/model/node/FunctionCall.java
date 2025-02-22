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
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
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
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a function call node.
 *
 * @since 2.0.0
 */
public class FunctionCall extends FunctionBuilder {

    @Override
    public void setConcreteConstData() {
        codedata().node(NodeKind.FUNCTION_CALL);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        if (isLocalFunction(context.workspaceManager(), context.filePath(), codedata)) {
            handleLocalFunction(context, codedata);
        } else {
            handleImportedFunction(context, codedata);
        }
    }

    private void handleImportedFunction(TemplateContext context, Codedata codedata) {
        Optional<FunctionResult> functionResult = getFunctionResult(codedata, DatabaseManager.FunctionKind.FUNCTION);

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

        setCustomProperties(function);

        String returnTypeName = function.returnType();
        if (CommonUtils.hasReturn(function.returnType())) {
            setReturnTypeProperties(returnTypeName, context, function.inferredReturnType());
        }

        if (function.returnError()) {
            properties().checkError(true);
        }
    }

    private void handleLocalFunction(TemplateContext context, Codedata codedata) {
        try {
            WorkspaceManager workspaceManager = context.workspaceManager();
            Project project = workspaceManager.loadProject(context.filePath());

            SemanticModel semanticModel = workspaceManager.semanticModel(context.filePath()).orElseThrow();
            Optional<Symbol> outSymbol = semanticModel.moduleSymbols().stream()
                    .filter(symbol -> symbol.kind() == SymbolKind.FUNCTION && symbol.nameEquals(codedata.symbol()))
                    .findFirst();
            if (outSymbol.isEmpty()) {
                throw new RuntimeException("Function not found: " + codedata.symbol());
            }

            FunctionSymbol functionSymbol = (FunctionSymbol) outSymbol.get();
            FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();

            List<ParameterResult> parameters = ParamUtils.buildFunctionParamResultMap(functionSymbol, semanticModel).values()
                    .stream().toList();

            String returnTypeName;
            Optional<TypeSymbol> returnType = functionTypeSymbol.returnTypeDescriptor();
            if (returnType.isPresent()) {
                returnTypeName = CommonUtils.getTypeSignature(semanticModel, returnType.get(), true, null);
                setReturnTypeProperties(returnTypeName, context, false);
            } else {
                returnTypeName = null;
            }

            String packageName = project.currentPackage().packageName().toString();
            String org = project.currentPackage().packageOrg().toString();
            String version = project.currentPackage().packageVersion().toString();

            String description = functionSymbol.documentation()
                    .flatMap(Documentation::description)
                    .orElse("");

            FunctionResult function = new FunctionResult(
                    0, // local functions don't have an ID
                    codedata.symbol(),
                    description,
                    returnTypeName,
                    packageName,
                    org,
                    version,
                    "", // no resource path for local functions
                    FunctionResult.Kind.FUNCTION,
                    containsErrorInReturnType(semanticModel, functionTypeSymbol),
                    false
            );
            function.setParameters(parameters);

            metadata().label(function.name());
            codedata()
                    .node(NodeKind.FUNCTION_CALL)
                    .symbol(function.name());

            setCustomProperties(function);

            if (containsErrorInReturnType(semanticModel, functionTypeSymbol)
                && FlowNodeUtil.withinDoClause(context)) {
                properties().checkError(true);
            }
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException("Error loading project: " + e.getMessage(), e);
        }
    }

    @Override
    protected Map<Path, List<TextEdit>> buildFunctionCall(SourceBuilder sourceBuilder, FlowNode flowNode) {
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

    protected static boolean containsErrorInReturnType(SemanticModel semanticModel,
                                                       FunctionTypeSymbol functionTypeSymbol) {
        TypeSymbol errorTypeSymbol = semanticModel.types().ERROR;
        return functionTypeSymbol.returnTypeDescriptor()
                .map(returnTypeDesc -> CommonUtils.subTypeOf(returnTypeDesc, errorTypeSymbol)).orElse(false);
    }
}
