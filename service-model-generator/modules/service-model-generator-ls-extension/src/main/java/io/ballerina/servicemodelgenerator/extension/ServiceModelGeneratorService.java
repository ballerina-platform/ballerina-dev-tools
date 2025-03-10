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
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ServiceDatabaseManager;
import io.ballerina.modelgenerator.commons.ServiceDeclaration;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.ModuleName;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.Listener;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.ServiceClass;
import io.ballerina.servicemodelgenerator.extension.model.TriggerBasicInfo;
import io.ballerina.servicemodelgenerator.extension.model.TriggerProperty;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.request.AddFieldRequest;
import io.ballerina.servicemodelgenerator.extension.request.ClassFieldModifierRequest;
import io.ballerina.servicemodelgenerator.extension.request.ClassModelFromSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.CommonModelFromSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.FunctionModelRequest;
import io.ballerina.servicemodelgenerator.extension.request.FunctionModifierRequest;
import io.ballerina.servicemodelgenerator.extension.request.FunctionSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerDiscoveryRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerModelRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerModifierRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceClassSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceModelRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceModifierRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.TriggerListRequest;
import io.ballerina.servicemodelgenerator.extension.request.TriggerRequest;
import io.ballerina.servicemodelgenerator.extension.response.CommonSourceResponse;
import io.ballerina.servicemodelgenerator.extension.response.FunctionModelResponse;
import io.ballerina.servicemodelgenerator.extension.response.ListenerDiscoveryResponse;
import io.ballerina.servicemodelgenerator.extension.response.ListenerFromSourceResponse;
import io.ballerina.servicemodelgenerator.extension.response.ListenerModelResponse;
import io.ballerina.servicemodelgenerator.extension.response.ServiceClassModelResponse;
import io.ballerina.servicemodelgenerator.extension.response.ServiceFromSourceResponse;
import io.ballerina.servicemodelgenerator.extension.response.ServiceModelResponse;
import io.ballerina.servicemodelgenerator.extension.response.TriggerListResponse;
import io.ballerina.servicemodelgenerator.extension.response.TriggerResponse;
import io.ballerina.servicemodelgenerator.extension.util.ListenerUtil;
import io.ballerina.servicemodelgenerator.extension.util.ServiceClassUtil;
import io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils;
import io.ballerina.servicemodelgenerator.extension.util.Utils;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getProtocol;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateFunctionList;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateGenericServiceModel;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateListenerItems;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.expectsTriggerByName;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.filterTriggers;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getFunction;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getFunctionSignature;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getImportStmt;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getListenerExpression;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getPath;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getServiceDeclarationNode;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.importExists;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.isHttpServiceContractType;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.populateRequiredFuncsDesignApproachAndServiceType;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.updateServiceContractModel;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.updateServiceModel;

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
    private static final Type propertyMapType = new TypeToken<Map<String, TriggerProperty>>() {
    }.getType();

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

    /**
     * Get the compatible listeners for the given module.
     *
     * @param request Listener discovery request
     * @return {@link ListenerDiscoveryResponse} of the listener discovery response
     */
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
                Set<String> listeners = ListenerUtil.getCompatibleListeners(request.moduleName(),
                        semanticModel, project);
                return new ListenerDiscoveryResponse(listeners);
            } catch (Throwable e) {
                return new ListenerDiscoveryResponse(e);
            }
        });
    }

    /**
     * Get the listener model template for the given module.
     *
     * @param request Listener model request
     * @return {@link ListenerModelResponse} of the listener model response
     */
    @JsonRequest
    public CompletableFuture<ListenerModelResponse> getListenerModel(ListenerModelRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ListenerUtil.getListenerModelByName(request.moduleName())
                        .map(ListenerModelResponse::new)
                        .orElseGet(ListenerModelResponse::new);
            } catch (Throwable e) {
                return new ListenerModelResponse(e);
            }
        });
    }

    /**
     * Get the list of text edits to add a listener to the given module.
     *
     * @param request Listener source request
     * @return {@link CommonSourceResponse} of the common source response
     */
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
                if (!importExists(node, listener.getOrgName(), listener.getModuleName())) {
                    String importText = getImportStmt(listener.getOrgName(), listener.getModuleName());
                    edits.add(new TextEdit(Utils.toRange(lineRange.startLine()), importText));
                }
                edits.add(new TextEdit(Utils.toRange(lineRange.endLine()),
                        ServiceModelGeneratorConstants.LINE_SEPARATOR + listenerDeclaration));
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    /**
     * Get the service model template for the given module.
     *
     * @param request Service model request
     * @return {@link ServiceModelResponse} of the service model response
     */
    @JsonRequest
    public CompletableFuture<ServiceModelResponse> getServiceModel(ServiceModelRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Service> service = ServiceModelUtils.getEmptyServiceModel(request.moduleName());
                if (service.isEmpty()) {
                    return new ServiceModelResponse();
                }
                Service serviceModel = service.get();
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Package currentPackage = project.currentPackage();
                Module module = currentPackage.module(ModuleName.from(currentPackage.packageName()));
                SemanticModel semanticModel = currentPackage.getCompilation().getSemanticModel(module.moduleId());
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new ServiceModelResponse();
                }
                Set<String> listenersList = ListenerUtil.getCompatibleListeners(
                        request.moduleName(), semanticModel, project);
                serviceModel.getListener().setItems(listenersList.stream().toList());
                return new ServiceModelResponse(serviceModel);
            } catch (Throwable e) {
                return new ServiceModelResponse(e);
            }
        });
    }

    /**
     * Get the list of text edits to add a service to the given module.
     *
     * @param request Service source request
     * @return {@link CommonSourceResponse} of the common source response
     */
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
                populateRequiredFuncsDesignApproachAndServiceType(service);

                boolean isDefaultListenerCreationRequired =
                        ListenerUtil.checkForDefaultListenerExistence(service.getListener());

                if (Objects.nonNull(service.getOpenAPISpec())) {
                    Path contractPath = Path.of(service.getOpenAPISpec().getValue());
                    String serviceContractTypeName = service.getServiceContractTypeName();
                    OpenApiServiceGenerator oasSvcGenerator = new OpenApiServiceGenerator(contractPath,
                            project.sourceRoot(), workspaceManager);
                    return new CommonSourceResponse(oasSvcGenerator.generateService(serviceContractTypeName,
                            service.getListener().getValue(), isDefaultListenerCreationRequired));
                }

                if (!importExists(node, service.getOrgName(), service.getModuleName())) {
                    String importText = Utils.getImportStmt(service.getOrgName(), service.getModuleName());
                    edits.add(new TextEdit(Utils.toRange(lineRange.startLine()), importText));
                }

                SemanticModel semanticModel = null;
                if (isDefaultListenerCreationRequired) {
                    List<ImportDeclarationNode> importsList = node.imports().stream().toList();
                    LinePosition listenerDeclaringLoc;
                    if (!importsList.isEmpty()) {
                        listenerDeclaringLoc = importsList.get(importsList.size() - 1).lineRange().endLine();
                    } else {
                        listenerDeclaringLoc = lineRange.endLine();
                    }
                    semanticModel = document.get().module().getCompilation().getSemanticModel();
                    String listenerDeclarationStmt = ListenerUtil.getListenerDeclarationStmt(
                            semanticModel, document.get(), listenerDeclaringLoc);
                    edits.add(new TextEdit(Utils.toRange(listenerDeclaringLoc), listenerDeclarationStmt));
                }

                Utils.FunctionAddContext context = Utils.getTriggerAddContext(service.getOrgName(),
                        service.getPackageName());
                if (context.equals(Utils.FunctionAddContext.TCP_SERVICE_ADD)) {
                    if (semanticModel == null) {
                        semanticModel = document.get().module().getCompilation().getSemanticModel();
                    }
                    String serviceName = Utils.generateTypeIdentifier(semanticModel, document.get(),
                            lineRange.endLine(), "TcpEchoService");
                    service.getProperties().put("returningServiceClass", Value.getTcpValue(serviceName));
                }

                updateFunctionList(service);
                String serviceDeclaration = getServiceDeclarationNode(service, context);
                edits.add(new TextEdit(Utils.toRange(lineRange.endLine()),
                        ServiceModelGeneratorConstants.LINE_SEPARATOR + serviceDeclaration));

                if (context.equals(Utils.FunctionAddContext.TCP_SERVICE_ADD)) {
                    String serviceName = service.getProperties().get("returningServiceClass").getValue();
                    String serviceClass = ServiceClassUtil.getTcpConnectionServiceTemplate().formatted(serviceName);
                    edits.add(new TextEdit(Utils.toRange(lineRange.endLine()),
                            ServiceModelGeneratorConstants.LINE_SEPARATOR + serviceClass
                                    + ServiceModelGeneratorConstants.LINE_SEPARATOR));
                }

                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    /**
     * Find matching trigger models for the given request.
     *
     * @param request Trigger list request
     * @return {@link TriggerListResponse} of the trigger list response
     */
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

    /**
     * Get the function model template for a given function in a service type.
     *
     * @return {@link FunctionModelResponse} of the resource model response
     */
    @JsonRequest
    public CompletableFuture<FunctionModelResponse> getFunctionModel(FunctionModelRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Utils.getFunctionModel(request.type(), request.functionName())
                        .map(FunctionModelResponse::new)
                        .orElseGet(FunctionModelResponse::new);
            } catch (Throwable e) {
                return new FunctionModelResponse(e);
            }
        });
    }

    /**
     * Get the list of text edits to add a resource function to a service.
     *
     * @param request Function source request
     * @return {@link CommonSourceResponse} of the common source response
     */
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
                String functionDefinition = ServiceModelGeneratorConstants.LINE_SEPARATOR +
                        "\t" + getFunction(request.function(), statusCodeResponses, Utils.FunctionBodyKind.DO_BLOCK,
                        Utils.FunctionAddContext.RESOURCE_ADD)
                        .replace(ServiceModelGeneratorConstants.LINE_SEPARATOR,
                                ServiceModelGeneratorConstants.LINE_SEPARATOR + "\t")
                        + ServiceModelGeneratorConstants.LINE_SEPARATOR;
                List<TextEdit> textEdits = new ArrayList<>();
                textEdits.add(new TextEdit(Utils.toRange(serviceEnd.startLine()), functionDefinition));
                String statusCodeResEdits = statusCodeResponses.stream()
                        .collect(Collectors.joining(ServiceModelGeneratorConstants.LINE_SEPARATOR
                                + ServiceModelGeneratorConstants.LINE_SEPARATOR));
                if (!statusCodeResEdits.isEmpty()) {
                    textEdits.add(new TextEdit(Utils.toRange(serviceEnd.endLine()),
                            ServiceModelGeneratorConstants.LINE_SEPARATOR + statusCodeResEdits));
                }
                return new CommonSourceResponse(Map.of(request.filePath(), textEdits));
            } catch (Exception e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    /**
     * Get the service model for the given line range.
     *
     * @param request Common model from source request
     * @return {@link ServiceFromSourceResponse} of the service from source response
     */
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
            ModuleAndServiceType moduleAndServiceType = deriveServiceType(serviceNode, semanticModel);
            if (Objects.isNull(moduleAndServiceType.moduleName())) {
                return new ServiceFromSourceResponse();
            }
            String moduleName = moduleAndServiceType.moduleName();
            if (moduleName.equals("http")) {
                Optional<Service> service = ServiceModelUtils.getHttpService();
                if (service.isEmpty()) {
                    return new ServiceFromSourceResponse();
                }
                Service serviceModel = service.get();
                serviceModel.setFunctions(new ArrayList<>());
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
                updateListenerItems(moduleName, semanticModel, project, serviceModel);
                return new ServiceFromSourceResponse(serviceModel);
            }
            String serviceType = serviceTypeWithoutPrefix(moduleAndServiceType);
            Optional<Service> service = ServiceModelUtils.getServiceModelWithFunctions(moduleName, serviceType);
            if (service.isEmpty()) {
                return new ServiceFromSourceResponse();
            }
            Service serviceModel = service.get();
            updateGenericServiceModel(serviceModel, serviceNode, semanticModel);
            updateListenerItems(moduleName, semanticModel, project, serviceModel);
            return new ServiceFromSourceResponse(serviceModel);
        });
    }

    private static String serviceTypeWithoutPrefix(ModuleAndServiceType moduleAndServiceType) {
        String[] serviceTypeNames = moduleAndServiceType.serviceType().split(":");
        String serviceType = "Service";
        if (serviceTypeNames.length > 1) {
            serviceType = serviceTypeNames[1];
        }
        return serviceType;
    }

    /**
     * Get the listener model for the given line range.
     *
     * @param request Common model from source request
     * @return {@link ListenerFromSourceResponse} of the listener from source response
     */
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
                Optional<Listener> listenerModelOp;
                if (ListenerUtil.isHttpDefaultListener(listenerNode)) {
                    listenerModelOp = ListenerUtil.getDefaultListenerModel();
                } else {
                    listenerModelOp = ListenerUtil.getListenerFromSource(listenerNode, semanticModel);
                }
                if (listenerModelOp.isEmpty()) {
                    return new ListenerFromSourceResponse();
                }
                Listener listenerModel = listenerModelOp.get();
                Value nameProperty = listenerModel.getProperty("name");
                nameProperty.setValue(listenerNode.variableName().text().trim());
                nameProperty.setCodedata(new Codedata(listenerNode.variableName().lineRange()));
                nameProperty.setEditable(false);
                listenerModel.setCodedata(new Codedata(listenerNode.lineRange()));
                return new ListenerFromSourceResponse(listenerModel);
            } catch (Exception e) {
                return new ListenerFromSourceResponse(e);
            }
        });
    }

    /**
     * Get the list of triggers for a given search query.
     *
     * @param request Trigger list request
     * @return {@link TriggerListResponse} of the trigger list response
     */
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

    /**
     * Get the list of text edits to add a function skeleton to the given service.
     *
     * @param request Function source request
     * @return {@link CommonSourceResponse} of the common source response
     */
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
                if (!(node.kind().equals(SyntaxKind.SERVICE_DECLARATION) ||
                        node.kind().equals(SyntaxKind.CLASS_DEFINITION))) {
                    return new CommonSourceResponse();
                }
                LineRange functionLineRange;
                NodeList<Node> members;
                if (node instanceof ServiceDeclarationNode serviceDeclarationNode) {
                    functionLineRange = serviceDeclarationNode.openBraceToken().lineRange();
                    members = serviceDeclarationNode.members();
                } else {
                    ClassDefinitionNode classDefinitionNode = (ClassDefinitionNode) node;
                    functionLineRange = classDefinitionNode.openBrace().lineRange();
                    members = classDefinitionNode.members();
                }

                if (!members.isEmpty()) {
                    functionLineRange = members.get(members.size() - 1).lineRange();
                }
                String functionNode = ServiceModelGeneratorConstants.LINE_SEPARATOR + "\t"
                        + getFunction(request.function(), new ArrayList<>(), Utils.FunctionBodyKind.DO_BLOCK,
                        Utils.FunctionAddContext.FUNCTION_ADD)
                        .replace(ServiceModelGeneratorConstants.LINE_SEPARATOR,
                                ServiceModelGeneratorConstants.LINE_SEPARATOR + "\t");
                edits.add(new TextEdit(Utils.toRange(functionLineRange.endLine()), functionNode));
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    /**
     * Get the list of text edits to modify a function in the given service.
     *
     * @param request Function modifier request
     * @return {@link CommonSourceResponse} of the common source response
     */
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
                Function function = request.function();
                LineRange lineRange = function.getCodedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (!(node instanceof FunctionDefinitionNode functionDefinitionNode)) {
                    return new CommonSourceResponse();
                }
                NonTerminalNode parentService = functionDefinitionNode.parent();
                if (!(parentService.kind().equals(SyntaxKind.SERVICE_DECLARATION) ||
                        parentService.kind().equals(SyntaxKind.CLASS_DEFINITION))) {
                    return new CommonSourceResponse();
                }

                String functionName = functionDefinitionNode.functionName().text().trim();
                LineRange nameRange = functionDefinitionNode.functionName().lineRange();
                String functionKind = function.getKind();
                boolean isRemoteFunction = functionKind.equals(ServiceModelGeneratorConstants.KIND_REMOTE)
                        || functionKind.equals(ServiceModelGeneratorConstants.KIND_MUTATION);
                if (isRemoteFunction && !functionName.equals(function.getName().getValue())) {
                    edits.add(new TextEdit(Utils.toRange(nameRange), function.getName().getValue()));
                } else {
                    if (!isRemoteFunction && !functionName.equals(function.getAccessor().getValue())) {
                        edits.add(new TextEdit(Utils.toRange(nameRange), function.getAccessor().getValue()));
                    }
                }

                if (!isRemoteFunction) {
                    NodeList<Node> path = functionDefinitionNode.relativeResourcePath();
                    if (Objects.nonNull(path) && !function.getName().getValue().equals(getPath(path))) {
                        LinePosition startPos = path.get(0).lineRange().startLine();
                        LinePosition endPos = path.get(path.size() - 1).lineRange().endLine();
                        LineRange pathLineRange = LineRange.from(lineRange.fileName(), startPos, endPos);
                        TextEdit pathEdit = new TextEdit(Utils.toRange(pathLineRange),
                                function.getName().getValue());
                        edits.add(pathEdit);
                    }
                }

                LineRange signatureRange = functionDefinitionNode.functionSignature().lineRange();
                List<String> statusCodeResponses = new ArrayList<>();
                String functionSignature = getFunctionSignature(function, statusCodeResponses);
                edits.add(new TextEdit(Utils.toRange(signatureRange), functionSignature));
                String statusCodeResEdits = statusCodeResponses.stream()
                        .collect(Collectors.joining(ServiceModelGeneratorConstants.LINE_SEPARATOR
                                + ServiceModelGeneratorConstants.LINE_SEPARATOR));

                if (parentService.kind().equals(SyntaxKind.SERVICE_DECLARATION)) {
                    ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) parentService;
                    LineRange serviceEnd = serviceDeclarationNode.closeBraceToken().lineRange();

                    if (!statusCodeResEdits.isEmpty()) {
                        edits.add(new TextEdit(Utils.toRange(serviceEnd.endLine()),
                                ServiceModelGeneratorConstants.LINE_SEPARATOR + statusCodeResEdits));
                    }
                }

                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    /**
     * Get the list of text edits to modify a service in the given module.
     *
     * @param request Service modifier request
     * @return {@link CommonSourceResponse} of the common source response
     */
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

                Value stringLiteral = service.getStringLiteralProperty();
                if (Objects.nonNull(stringLiteral) && stringLiteral.isEnabledWithValue()) {
                    String stringLiteralValue = stringLiteral.getValue();
                    NodeList<Node> nodes = serviceNode.absoluteResourcePath();
                    if (!nodes.isEmpty()) {
                        LinePosition startPos = nodes.get(0).lineRange().startLine();
                        LinePosition endPos = nodes.get(nodes.size() - 1).lineRange().endLine();
                        LineRange basePathLineRange = LineRange.from(lineRange.fileName(), startPos, endPos);
                        TextEdit basePathEdit = new TextEdit(Utils.toRange(basePathLineRange), stringLiteralValue);
                        edits.add(basePathEdit);
                    }
                }

                Value listener = service.getListener();
                boolean isDefaultListenerCreationRequired = false;
                if (Objects.nonNull(listener) && listener.isEnabledWithValue()) {
                    isDefaultListenerCreationRequired = ListenerUtil
                            .checkForDefaultListenerExistence(service.getListener());

                    String listenerName = listener.getValue();
                    Optional<ExpressionNode> listenerExpression = getListenerExpression(serviceNode);
                    if (listenerExpression.isPresent()) {
                        LineRange listenerLineRange = listenerExpression.get().lineRange();
                        TextEdit listenerEdit = new TextEdit(Utils.toRange(listenerLineRange), listenerName);
                        edits.add(listenerEdit);
                    }
                }

                if (isDefaultListenerCreationRequired) {
                    List<ImportDeclarationNode> importsList = modulePartNode.imports().stream().toList();
                    LinePosition listenerDeclaringLoc;
                    if (!importsList.isEmpty()) {
                        listenerDeclaringLoc = importsList.get(importsList.size() - 1).lineRange().endLine();
                    } else {
                        listenerDeclaringLoc = lineRange.endLine();
                    }
                    SemanticModel semanticModel = document.get().module().getCompilation().getSemanticModel();
                    String listenerDeclarationStmt = ListenerUtil.getListenerDeclarationStmt(
                            semanticModel, document.get(), listenerDeclaringLoc);
                    edits.add(new TextEdit(Utils.toRange(listenerDeclaringLoc), listenerDeclarationStmt));
                }

                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    /**
     * Get the list of text edits to modify a listener in the given module.
     *
     * @param request Listener modifier request
     * @return {@link CommonSourceResponse} of the common source response
     */
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

    /**
     * Get the JSON model for a service class from the source.
     *
     * @param request Service lass model request
     * @return Service class model response
     */
    @JsonRequest
    public CompletableFuture<ServiceClassModelResponse> getServiceClassModelFromSource(
            ClassModelFromSourceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                try {
                    this.workspaceManager.loadProject(filePath);
                } catch (Exception e) {
                    return new ServiceClassModelResponse(e);
                }
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new ServiceClassModelResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.codedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (node.kind() != SyntaxKind.CLASS_DEFINITION) {
                    return new ServiceClassModelResponse();
                }
                ServiceClassUtil.ServiceClassContext context = ServiceClassUtil.ServiceClassContext
                        .valueOf(request.context());
                ClassDefinitionNode classDefinitionNode = (ClassDefinitionNode) node;
                ServiceClass serviceClass = ServiceClassUtil.getServiceClass(classDefinitionNode, context);
                return new ServiceClassModelResponse(serviceClass);
            } catch (Throwable e) {
                return new ServiceClassModelResponse(e);
            }
        });
    }

    /**
     * Get the list of text edits to modify a service class.
     *
     * @param request Service class source request
     * @return {@link CommonSourceResponse} of the common source response
     */
    @JsonRequest
    public CompletableFuture<CommonSourceResponse> updateServiceClass(ServiceClassSourceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                ServiceClass serviceClass = request.serviceClass();
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new CommonSourceResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = serviceClass.codedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (node.kind() != SyntaxKind.CLASS_DEFINITION) {
                    return new CommonSourceResponse();
                }
                ClassDefinitionNode classDefinitionNode = (ClassDefinitionNode) node;
                Value className = serviceClass.className();
                if (Objects.nonNull(className) && className.isEnabledWithValue()
                        && !className.getValue().equals(classDefinitionNode.className().text().trim())) {
                    LineRange nameRange = classDefinitionNode.className().lineRange();
                    edits.add(new TextEdit(Utils.toRange(nameRange), className.getValue()));
                }
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }


    /**
     * Get the list of text edits to add a function skeleton to the given service.
     *
     * @param request Function source request
     * @return {@link CommonSourceResponse} of the common source response
     */
    @JsonRequest
    public CompletableFuture<CommonSourceResponse> addField(AddFieldRequest request) {
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
                if (!(node.kind().equals(SyntaxKind.SERVICE_DECLARATION) ||
                        node.kind().equals(SyntaxKind.CLASS_DEFINITION))) {
                    return new CommonSourceResponse();
                }
                LineRange functionLineRange;
                if (node instanceof ServiceDeclarationNode serviceDeclarationNode) {
                    functionLineRange = serviceDeclarationNode.openBraceToken().lineRange();
                } else {
                    ClassDefinitionNode classDefinitionNode = (ClassDefinitionNode) node;
                    functionLineRange = classDefinitionNode.openBrace().lineRange();
                }

                String functionNode = ServiceModelGeneratorConstants.LINE_SEPARATOR + "\t"
                        + ServiceClassUtil.buildObjectFiledString(request.field());
                edits.add(new TextEdit(Utils.toRange(functionLineRange.endLine()), functionNode));
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    /**
     * Get the list of text edits to add a class field to the given class.
     *
     * @param request Class field source request
     * @return {@link CommonSourceResponse} of the common source response
     */
    @JsonRequest
    public CompletableFuture<CommonSourceResponse> updateClassField(ClassFieldModifierRequest request) {
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
                LineRange lineRange = request.field().codedata().getLineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (!(node instanceof ObjectFieldNode)) {
                    return new CommonSourceResponse();
                }
                TextEdit fieldEdit = new TextEdit(Utils.toRange(lineRange),
                        ServiceClassUtil.buildObjectFiledString(request.field()));
                edits.add(fieldEdit);
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }


    public static ModuleAndServiceType deriveServiceType(ServiceDeclarationNode serviceNode,
                                                         SemanticModel semanticModel) {
        Optional<TypeDescriptorNode> serviceTypeDesc = serviceNode.typeDescriptor();
        Optional<ModuleSymbol> module = Optional.empty();
        String serviceType = "Service";
        if (serviceTypeDesc.isPresent()) {
            TypeDescriptorNode typeDescriptorNode = serviceTypeDesc.get();
            serviceType = typeDescriptorNode.toString().trim();
            Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(typeDescriptorNode);
            if (typeSymbol.isPresent()) {
                module = typeSymbol.get().getModule();
            }
        }

        if (module.isEmpty()) {
            SeparatedNodeList<ExpressionNode> expressions = serviceNode.expressions();
            if (expressions.isEmpty()) {
                return new ModuleAndServiceType(null, serviceType);
            }
            ExpressionNode expressionNode = expressions.get(0);
            if (expressionNode instanceof ExplicitNewExpressionNode explicitNewExpressionNode) {
                Optional<Symbol> symbol = semanticModel.symbol(explicitNewExpressionNode.typeDescriptor());
                if (symbol.isEmpty()) {
                    return new ModuleAndServiceType(null, serviceType);
                }
                module = symbol.get().getModule();
            } else if (expressionNode instanceof NameReferenceNode nameReferenceNode) {
                Optional<Symbol> symbol = semanticModel.symbol(nameReferenceNode);
                if (symbol.isPresent() && symbol.get() instanceof VariableSymbol variableSymbol) {
                    module = variableSymbol.typeDescriptor().getModule();
                }
            }
        }

        if (module.isEmpty()) {
            return new ModuleAndServiceType(null, serviceType);
        }
        return new ModuleAndServiceType(module.get().getName().orElse(null), serviceType);
    }

    public record ModuleAndServiceType(String moduleName, String serviceType) {
    }

    private Optional<TriggerBasicInfo> getTriggerBasicInfoByName(String name) {
        Optional<ServiceDeclaration> serviceDeclaration = ServiceDatabaseManager.getInstance()
                .getServiceDeclaration(name);

        if (serviceDeclaration.isEmpty()) {
            return Optional.empty();
        }
        ServiceDeclaration serviceTemplate = serviceDeclaration.get();
        ServiceDeclaration.Package pkg = serviceTemplate.packageInfo();
        String protocol = getProtocol(name);
        String label = serviceTemplate.displayName();
        String icon = CommonUtils.generateIcon(pkg.org(), pkg.name(), pkg.version());
        TriggerBasicInfo triggerBasicInfo = new TriggerBasicInfo(pkg.packageId(),
                label, pkg.org(), pkg.name(), pkg.name(),
                pkg.version(), serviceTemplate.kind(), label, "",
                protocol, icon);

        return Optional.of(triggerBasicInfo);
    }
}
