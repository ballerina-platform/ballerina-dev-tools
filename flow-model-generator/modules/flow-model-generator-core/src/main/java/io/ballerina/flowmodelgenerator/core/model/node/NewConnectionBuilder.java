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
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.FunctionResult;
import io.ballerina.modelgenerator.commons.FunctionResultBuilder;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a new connection node in the flow model.
 *
 * @since 2.0.0
 */
public class NewConnectionBuilder extends FunctionBuilder {

    private static final String NEW_CONNECTION_LABEL = "New Connection";

    public static final String INIT_SYMBOL = "init";
    public static final String CLIENT_SYMBOL = "Client";
    public static final String CHECK_ERROR_DOC = "Terminate on error";
    public static final String CONNECTION_NAME_LABEL = "Connection Name";
    public static final String CONNECTION_TYPE_LABEL = "Connection Type";
    protected static final Logger LOG = LoggerFactory.getLogger(NewConnectionBuilder.class);

    @Override
    public void setConcreteConstData() {
        metadata().label(NEW_CONNECTION_LABEL);
        codedata().node(NodeKind.NEW_CONNECTION).symbol(INIT_SYMBOL);
    }

    @Override
    protected Map<Path, List<TextEdit>> buildFunctionCall(SourceBuilder sourceBuilder, FlowNode flowNode) {
        sourceBuilder
                .token().keyword(SyntaxKind.FINAL_KEYWORD).stepOut()
                .newVariable();

        sourceBuilder.token()
                .keyword(SyntaxKind.CHECK_KEYWORD)
                .keyword(SyntaxKind.NEW_KEYWORD)
                .stepOut()
                .functionParameters(sourceBuilder.flowNode,
                        Set.of(Property.VARIABLE_KEY, Property.TYPE_KEY, Property.SCOPE_KEY,
                                Property.CHECK_ERROR_KEY));

        Optional<Property> scope = sourceBuilder.flowNode.getProperty(Property.SCOPE_KEY);
        if (scope.isEmpty()) {
            throw new IllegalStateException("Scope is not defined for the new connection node");
        }
        return switch (scope.get().value().toString()) {
            case Property.LOCAL_SCOPE -> sourceBuilder.textEdit(false).acceptImport().build();
            case Property.GLOBAL_SCOPE -> sourceBuilder.textEdit(false, "connections.bal", true).build();
            default -> throw new IllegalStateException("Invalid scope for the new connection node");
        };
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        FunctionResult function;

        if (Boolean.TRUE.equals(codedata.isGenerated())) {
            function = buildGeneratedFunctionResult(context);
        } else {
            FunctionResultBuilder functionResultBuilder = new FunctionResultBuilder()
                    .name(codedata.symbol())
                    .moduleInfo(new ModuleInfo(codedata.org(), codedata.module(), codedata.module(), codedata.version()))
                    .functionResultKind(FunctionResult.Kind.CONNECTOR)
                    .userModuleInfo(moduleInfo);
            function = functionResultBuilder.build();
        }

        metadata()
                .label(function.packageName())
                .description(function.description())
                .icon(CommonUtils.generateIcon(function.org(), function.packageName(), function.version()));
        codedata()
                .node(NodeKind.NEW_CONNECTION)
                .org(function.org())
                .module(function.packageName())
                .object(CLIENT_SYMBOL)
                .symbol(INIT_SYMBOL)
                .id(function.functionId())
                .isGenerated(codedata.isGenerated());

        setParameterProperties(function);

        if (CommonUtils.hasReturn(function.returnType())) {
            setReturnTypeProperties(function.returnType(), context, false, CONNECTION_NAME_LABEL);
        }

        properties()
                .scope(Property.GLOBAL_SCOPE)
                .checkError(true, CHECK_ERROR_DOC, false);
    }

    private FunctionResult buildGeneratedFunctionResult(TemplateContext context) {
        Path projectPath = context.filePath().getParent();
        if (projectPath == null) {
            throw new IllegalStateException("Project path not found");
        }

        Codedata codedata = context.codedata();
        Path clientPath = projectPath.resolve("generated").resolve(codedata.module()).resolve("client.bal");

        try {
            WorkspaceManager workspaceManager = context.workspaceManager();
            workspaceManager.loadProject(clientPath);

            Optional<SemanticModel> optSemanticModel = workspaceManager.semanticModel(clientPath);
            if (optSemanticModel.isEmpty()) {
                throw new IllegalStateException("Semantic model not found");
            }

            ClassSymbol clientClass = findClientClass(optSemanticModel.get());
            MethodSymbol initMethod = clientClass.initMethod()
                    .orElseThrow(() -> new IllegalStateException("Init method not found"));

            return new FunctionResultBuilder()
                    .semanticModel(optSemanticModel.get())
                    .name(initMethod.getName().orElse(INIT_SYMBOL))
                    .moduleInfo(
                            new ModuleInfo(codedata.org(), codedata.module(), codedata.module(), codedata.version()))
                    .functionSymbol(initMethod)
                    .functionResultKind(FunctionResult.Kind.CONNECTOR)
                    .build();
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException("Error loading generated client", e);
        }
    }

    private ClassSymbol findClientClass(SemanticModel semanticModel) {
        return semanticModel.moduleSymbols().stream()
                .filter(symbol -> symbol.kind() == SymbolKind.CLASS)
                .map(symbol -> (ClassSymbol) symbol)
                .filter(classSymbol -> classSymbol.qualifiers().contains(Qualifier.CLIENT))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Client class not found"));
    }
}
