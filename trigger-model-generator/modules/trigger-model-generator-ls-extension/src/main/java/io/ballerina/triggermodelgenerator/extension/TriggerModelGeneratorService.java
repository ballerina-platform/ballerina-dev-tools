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

package io.ballerina.triggermodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;
import io.ballerina.triggermodelgenerator.extension.model.Service;
import io.ballerina.triggermodelgenerator.extension.model.Trigger;
import io.ballerina.triggermodelgenerator.extension.model.TriggerBasicInfo;
import io.ballerina.triggermodelgenerator.extension.model.TriggerProperty;
import io.ballerina.triggermodelgenerator.extension.model.Value;
import io.ballerina.triggermodelgenerator.extension.request.TriggerFunctionRequest;
import io.ballerina.triggermodelgenerator.extension.request.TriggerListRequest;
import io.ballerina.triggermodelgenerator.extension.request.TriggerModelGenRequest;
import io.ballerina.triggermodelgenerator.extension.request.TriggerModifierRequest;
import io.ballerina.triggermodelgenerator.extension.request.TriggerRequest;
import io.ballerina.triggermodelgenerator.extension.request.TriggerSourceRequest;
import io.ballerina.triggermodelgenerator.extension.response.TriggerCommonResponse;
import io.ballerina.triggermodelgenerator.extension.response.TriggerListResponse;
import io.ballerina.triggermodelgenerator.extension.response.TriggerModelGenResponse;
import io.ballerina.triggermodelgenerator.extension.response.TriggerResponse;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.ballerina.triggermodelgenerator.extension.Utils.expectsTriggerByName;
import static io.ballerina.triggermodelgenerator.extension.Utils.filterTriggers;
import static io.ballerina.triggermodelgenerator.extension.Utils.getFunction;
import static io.ballerina.triggermodelgenerator.extension.Utils.getFunctionSignature;
import static io.ballerina.triggermodelgenerator.extension.Utils.getListenerExpression;
import static io.ballerina.triggermodelgenerator.extension.Utils.getServiceDeclarationNode;
import static io.ballerina.triggermodelgenerator.extension.Utils.importExists;
import static io.ballerina.triggermodelgenerator.extension.Utils.populateProperties;
import static io.ballerina.triggermodelgenerator.extension.Utils.updateTriggerModel;

