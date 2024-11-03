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

import com.google.gson.Gson;
import io.ballerina.compiler.api.SemanticModel;
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
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
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

    private static final Gson gson = new Gson();

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
                    boolean optional = param.paramKind() != ParameterKind.REQUIRED;
                    properties().custom(name.get(), name.get(), "", Property.ValueType.EXPRESSION,
                            param.typeDescriptor().signature(),
                            DefaultValueGenerationUtil.getDefaultValueForType(param.typeDescriptor()).orElse(""),
                            optional, optional);
                }
            }

            functionTypeSymbol.returnTypeDescriptor().ifPresent(returnType -> {
                properties().type(CommonUtils.getTypeSignature(semanticModel, returnType, true)).data(null);
            });
            properties().dataVariable(null);
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
            boolean optional = isOptional(paramResult, codedata.org(), codedata.module(), codedata.version());
            properties().custom(paramResult.name(), paramResult.name(), paramResult.description(),
                    Property.ValueType.EXPRESSION, paramResult.type(), "", optional, optional);
        }

        if (TypeUtils.hasReturn(function.returnType())) {
            properties().type(function.returnType()).data(null);
        }
    }

    private boolean isOptional(ParameterResult param, String orgName, String moduleName, String versionName) {
        if (param.kind() == ParameterKind.DEFAULTABLE) {
            return true;
        }
        if (param.kind() != ParameterKind.INCLUDED_RECORD) {
            return false;
        }

        Package modulePackage = PackageUtil.getModulePackage(orgName, moduleName, versionName);
        Iterable<Module> modules = modulePackage.modules();
        for (Module module : modules) {
            SemanticModel modSemanticModel = module.getCompilation().getSemanticModel();
            List<Symbol> symbols = modSemanticModel.moduleSymbols();
            for (Symbol symbol : symbols) {
                Optional<String> optSymbolName = symbol.getName();
                if (optSymbolName.isEmpty()) {
                    continue;
                }
                if (foundSymbol(optSymbolName.get(), param.type())) {
                    return allFieldsOptional(symbol);
                }
            }
        }
        return false;
    }

    private boolean allFieldsOptional(Symbol symbol) {
        RecordTypeSymbol recordType = getRecordType(symbol);
        if (recordType == null) {
            return false;
        }

        for (Map.Entry<String, RecordFieldSymbol> entry : recordType.fieldDescriptors().entrySet()) {
            if (!entry.getValue().isOptional()) {
                return false;
            }
        }

        List<TypeSymbol> typeSymbols = recordType.typeInclusions();
        for (TypeSymbol typeSymbol : typeSymbols) {
            if (!allFieldsOptional(typeSymbol)) {
                return false;
            }
        }

        return true;
    }

    private RecordTypeSymbol getRecordType(Symbol symbol) {
        if (symbol.kind() == SymbolKind.TYPE_DEFINITION) {
            return getRecordType(((TypeDefinitionSymbol) symbol).typeDescriptor());
        } else if (symbol.kind() == SymbolKind.TYPE) {
            if (((TypeSymbol) symbol).typeKind() == TypeDescKind.RECORD) {
                return (RecordTypeSymbol) symbol;
            }
        }
        return null;
    }

    private boolean foundSymbol(String symbol, String param) {
        String[] split = param.split(":");
        return split[split.length - 1].equals(symbol);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();
        FlowNode flowNode = sourceBuilder.flowNode;

        // TODO: Make this condition and once we get the correct flag using index
        if (flowNode.hasFlag(FlowNode.NODE_FLAG_CHECKED)
                || CommonUtils.withinDoClause(sourceBuilder.workspaceManager, sourceBuilder.filePath,
                flowNode.codedata().lineRange())) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        Codedata codedata = flowNode.codedata();
        if (isLocalFunction(sourceBuilder.workspaceManager, sourceBuilder.filePath, codedata)) {
            return sourceBuilder.token()
                    .name(codedata.symbol())
                    .stepOut()
                    .functionParameters(flowNode, Set.of("variable", "type", "view"))
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
                .functionParameters(flowNode, Set.of("variable", "type", "view"))
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
