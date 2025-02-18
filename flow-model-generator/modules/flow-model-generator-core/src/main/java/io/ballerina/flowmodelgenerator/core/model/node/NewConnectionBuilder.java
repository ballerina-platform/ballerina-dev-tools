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
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.flowmodelgenerator.core.db.DatabaseManager;
import io.ballerina.flowmodelgenerator.core.db.model.Function;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.db.model.Parameter;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a new connection node in the flow model.
 *
 * @since 2.0.0
 */
public class NewConnectionBuilder extends NodeBuilder {

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
        codedata().node(NodeKind.NEW_CONNECTION).symbol("init");
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
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
        FunctionResult function = null;
        List<ParameterResult> functionParameters = null;
        if (codedata.isGenerated() != null && codedata.isGenerated()) {
            Path projectPath = context.filePath().getParent();
            if (projectPath == null) {
                return;
            }
            Path clientPath = projectPath.resolve("generated").resolve(codedata.module()).resolve("client.bal");
            try {
                if (clientPath.toFile().exists()) {
                    LOG.info("Loading client file from: " + clientPath);
                } else {
                    LOG.info("Client file does not exist: " + clientPath);
                }
                WorkspaceManager workspaceManager = context.workspaceManager();
                try {
                    workspaceManager.loadProject(clientPath);
                } catch (WorkspaceDocumentException e) {
                    LOG.error("Error loading project: " + clientPath, e);
                    throw new RuntimeException(e);
                }
                Optional<SemanticModel> optSemanticModel = workspaceManager.semanticModel(clientPath);
                if (optSemanticModel.isEmpty()) {
                    return;
                }
                for (Symbol symbol : optSemanticModel.get().moduleSymbols()) {
                    if (symbol.kind() != SymbolKind.CLASS) {
                        continue;
                    }
                    ClassSymbol classSymbol = (ClassSymbol) symbol;
                    if (!classSymbol.qualifiers().contains(Qualifier.CLIENT)) {
                        continue;
                    }
                    Optional<MethodSymbol> optInitMethodSymbol = classSymbol.initMethod();
                    if (optInitMethodSymbol.isEmpty()) {
                        continue;
                    }
                    MethodSymbol methodSymbol = optInitMethodSymbol.get();
                    function = convertMethodSymbolToFunctionResult(methodSymbol, codedata.module(),
                            classSymbol.getName().get());
                    functionParameters = getParametersFromMethodSymbol(workspaceManager, methodSymbol, clientPath);
                    break;
                }
            } catch (EventSyncException e) {
                throw new RuntimeException(e);
            }
            if (function == null) {
                return;
            }
        } else {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            Optional<FunctionResult> optFunctionResult = codedata.id() != null ? dbManager.getFunction(codedata.id()) :
                    dbManager.getFunction(codedata.org(), codedata.module(), codedata.symbol(),
                            DatabaseManager.FunctionKind.CONNECTOR);
            if (optFunctionResult.isEmpty()) {
                return;
            }
            function = optFunctionResult.get();
            functionParameters = dbManager.getFunctionParameters(function.functionId());
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
            } else {
                customPropBuilder.type(Property.ValueType.EXPRESSION);
            }
            customPropBuilder
                    .stepOut()
                    .addProperty(unescapedParamName);
        }

        if (CommonUtils.hasReturn(function.returnType())) {
            properties()
                    .type(function.returnType(), false)
                    .data(function.returnType(), context.getAllVisibleSymbolNames(), CONNECTION_NAME_LABEL);
        }
        properties()
                .scope(Property.GLOBAL_SCOPE)
                .checkError(true, CHECK_ERROR_DOC, false);
    }

    private FunctionResult convertMethodSymbolToFunctionResult(MethodSymbol methodSymbol, String module, String name) {
        String retType = module + ":" + name;
        String description = "";
        Optional<Documentation> documentation = methodSymbol.documentation();
        if (documentation.isPresent()) {
            Optional<String> optDescription = documentation.get().description();
            if (optDescription.isPresent()) {
                description = optDescription.get();
            }
        }

        return new FunctionResult(-1, methodSymbol.getName().orElse(""), description, retType, module, "", "", "",
                Function.Kind.CONNECTOR, false, false);
    }

    private List<ParameterResult> getParametersFromMethodSymbol(WorkspaceManager workspaceManager,
                                                                MethodSymbol methodSymbol, Path filePath) {
        List<ParameterResult> parameterResults = new ArrayList<>();
        Optional<List<ParameterSymbol>> optParams = methodSymbol.typeDescriptor().params();
        if (optParams.isEmpty()) {
            return parameterResults;
        }

        Optional<Location> optLocation = methodSymbol.getLocation();
        Map<String, String> defaultValues = new HashMap<>();
        if (optLocation.isPresent()) {
            defaultValues = getDefaultValues(workspaceManager, filePath, optLocation.get().textRange());
        }
        List<ParameterSymbol> paramSymbols = optParams.get();
        for (int i = 0; i < paramSymbols.size(); i++) {
            ParameterSymbol paramSymbol = paramSymbols.get(i);
            Optional<String> optParamName = paramSymbol.getName();
            String paramName = optParamName.orElse("param" + i);
            TypeSymbol paramType = paramSymbol.typeDescriptor();
            String type = CommonUtils.getTypeSignature(semanticModel, paramType, true);
            parameterResults.add(new ParameterResult(i, paramName, type, getParamKind(paramSymbol.paramKind()),
                    defaultValues.getOrDefault(paramName, ""), "", false, ""));
        }
        return parameterResults;
    }

    private Map<String, String> getDefaultValues(WorkspaceManager workspaceManager, Path file,
                                                 TextRange functionLocation) {
        Optional<Document> document = workspaceManager.document(file);
        Map<String, String> defaultValues = new HashMap<>();
        if (document.isEmpty()) {
            return defaultValues;
        }
        ModulePartNode modulePartNode = document.get().syntaxTree().rootNode();
        NonTerminalNode node = modulePartNode.findNode(functionLocation);
        if (node.kind() != SyntaxKind.OBJECT_METHOD_DEFINITION) {
            return defaultValues;
        }
        FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
        functionDefinitionNode.functionSignature().parameters().forEach(parameter -> {
            if (parameter instanceof DefaultableParameterNode defaultableParameterNode) {
                Optional<Token> optParamName = defaultableParameterNode.paramName();
                optParamName.ifPresent(token -> defaultValues.put(token.text(),
                        defaultableParameterNode.expression().toString()));
            }
        });
        return defaultValues;
    }

    private Parameter.Kind getParamKind(ParameterKind kind) {
        return switch (kind) {
            case DEFAULTABLE -> Parameter.Kind.DEFAULTABLE;
            case INCLUDED_RECORD -> Parameter.Kind.INCLUDED_RECORD;
            case REST -> Parameter.Kind.REST_PARAMETER;
            default -> Parameter.Kind.REQUIRED;
        };
    }
}