/**
 * Represents the extended language server service for the trigger model generator service.
 *
 * @since 2.0.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("triggerDesignService")
public class TriggerModelGeneratorService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;
    private final Map<String, TriggerProperty> triggerProperties;
    private static Type propertyMapType = new TypeToken<Map<String, TriggerProperty>>() { }.getType();

    public TriggerModelGeneratorService() {
        InputStream propertiesStream = getClass().getClassLoader()
                .getResourceAsStream("triggers/properties.json");
        Map<String, TriggerProperty> triggerProperties = Map.of();
        if (propertiesStream != null) {
            try (JsonReader reader = new JsonReader(new InputStreamReader(propertiesStream, StandardCharsets.UTF_8))) {
                triggerProperties = new Gson().fromJson(reader, propertyMapType);
            } catch (IOException e) {
                // Ignore
            }
        }
        this.triggerProperties = triggerProperties;
    }

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    @JsonRequest
    public CompletableFuture<TriggerListResponse> getTriggerModels(TriggerListRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            List<TriggerBasicInfo> triggerBasicInfoList = triggerProperties.values().stream()
                    .filter(triggerProperty -> filterTriggers(triggerProperty, request))
                    .map(trigger -> getTriggerBasicInfoByName(trigger.name()))
                    .flatMap(Optional::stream)
                    .toList();
            return new TriggerListResponse(triggerBasicInfoList);
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerResponse> getTriggerModel(TriggerRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (expectsTriggerByName(request)) {
                return new TriggerResponse(getTriggerByName(request.packageName()).orElse(null));
            }

            TriggerProperty triggerProperty = triggerProperties.get(request.id());
            if (triggerProperty == null) {
                return new TriggerResponse();
            }
            return new TriggerResponse(getTriggerByName(triggerProperty.name()).orElse(null));
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerModelGenResponse> getTriggerModelFromCode(TriggerModelGenRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new TriggerModelGenResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.codedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
                    return new TriggerModelGenResponse();
                }
                ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
                Optional<String> triggerName = getTriggerName(serviceNode);
                if (triggerName.isEmpty()) {
                    return new TriggerModelGenResponse();
                }
                Optional<Trigger> trigger = getTriggerByName(triggerName.get());
                if (trigger.isEmpty()) {
                    return new TriggerModelGenResponse();
                }
                updateTriggerModel(trigger.get(), serviceNode);
                return new TriggerModelGenResponse(trigger.get());
            } catch (Throwable e) {
                return new TriggerModelGenResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerCommonResponse> getSourceCode(TriggerSourceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new TriggerCommonResponse();
                }
                ModulePartNode node = document.get().syntaxTree().rootNode();
                LineRange lineRange = node.lineRange();

                Trigger trigger = request.trigger();
                Service service = trigger.getService();
                if (!service.isEnabled()) {
                    return new TriggerCommonResponse();
                }
                populateProperties(trigger);
                String serviceDeclaration = getServiceDeclarationNode(trigger, service);
                TextEdit serviceEdit = new TextEdit(Utils.toRange(lineRange.endLine()),
                        System.lineSeparator() + serviceDeclaration);
                if (!importExists(node, trigger.getOrgName(), trigger.getModuleName())) {
                    String importText = String.format("%simport %s/%s;%s", System.lineSeparator(), trigger.getOrgName(),
                            trigger.getModuleName(), System.lineSeparator());
                    TextEdit importEdit = new TextEdit(Utils.toRange(lineRange.startLine()), importText);
                    edits.add(importEdit);
                }
                edits.add(serviceEdit);
                return new TriggerCommonResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new TriggerCommonResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerCommonResponse> addTriggerFunction(TriggerFunctionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new TriggerCommonResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.codedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
                    return new TriggerCommonResponse();
                }
                ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
                LineRange functionLineRange = serviceNode.openBraceToken().lineRange();
                NodeList<Node> members = serviceNode.members();
                if (!members.isEmpty()) {
                    functionLineRange = members.get(members.size() - 1).lineRange();
                }
                String functionNode = "\n\t" + getFunction(request.function()).replace(System.lineSeparator(),
                        System.lineSeparator() + "\t");
                TextEdit functionEdit = new TextEdit(Utils.toRange(functionLineRange.endLine()), functionNode);
                edits.add(functionEdit);
                return new TriggerCommonResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new TriggerCommonResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerCommonResponse> updateTriggerFunction(TriggerFunctionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new TriggerCommonResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.function().getCodedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (!(node instanceof FunctionDefinitionNode functionDefinitionNode)) {
                    return new TriggerCommonResponse();
                }
                LineRange functionLineRange = functionDefinitionNode.functionSignature().lineRange();
                String functionSignature = getFunctionSignature(request.function());
                TextEdit functionEdit = new TextEdit(Utils.toRange(functionLineRange), functionSignature);
                edits.add(functionEdit);
                return new TriggerCommonResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new TriggerCommonResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerCommonResponse> updateTrigger(TriggerModifierRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Trigger trigger = request.trigger();
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new TriggerCommonResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.codedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
                    return new TriggerCommonResponse();
                }
                ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
                Optional<ExpressionNode> listenerExpression = getListenerExpression(serviceNode);
                if (listenerExpression.isEmpty() || !(listenerExpression.get() instanceof NewExpressionNode listener)) {
                    return new TriggerCommonResponse();
                }
                Optional<Value> displayAnnotation = trigger.getSvcDisplayAnnotationProperty();
                if (displayAnnotation.isPresent()) {
                    LineRange labelValueLineRange = displayAnnotation.get().getCodedata().getLineRange();
                    TextEdit labelValueEdit = new TextEdit(Utils.toRange(labelValueLineRange),
                            Utils.getValueString(displayAnnotation.get()));
                    edits.add(labelValueEdit);
                }
                Optional<String> basePath = trigger.getBasePath();
                if (basePath.isPresent()) {
                    NodeList<Node> nodes = serviceNode.absoluteResourcePath();
                    if (!nodes.isEmpty() && nodes.size() == 1) {
                        LineRange basePathLineRange = nodes.get(0).lineRange();
                        TextEdit basePathEdit = new TextEdit(Utils.toRange(basePathLineRange), basePath.get());
                        edits.add(basePathEdit);
                    }
                }
                LineRange listenerLineRange = listener.lineRange();
                String listenerDeclaration = trigger.getListenerDeclaration();
                TextEdit listenerEdit = new TextEdit(Utils.toRange(listenerLineRange), listenerDeclaration);
                edits.add(listenerEdit);
                return new TriggerCommonResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new TriggerCommonResponse(e);
            }
        });
    }

    public Optional<String> getTriggerName(ServiceDeclarationNode serviceNode) {
        Optional<ExpressionNode> expressionNode = getListenerExpression(serviceNode);
        if (expressionNode.isEmpty()) {
            return Optional.empty();
        }
        if (!(expressionNode.get() instanceof ExplicitNewExpressionNode explicitNewExpressionNode)) {
            return Optional.empty();
        }
        TypeDescriptorNode typeDescriptorNode = explicitNewExpressionNode.typeDescriptor();
        if (!(typeDescriptorNode instanceof QualifiedNameReferenceNode qualifiedNameReferenceNode)) {
            return Optional.empty();
        }
        return Optional.of(qualifiedNameReferenceNode.modulePrefix().text());
    }

    private Optional<TriggerBasicInfo> getTriggerBasicInfoByName(String name) {
        if (triggerProperties.values().stream().noneMatch(trigger -> trigger.name().equals(name))) {
            return Optional.empty();
        }
        InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(String.format("triggers/%s.json", name));
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, TriggerBasicInfo.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Optional<Trigger> getTriggerByName(String name) {
        if (triggerProperties.values().stream().noneMatch(trigger -> trigger.name().equals(name))) {
            return Optional.empty();
        }
        InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(String.format("triggers/%s.json", name));
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Trigger.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
