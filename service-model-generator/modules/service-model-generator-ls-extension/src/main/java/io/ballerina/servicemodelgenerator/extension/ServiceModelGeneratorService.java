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

package io.ballerina.servicemodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.ModuleName;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.servicemodelgenerator.extension.model.Listener;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.TriggerBasicInfo;
import io.ballerina.servicemodelgenerator.extension.model.TriggerProperty;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.request.CommonModelFromSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.FunctionModifierRequest;
import io.ballerina.servicemodelgenerator.extension.request.FunctionSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerDiscoveryRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerModelRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerModifierRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceModelRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceModifierRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.TriggerListRequest;
import io.ballerina.servicemodelgenerator.extension.request.TriggerRequest;
import io.ballerina.servicemodelgenerator.extension.response.CommonSourceResponse;
import io.ballerina.servicemodelgenerator.extension.response.ListenerDiscoveryResponse;
import io.ballerina.servicemodelgenerator.extension.response.ListenerFromSourceResponse;
import io.ballerina.servicemodelgenerator.extension.response.ListenerModelResponse;
import io.ballerina.servicemodelgenerator.extension.response.ResourceModelResponse;
import io.ballerina.servicemodelgenerator.extension.response.ServiceFromSourceResponse;
import io.ballerina.servicemodelgenerator.extension.response.ServiceModelResponse;
import io.ballerina.servicemodelgenerator.extension.response.TriggerListResponse;
import io.ballerina.servicemodelgenerator.extension.response.TriggerResponse;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.ballerina.servicemodelgenerator.extension.Utils.expectsTriggerByName;
import static io.ballerina.servicemodelgenerator.extension.Utils.filterTriggers;
import static io.ballerina.servicemodelgenerator.extension.Utils.getFunction;
import static io.ballerina.servicemodelgenerator.extension.Utils.getFunctionSignature;
import static io.ballerina.servicemodelgenerator.extension.Utils.getListenerExpression;
import static io.ballerina.servicemodelgenerator.extension.Utils.getPath;
import static io.ballerina.servicemodelgenerator.extension.Utils.getResourceFunctionModel;
import static io.ballerina.servicemodelgenerator.extension.Utils.getServiceDeclarationNode;
import static io.ballerina.servicemodelgenerator.extension.Utils.importExists;
import static io.ballerina.servicemodelgenerator.extension.Utils.isHttpServiceContractType;
import static io.ballerina.servicemodelgenerator.extension.Utils.populateProperties;
import static io.ballerina.servicemodelgenerator.extension.Utils.updateListenerModel;
import static io.ballerina.servicemodelgenerator.extension.Utils.updateServiceContractModel;
import static io.ballerina.servicemodelgenerator.extension.Utils.updateServiceModel;

