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
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.TypeUtils;
import io.ballerina.flowmodelgenerator.core.db.DatabaseManager;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.PackageUtil;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.common.utils.DefaultValueGenerationUtil;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
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
                workspaceManager.loadProject(context.filePath());
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

            Optional<List<ParameterSymbol>> params = functionTypeSymbol.params();
            if (params.isPresent()) {
                for (ParameterSymbol param : params.get()) {
                    Optional<String> name = param.getName();
                    if (name.isEmpty()) {
                        continue;
                    }
                    properties().custom()
                            .metadata()
                                .label(name.get())
                                .description("")
                                .stepOut()
                            .type(Property.ValueType.EXPRESSION)
                            .typeConstraint(CommonUtils.getTypeSignature(param.typeDescriptor(), moduleDescriptor))
                            .value(DefaultValueGenerationUtil.getDefaultValueForType(param.typeDescriptor()).orElse(""))
                            .defaultable(param.paramKind() == ParameterKind.DEFAULTABLE)
                            .editable()
                            .stepOut()
                            .addProperty(name.get());
                }
            }

            functionTypeSymbol.returnTypeDescriptor().ifPresent(returnType -> {
                String returnTypeName = CommonUtils.getTypeSignature(semanticModel, returnType, true);
                boolean editable = false;
                if (returnTypeName.contains(ActionCall.TARGET_TYPE_KEY)) {
                    returnTypeName = returnTypeName.replace(ActionCall.TARGET_TYPE_KEY, "json");
                    editable = true;
                }
                properties()
                        .type(returnTypeName, editable)
                        .data(returnTypeName, context.getAllVisibleSymbolNames(), Property.DATA_VARIABLE_LABEL);
            });
            TypeSymbol errorTypeSymbol = semanticModel.types().ERROR;
            int returnError = functionTypeSymbol.returnTypeDescriptor()
                    .map(returnTypeDesc -> returnTypeDesc.subtypeOf(errorTypeSymbol) ? 1 : 0).orElse(0);
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
        for (ParameterResult paramResult : functionParameters) {
            if (paramResult.kind() == ParameterKind.INCLUDED_RECORD) {
                Package modulePackage = PackageUtil
                        .getModulePackage(function.org(), function.packageName(), function.version());
                SemanticModel pkgModel = modulePackage.getDefaultModule().getCompilation().getSemanticModel();
                Optional<Symbol> includedRecordType = pkgModel.moduleSymbols().stream()
                        .filter(symbol -> symbol.nameEquals(paramResult.type().split(":")[1])).findFirst();
                if (includedRecordType.isPresent() && includedRecordType.get() instanceof TypeDefinitionSymbol) {
                    FunctionCall.addIncludedRecordToParams(
                            ((TypeDefinitionSymbol) includedRecordType.get()).typeDescriptor(), this);
                }
            } else {
                properties().custom()
                        .metadata()
                            .label(paramResult.name())
                            .description(paramResult.description())
                            .stepOut()
                        .type(Property.ValueType.EXPRESSION)
                        .typeConstraint(paramResult.type())
                        .value(paramResult.getDefaultValue())
                        .editable()
                        .defaultable(paramResult.kind() == ParameterKind.DEFAULTABLE)
                        .stepOut()
                        .addProperty(paramResult.name());
            }
        }

        String returnTypeName = function.returnType();
        if (TypeUtils.hasReturn(function.returnType())) {
            boolean editable = false;
            if (returnTypeName.contains(ActionCall.TARGET_TYPE_KEY)) {
                returnTypeName = returnTypeName.replace(ActionCall.TARGET_TYPE_KEY, "json");
                editable = true;
            }
            properties()
                    .type(returnTypeName, editable)
                    .data(function.returnType(), context.getAllVisibleSymbolNames(), Property.DATA_VARIABLE_LABEL);
        }

        if (function.returnError() == 1) {
            properties().checkError(true);
        }
    }

    protected static void addIncludedRecordToParams(TypeSymbol typeSymbol, NodeBuilder nodeBuilder) {
        RecordTypeSymbol recordTypeSymbol = (RecordTypeSymbol) CommonUtils.getRawType(typeSymbol);
        recordTypeSymbol.typeInclusions().forEach(includedType -> {
            addIncludedRecordToParams(includedType, nodeBuilder);
        });
        for (Map.Entry<String, RecordFieldSymbol> entry : recordTypeSymbol.fieldDescriptors().entrySet()) {
            RecordFieldSymbol recordFieldSymbol = entry.getValue();
            TypeSymbol fieldType = CommonUtil.getRawType(recordFieldSymbol.typeDescriptor());
            if (fieldType.typeKind() == TypeDescKind.NEVER) {
                continue;
            }
            String attributeName = entry.getKey();
            String doc = entry.getValue().documentation().flatMap(Documentation::description).orElse("");
            nodeBuilder.properties().custom()
                    .metadata()
                        .label(attributeName)
                        .description(doc)
                        .stepOut()
                    .type(Property.ValueType.EXPRESSION)
                    .typeConstraint(recordFieldSymbol.typeDescriptor().getName().orElse(""))
                    .value("")
                    .optional(recordFieldSymbol.hasDefaultValue() || recordFieldSymbol.isOptional())
                    .stepOut()
                    .addProperty(attributeName);
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
                            Set.of(Property.VARIABLE_KEY, Property.DATA_TYPE_KEY, Property.CHECK_ERROR_KEY))
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
                .acceptImport()
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