/**
 * Represents the extended language server service for the trigger model generator service.
 *
 * @since 2.0.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("serviceDesign")
public class ServiceModelGeneratorService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;
    private final Map<String, TriggerProperty> triggerProperties;
    private static final Type propertyMapType = new TypeToken<Map<String, TriggerProperty>>() { }.getType();

    public ServiceModelGeneratorService() {
        InputStream newPropertiesStream = getClass().getClassLoader()
                .getResourceAsStream("trigger_properties.json");
        Map<String, TriggerProperty> newTriggerProperties = Map.of();
        if (newPropertiesStream != null) {
            try (JsonReader reader = new JsonReader(new InputStreamReader(newPropertiesStream,
                    StandardCharsets.UTF_8))) {
                newTriggerProperties = new Gson().fromJson(reader, propertyMapType);
            } catch (IOException e) {
                // Ignore
            }
        }
        this.triggerProperties = newTriggerProperties;
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
    public CompletableFuture<ListenerDiscoveryResponse> getListeners(ListenerDiscoveryRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Package currentPackage = project.currentPackage();
                Module module = currentPackage.module(ModuleName.from(currentPackage.packageName()));
                ModuleId moduleId = module.moduleId();
                SemanticModel semanticModel = currentPackage.getCompilation().getSemanticModel(moduleId);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new ListenerDiscoveryResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                List<String> listeners = getCompatibleListeners(request.moduleName(), modulePartNode,
                        semanticModel);
                return new ListenerDiscoveryResponse(listeners);
            } catch (Throwable e) {
                return new ListenerDiscoveryResponse(e);
            }
        });
    }

    private static List<String> getCompatibleListeners(String moduleName, ModulePartNode modulePartNode,
                                                        SemanticModel semanticModel) {
        return modulePartNode.members().stream()
                .filter(member -> member.kind().equals(SyntaxKind.LISTENER_DECLARATION))
                .map(member -> (ListenerDeclarationNode) member)
                .filter(listener -> getListenerName(listener, semanticModel).isPresent() &&
                        getListenerName(listener, semanticModel).get().equals(moduleName))
                .map(listener -> listener.variableName().text().trim())
                .toList();
    }

    @JsonRequest
    public CompletableFuture<ListenerModelResponse> getListenerModel(ListenerModelRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getListenerByName(request.moduleName())
                        .map(ListenerModelResponse::new)
                        .orElseGet(ListenerModelResponse::new);
            } catch (Throwable e) {
                return new ListenerModelResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<CommonSourceResponse> addListener(ListenerSourceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new CommonSourceResponse();
                }
                ModulePartNode node = document.get().syntaxTree().rootNode();
                LineRange lineRange = node.lineRange();

                Listener listener = request.listener();
                String listenerDeclaration = listener.getDeclaration();
                TextEdit listenerEdit = new TextEdit(Utils.toRange(lineRange.endLine()),
                        System.lineSeparator() + listenerDeclaration);
                if (!importExists(node, listener.getOrgName(), listener.getModuleName())) {
                    String importText = String.format("%simport %s/%s;%s", System.lineSeparator(),
                            listener.getOrgName(), listener.getModuleName(), System.lineSeparator());
                    TextEdit importEdit = new TextEdit(Utils.toRange(lineRange.startLine()), importText);
                    edits.add(importEdit);
                }
                edits.add(listenerEdit);
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<ServiceModelResponse> getServiceModel(ServiceModelRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Service> service = getServiceByName(request.moduleName());
                if (service.isEmpty()) {
                    return new ServiceModelResponse();
                }
                Service serviceModel = service.get();
                Value listener = serviceModel.getListener();
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Package currentPackage = project.currentPackage();
                Module module = currentPackage.module(ModuleName.from(currentPackage.packageName()));
                ModuleId moduleId = module.moduleId();
                SemanticModel semanticModel = currentPackage.getCompilation().getSemanticModel(moduleId);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new ServiceModelResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                List<String> listenersList = getCompatibleListeners(request.moduleName(), modulePartNode,
                        semanticModel);
                if (Objects.nonNull(request.listenerName())) {
                    listener.addValue(request.listenerName());
                    removeAlreadyDefinedServiceTypes(serviceModel, request.listenerName(), modulePartNode);
                }
                if (!listenersList.isEmpty()) {
                    if (request.moduleName().equals("kafka")) {
                        listener.setValueType("SINGLE_SELECT");
                    } else {
                        listener.setValueType("MULTIPLE_SELECT");
                    }
                    listener.setItems(listenersList);
                }
                return new ServiceModelResponse(serviceModel);
            } catch (Throwable e) {
                return new ServiceModelResponse(e);
            }
        });
    }

    private void removeAlreadyDefinedServiceTypes(Service serviceModel, String listenerName,
                                                  ModulePartNode modulePartNode) {
        Value serviceTypeValue = serviceModel.getServiceType();
        if (Objects.isNull(serviceTypeValue) || Objects.isNull(serviceTypeValue.getItems())) {
            return;
        }
        List<ServiceDeclarationNode> services = modulePartNode.members().stream()
                .filter(member -> member.kind().equals(SyntaxKind.SERVICE_DECLARATION))
                .map(member -> (ServiceDeclarationNode) member)
                .toList();
        services.forEach(service -> {
            Optional<TypeDescriptorNode> serviceType = service.typeDescriptor();
            if (serviceType.isEmpty() ||
                    !serviceType.get().kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
                return;
            }
            String serviceTypeName = ((QualifiedNameReferenceNode) serviceType.get()).identifier().text().trim();
            Optional<ExpressionNode> listenerExpression = getListenerExpression(service);
            if (listenerExpression.isEmpty() ||
                    !(listenerExpression.get() instanceof SimpleNameReferenceNode listener)) {
                return;
            }
            if (listener.name().text().trim().equals(listenerName)) {
                serviceTypeValue.getItems().remove(serviceTypeName);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<CommonSourceResponse> addService(ServiceSourceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new CommonSourceResponse();
                }
                ModulePartNode node = document.get().syntaxTree().rootNode();
                LineRange lineRange = node.lineRange();
                Service service = request.service();
                populateProperties(service);

                if (Objects.nonNull(service.getOpenAPISpec())) {
                    Path contractPath = Path.of(service.getOpenAPISpec().getValue());
                    String serviceContractTypeName = service.getServiceContractTypeName();
                    OpenApiServiceGenerator oasSvcGenerator = new OpenApiServiceGenerator(contractPath,
                            project.sourceRoot(), workspaceManager);
                    return new CommonSourceResponse(oasSvcGenerator.generateService(serviceContractTypeName,
                            service.getListener().getValue()));
                }

                String serviceDeclaration = getServiceDeclarationNode(service);
                TextEdit serviceEdit = new TextEdit(Utils.toRange(lineRange.endLine()),
                        System.lineSeparator() + serviceDeclaration);
                if (!importExists(node, service.getOrgName(), service.getModuleName())) {
                    String importText = String.format("%simport %s/%s;%s", System.lineSeparator(), service.getOrgName(),
                            service.getModuleName(), System.lineSeparator());
                    TextEdit importEdit = new TextEdit(Utils.toRange(lineRange.startLine()), importText);
                    edits.add(importEdit);
                }
                edits.add(serviceEdit);
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
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
    public CompletableFuture<ResourceModelResponse> getHttpResourceModel() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getResourceFunctionModel()
                        .map(ResourceModelResponse::new)
                        .orElseGet(ResourceModelResponse::new);
            } catch (Throwable e) {
                return new ResourceModelResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<CommonSourceResponse> addResource(FunctionSourceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new CommonSourceResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.codedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
                    return new CommonSourceResponse();
                }
                ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
                LineRange serviceEnd = serviceNode.closeBraceToken().lineRange();
                List<String> statusCodeResponses = new ArrayList<>();
                String functionDefinition = System.lineSeparator() +
                        "\t" + getFunction(request.function(), statusCodeResponses).replace(System.lineSeparator(),
                        System.lineSeparator() + "\t") + System.lineSeparator();
                List<TextEdit> textEdits = new ArrayList<>();
                textEdits.add(new TextEdit(Utils.toRange(serviceEnd.startLine()), functionDefinition));
                String statusCodeResEdits = statusCodeResponses.stream()
                        .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
                if (!statusCodeResEdits.isEmpty()) {
                    textEdits.add(new TextEdit(Utils.toRange(serviceEnd.endLine()),
                            System.lineSeparator() + statusCodeResEdits));
                }
                return new CommonSourceResponse(Map.of(request.filePath(), textEdits));
            } catch (Exception e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<ServiceFromSourceResponse> getServiceFromSource(CommonModelFromSourceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Path filePath = Path.of(request.filePath());
            Project project;
            try {
                project = this.workspaceManager.loadProject(filePath);
            } catch (Exception e) {
                return new ServiceFromSourceResponse(e);
            }
            Package currentPackage = project.currentPackage();
            Module module = currentPackage.module(ModuleName.from(currentPackage.packageName()));
            ModuleId moduleId = module.moduleId();
            SemanticModel semanticModel = currentPackage.getCompilation().getSemanticModel(moduleId);
            Optional<Document> document = this.workspaceManager.document(filePath);
            if (document.isEmpty() || Objects.isNull(semanticModel)) {
                return new ServiceFromSourceResponse();
            }
            SyntaxTree syntaxTree = document.get().syntaxTree();
            ModulePartNode modulePartNode = syntaxTree.rootNode();
            TextDocument textDocument = syntaxTree.textDocument();
            LineRange lineRange = request.codedata().getLineRange();
            int start = textDocument.textPositionFrom(lineRange.startLine());
            int end = textDocument.textPositionFrom(lineRange.endLine());
            NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
            if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
                return new ServiceFromSourceResponse();
            }
            ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
            Optional<String> serviceName = getServiceName(serviceNode, semanticModel);
            if (serviceName.isEmpty()) {
                return new ServiceFromSourceResponse();
            }
            Optional<Service> service = getServiceByName(serviceName.get());
            if (service.isEmpty()) {
                return new ServiceFromSourceResponse();
            }
            Service serviceModel = service.get();
            Optional<TypeDescriptorNode> serviceTypeDesc = serviceNode.typeDescriptor();
            if (serviceTypeDesc.isPresent() && isHttpServiceContractType(semanticModel, serviceTypeDesc.get())) {
                String serviceContractName = serviceTypeDesc.get().toString().trim();
                Path contractPath = project.sourceRoot().toAbsolutePath()
                        .resolve(String.format("service_contract_%s.bal", serviceContractName));
                Optional<Document> contractDoc = this.workspaceManager.document(contractPath);
                if (contractDoc.isEmpty()) {
                    updateServiceModel(serviceModel, serviceNode, semanticModel);
                } else {
                    SyntaxTree contractSyntaxTree = contractDoc.get().syntaxTree();
                    ModulePartNode contractModulePartNode = contractSyntaxTree.rootNode();
                    Optional<TypeDefinitionNode> serviceContractType = contractModulePartNode.members().stream()
                            .filter(member -> member.kind().equals(SyntaxKind.TYPE_DEFINITION))
                            .map(member -> ((TypeDefinitionNode) member))
                            .filter(member -> member.typeDescriptor().kind().equals(SyntaxKind.OBJECT_TYPE_DESC))
                            .findFirst();
                    if (serviceContractType.isEmpty()) {
                        updateServiceModel(serviceModel, serviceNode, semanticModel);
                    } else {
                        updateServiceContractModel(serviceModel, serviceContractType.get(), serviceNode,
                                semanticModel);
                    }
                }
            } else {
                updateServiceModel(serviceModel, serviceNode, semanticModel);
            }
            List<String> listenersList = getCompatibleListeners(serviceName.get(), modulePartNode,
                    semanticModel);
            Value listener = serviceModel.getListener();
            if (!listenersList.isEmpty()) {
                if (serviceName.get().equals("kafka")) {
                    listener.setValueType("SINGLE_SELECT");
                } else {
                    listener.setValueType("MULTIPLE_SELECT");
                }
                listener.setItems(listenersList);
            }
            return new ServiceFromSourceResponse(serviceModel);
        });
    }

    @JsonRequest
    public CompletableFuture<ListenerFromSourceResponse> getListenerFromSource(CommonModelFromSourceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Package currentPackage = project.currentPackage();
                Module module = currentPackage.module(ModuleName.from(currentPackage.packageName()));
                ModuleId moduleId = module.moduleId();
                SemanticModel semanticModel = currentPackage.getCompilation().getSemanticModel(moduleId);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new ListenerFromSourceResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.codedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (node.kind() != SyntaxKind.LISTENER_DECLARATION) {
                    return new ListenerFromSourceResponse();
                }
                ListenerDeclarationNode listenerNode = (ListenerDeclarationNode) node;
                Optional<String> listenerName = getListenerName(listenerNode, semanticModel);
                if (listenerName.isEmpty()) {
                    return new ListenerFromSourceResponse();
                }
                Optional<Listener> listener = getListenerByName(listenerName.get());
                if (listener.isEmpty()) {
                    return new ListenerFromSourceResponse();
                }
                Listener listenerModel = listener.get();
                updateListenerModel(listenerModel, listenerNode);
                return new ListenerFromSourceResponse(listenerModel);
            } catch (Exception e) {
                return new ListenerFromSourceResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerResponse> getTriggerModel(TriggerRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (expectsTriggerByName(request)) {
                return new TriggerResponse(getTriggerBasicInfoByName(request.packageName()).orElse(null));
            }

            TriggerProperty triggerProperty = triggerProperties.get(request.id());
            if (triggerProperty == null) {
                return new TriggerResponse();
            }
            return new TriggerResponse(getTriggerBasicInfoByName(triggerProperty.name()).orElse(null));
        });
    }

    @JsonRequest
    public CompletableFuture<CommonSourceResponse> addFunction(FunctionSourceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new CommonSourceResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.codedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
                    return new CommonSourceResponse();
                }
                ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
                LineRange functionLineRange = serviceNode.openBraceToken().lineRange();
                NodeList<Node> members = serviceNode.members();
                if (!members.isEmpty()) {
                    functionLineRange = members.get(members.size() - 1).lineRange();
                }
                String functionNode = System.lineSeparator() + "\t" + getFunction(request.function(), new ArrayList<>())
                        .replace(System.lineSeparator(), System.lineSeparator() + "\t");
                edits.add(new TextEdit(Utils.toRange(functionLineRange.endLine()), functionNode));
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<CommonSourceResponse> updateFunction(FunctionModifierRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new CommonSourceResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.function().getCodedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (!(node instanceof FunctionDefinitionNode functionDefinitionNode)) {
                    return new CommonSourceResponse();
                }
                NonTerminalNode parentService = functionDefinitionNode.parent();
                if (!(parentService.kind().equals(SyntaxKind.SERVICE_DECLARATION))) {
                    return new CommonSourceResponse();
                }
                ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) parentService;
                LineRange serviceEnd = serviceDeclarationNode.closeBraceToken().lineRange();

                String functionName = functionDefinitionNode.functionName().text().trim();
                LineRange nameRange = functionDefinitionNode.functionName().lineRange();
                if (!functionName.equals(request.function().getAccessor().getValue())) {
                    edits.add(new TextEdit(Utils.toRange(nameRange), request.function().getAccessor().getValue()));
                }

                NodeList<Node> path = functionDefinitionNode.relativeResourcePath();
                if (Objects.nonNull(path) && !request.function().getName().getValue().equals(getPath(path))) {
                    LinePosition startPos = path.get(0).lineRange().startLine();
                    LinePosition endPos = path.get(path.size() - 1).lineRange().endLine();
                    LineRange pathLineRange = LineRange.from(lineRange.fileName(), startPos, endPos);
                    TextEdit pathEdit = new TextEdit(Utils.toRange(pathLineRange), getPath(path));
                    edits.add(pathEdit);
                }

                LineRange signatureRange = functionDefinitionNode.functionSignature().lineRange();
                List<String> statusCodeResponses = new ArrayList<>();
                String functionSignature = getFunctionSignature(request.function(), statusCodeResponses);
                edits.add(new TextEdit(Utils.toRange(signatureRange), functionSignature));
                String statusCodeResEdits = statusCodeResponses.stream()
                        .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
                if (!statusCodeResEdits.isEmpty()) {
                    edits.add(new TextEdit(Utils.toRange(serviceEnd.endLine()),
                            System.lineSeparator() + statusCodeResEdits));
                }

                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<CommonSourceResponse> updateService(ServiceModifierRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Service service = request.service();
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new CommonSourceResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = service.getCodedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
                    return new CommonSourceResponse();
                }
                ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
                Value basePathValue = service.getBasePath();
                if (Objects.nonNull(basePathValue) && basePathValue.isEnabledWithValue()) {
                    String basePath = basePathValue.getValue();
                    NodeList<Node> nodes = serviceNode.absoluteResourcePath();
                    if (!nodes.isEmpty()) {
                        LinePosition startPos = nodes.get(0).lineRange().startLine();
                        LinePosition endPos = nodes.get(nodes.size() - 1).lineRange().endLine();
                        LineRange basePathLineRange = LineRange.from(lineRange.fileName(), startPos, endPos);
                        TextEdit basePathEdit = new TextEdit(Utils.toRange(basePathLineRange), basePath);
                        edits.add(basePathEdit);
                    }
                }
                Value listener = service.getListener();
                if (Objects.nonNull(listener) && listener.isEnabledWithValue()) {
                    String listenerName = listener.getValue();
                    Optional<ExpressionNode> listenerExpression = getListenerExpression(serviceNode);
                    if (listenerExpression.isPresent()) {
                        LineRange listenerLineRange = listenerExpression.get().lineRange();
                        TextEdit listenerEdit = new TextEdit(Utils.toRange(listenerLineRange), listenerName);
                        edits.add(listenerEdit);
                    }
                }
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<CommonSourceResponse> updateListener(ListenerModifierRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Listener listener = request.listener();
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new CommonSourceResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = listener.getCodedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (node.kind() != SyntaxKind.LISTENER_DECLARATION) {
                    return new CommonSourceResponse();
                }
                String listenerDeclaration = listener.getDeclaration();
                TextEdit basePathEdit = new TextEdit(Utils.toRange(lineRange), listenerDeclaration);
                edits.add(basePathEdit);
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    public static Optional<String> getServiceName(ServiceDeclarationNode serviceNode, SemanticModel semanticModel) {
        Optional<Symbol> serviceSymbol = semanticModel.symbol(serviceNode);
        if (serviceSymbol.isEmpty() ||
                !(serviceSymbol.get() instanceof ServiceDeclarationSymbol serviceDeclaration)) {
            return Optional.empty();
        }
        Optional<ModuleSymbol> module = serviceDeclaration.listenerTypes().getFirst().getModule();
        if (module.isEmpty()) {
            return Optional.empty();
        }
        return module.get().getName();
    }

    public static Optional<String> getListenerName(ListenerDeclarationNode listenerDeclarationNode,
                                                   SemanticModel semanticModel) {
        Optional<TypeDescriptorNode> typeDescriptorNode = listenerDeclarationNode.typeDescriptor();
        if (typeDescriptorNode.isEmpty() ||
                !typeDescriptorNode.get().kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
            return Optional.empty();
        }
        Optional<Symbol> listenerTypeSymbol = semanticModel.symbol(typeDescriptorNode.get());
        if (listenerTypeSymbol.isEmpty() ||
                !(listenerTypeSymbol.get() instanceof TypeReferenceTypeSymbol listenerType)) {
            return Optional.empty();
        }
        Optional<ModuleSymbol> module = listenerType.typeDescriptor().getModule();
        if (module.isEmpty()) {
            return Optional.empty();
        }
        return module.get().getName();
    }

    private Optional<TriggerBasicInfo> getTriggerBasicInfoByName(String name) {
        if (triggerProperties.values().stream().noneMatch(trigger -> trigger.name().equals(name))) {
            return Optional.empty();
        }
        InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(String.format("listeners/%s.json", name));
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, TriggerBasicInfo.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Optional<Listener> getListenerByName(String name) {
        if (!name.equals("http") && !name.equals("graphql") &&
                triggerProperties.values().stream().noneMatch(trigger -> trigger.name().equals(name))) {
            return Optional.empty();
        }
        InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(String.format("listeners/%s.json", name));
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Listener.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Optional<Service> getServiceByName(String name) {
        if (!name.equals("http") && !name.equals("graphql") &&
                triggerProperties.values().stream().noneMatch(trigger -> trigger.name().equals(name))) {
            return Optional.empty();
        }
        InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(String.format("services/%s.json", name));
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Service.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
