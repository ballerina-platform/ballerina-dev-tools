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
import io.ballerina.servicemodelgenerator.extension.response.AddOrGetDefaultListenerResponse;
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

import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_TYPE_DESC;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.KIND_MUTATION;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.KIND_REMOTE;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.NEW_LINE;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.NEW_LINE_WITH_TAB;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.TWO_NEW_LINES;
import static io.ballerina.servicemodelgenerator.extension.util.HttpUtil.updateHttpServiceContractModel;
import static io.ballerina.servicemodelgenerator.extension.util.HttpUtil.updateHttpServiceModel;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getProtocol;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.populateRequiredFunctionsForServiceType;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateGenericServiceModel;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateListenerItems;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.FunctionAddContext.RESOURCE_ADD;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.FunctionAddContext.TCP_SERVICE_ADD;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.FunctionBodyKind.DO_BLOCK;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.addServiceAnnotationTextEdits;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.expectsTriggerByName;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.filterTriggers;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getFunction;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getFunctionSignature;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getImportStmt;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getListenerExpression;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getPath;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getServiceDeclarationNode;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.importExists;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.isAiAgentModule;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getHttpServiceContractSym;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.populateRequiredFuncsDesignApproachAndServiceType;

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
                reader.close();
                newPropertiesStream.close();
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
                ModulePartNode modulePartNode = document.get().syntaxTree().rootNode();
                LineRange lineRange = modulePartNode.lineRange();
                Listener listener = request.listener();
                if (!importExists(modulePartNode, listener.getOrgName(), listener.getModuleName())) {
                    String importText = getImportStmt(listener.getOrgName(), listener.getModuleName());
                    edits.add(new TextEdit(Utils.toRange(lineRange.startLine()), importText));
                }
                String listenerDeclaration = listener.getDeclaration();
                edits.add(new TextEdit(Utils.toRange(lineRange.endLine()), NEW_LINE + listenerDeclaration));
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    /**
     * Get the http default listener reference or send text edits to add a default listener.
     *
     * @param request Listener discovery request
     * @return {@link AddOrGetDefaultListenerResponse} of the add or get default listener response
     */
    @JsonRequest
    public CompletableFuture<AddOrGetDefaultListenerResponse> addOrGetDefaultListener(
            ListenerDiscoveryRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AddOrGetDefaultListenerResponse response = new AddOrGetDefaultListenerResponse();
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Package currentPackage = project.currentPackage();
                Module module = currentPackage.module(ModuleName.from(currentPackage.packageName()));
                ModuleId moduleId = module.moduleId();
                SemanticModel semanticModel = currentPackage.getCompilation().getSemanticModel(moduleId);

                Optional<String> httpDefaultListenerNameRef = ListenerUtil.getHttpDefaultListenerNameRef(
                        semanticModel, project);
                if (httpDefaultListenerNameRef.isPresent()) {
                    response.setDefaultListenerRef(httpDefaultListenerNameRef.get());
                    return response;
                }
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return response;
                }
                ModulePartNode node = document.get().syntaxTree().rootNode();
                LineRange lineRange = node.lineRange();

                List<TextEdit> edits = new ArrayList<>();
                if (!importExists(node, "ballerina", "http")) {
                    String importText = getImportStmt("ballerina", "http");
                    edits.add(new TextEdit(Utils.toRange(lineRange.startLine()), importText));
                }

                List<ImportDeclarationNode> importsList = node.imports().stream().toList();
                LinePosition listenerDeclaringLoc = importsList.isEmpty() ? lineRange.endLine() :
                        importsList.getLast().lineRange().endLine();
                String listenerDeclarationStmt = ListenerUtil.getHttpDefaultListenerDeclarationStmt(
                        semanticModel, document.get(), listenerDeclaringLoc);
                edits.add(new TextEdit(Utils.toRange(listenerDeclaringLoc), listenerDeclarationStmt));

                response.setTextEdits(Map.of(request.filePath(), edits));
                return response;
            } catch (Throwable e) {
                return new AddOrGetDefaultListenerResponse(e);
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
                Set<String> listenersList = ListenerUtil.getCompatibleListeners(request.moduleName(), semanticModel,
                        project);
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
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                if (document.isEmpty() || semanticModel.isEmpty()) {
                    return new CommonSourceResponse();
                }
                ModulePartNode node = document.get().syntaxTree().rootNode();
                LineRange lineRange = node.lineRange();
                Service service = request.service();
                populateRequiredFuncsDesignApproachAndServiceType(service);

                boolean createDefaultListener = ListenerUtil.createDefaultListener(service.getListener());
                if (Objects.nonNull(service.getOpenAPISpec())) {
                    OpenApiServiceGenerator oasSvcGenerator = new OpenApiServiceGenerator(
                            Path.of(service.getOpenAPISpec().getValue()), project.sourceRoot(), workspaceManager);
                    return new CommonSourceResponse(oasSvcGenerator.generateService(service, createDefaultListener));
                }

                List<String> importStmts = new ArrayList<>();
                if (isAiAgentModule(service.getOrgName(), service.getModuleName()) &&
                        !importExists(node, "ballerina", "http")) {
                    importStmts.add(Utils.getImportStmt("ballerina", "http"));
                }

                if (!importExists(node, service.getOrgName(), service.getModuleName())) {
                    importStmts.add(Utils.getImportStmt(service.getOrgName(), service.getModuleName()));
                }

                List<TextEdit> edits = new ArrayList<>();
                if (!importStmts.isEmpty()) {
                    String imports = String.join(NEW_LINE, importStmts);
                    edits.add(new TextEdit(Utils.toRange(lineRange.startLine()), imports));
                }

                if (createDefaultListener) {
                    List<ImportDeclarationNode> importsList = node.imports().stream().toList();
                    LinePosition listenerDeclaringLoc = importsList.isEmpty() ? lineRange.endLine() :
                            importsList.getLast().lineRange().endLine();
                    String listenerDeclarationStmt = ListenerUtil.getHttpDefaultListenerDeclarationStmt(
                            semanticModel.get(), document.get(), listenerDeclaringLoc);
                    edits.add(new TextEdit(Utils.toRange(listenerDeclaringLoc), listenerDeclarationStmt));
                }

                Utils.FunctionAddContext context = Utils.getTriggerAddContext(service.getOrgName(),
                        service.getPackageName());
                if (context.equals(TCP_SERVICE_ADD)) {
                    String serviceName = Utils.generateTypeIdentifier(semanticModel.get(), document.get(),
                            lineRange.endLine(), "TcpEchoService");
                    service.getProperties().put("returningServiceClass", Value.getTcpValue(serviceName));
                }

                populateRequiredFunctionsForServiceType(service);
                String serviceDeclaration = getServiceDeclarationNode(service, context);
                edits.add(new TextEdit(Utils.toRange(lineRange.endLine()), NEW_LINE + serviceDeclaration));

                if (context.equals(TCP_SERVICE_ADD)) {
                    String serviceName = service.getProperties().get("returningServiceClass").getValue();
                    String serviceClass = ServiceClassUtil.getTcpConnectionServiceTemplate().formatted(serviceName);
                    edits.add(new TextEdit(Utils.toRange(lineRange.endLine()), serviceClass));
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
     * Get the list of text edits to add a http resource function.
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
                NonTerminalNode node = findNonTerminalNode(request.codedata(), document.get());
                if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
                    return new CommonSourceResponse();
                }
                ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
                List<String> newStatusCodeTypesDef = new ArrayList<>();
                String functionDefinition = NEW_LINE_WITH_TAB + getFunction(request.function(), newStatusCodeTypesDef,
                        DO_BLOCK, RESOURCE_ADD).replace(NEW_LINE, NEW_LINE_WITH_TAB) + NEW_LINE;

                List<TextEdit> textEdits = new ArrayList<>();
                LineRange serviceEnd = serviceNode.closeBraceToken().lineRange();
                textEdits.add(new TextEdit(Utils.toRange(serviceEnd.startLine()), functionDefinition));
                if (!newStatusCodeTypesDef.isEmpty()) {
                    String statusCodeResEdits = String.join(TWO_NEW_LINES, newStatusCodeTypesDef);
                    textEdits.add(new TextEdit(Utils.toRange(serviceEnd.endLine()),
                            NEW_LINE + statusCodeResEdits));
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
            Optional<SemanticModel> semanticModelOp;
            Optional<Document> document;
            Project project;
            try {
                project = this.workspaceManager.loadProject(filePath);
                semanticModelOp = this.workspaceManager.semanticModel(filePath);
                document = this.workspaceManager.document(filePath);
            } catch (Exception e) {
                return new ServiceFromSourceResponse(e);
            }

            if (Objects.isNull(project) || document.isEmpty() || semanticModelOp.isEmpty()) {
                return new ServiceFromSourceResponse();
            }
            NonTerminalNode node = findNonTerminalNode(request.codedata(), document.get());
            if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
                return new ServiceFromSourceResponse();
            }
            ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
            SemanticModel semanticModel = semanticModelOp.get();
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
                boolean serviceContractExists = false;
                if (serviceNode.typeDescriptor().isPresent()) {
                    Optional<Symbol> httpServiceContractSym = getHttpServiceContractSym(semanticModel,
                            serviceNode.typeDescriptor().get());
                    if (httpServiceContractSym.isPresent() && httpServiceContractSym.get().getLocation().isPresent()) {
                        Path contractPath = project.sourceRoot().toAbsolutePath()
                                .resolve(httpServiceContractSym.get().getLocation().get().lineRange().fileName());
                        Optional<Document> contractDoc = this.workspaceManager.document(contractPath);
                        if (contractDoc.isPresent()) {
                            ModulePartNode contractModulePartNode = contractDoc.get().syntaxTree().rootNode();
                            Optional<TypeDefinitionNode> serviceContractType = contractModulePartNode.members().stream()
                                    .filter(member -> member.kind().equals(SyntaxKind.TYPE_DEFINITION))
                                    .map(member -> ((TypeDefinitionNode) member))
                                    .filter(member -> member.typeDescriptor().kind().equals(OBJECT_TYPE_DESC))
                                    .findFirst();
                            if (serviceContractType.isPresent()) {
                                serviceContractExists = true;
                                updateHttpServiceContractModel(serviceModel, serviceContractType.get(), serviceNode,
                                        semanticModel);
                            }
                        }
                    }
                }

                if (!serviceContractExists) {
                    updateHttpServiceModel(serviceModel, serviceNode, semanticModel);
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
                NonTerminalNode node = findNonTerminalNode(request.codedata(), document.get());
                if (!(node instanceof ListenerDeclarationNode listenerNode)) {
                    return new ListenerFromSourceResponse();
                }
                Optional<Listener> listenerModelOp = ListenerUtil.getListenerFromSource(listenerNode, semanticModel);
                if (listenerModelOp.isEmpty()) {
                    return new ListenerFromSourceResponse();
                }
                Listener listenerModel = listenerModelOp.get();
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
                NonTerminalNode node = findNonTerminalNode(request.codedata(), document.get());
                if (!(node instanceof ServiceDeclarationNode || node instanceof ClassDefinitionNode)) {
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
                String functionNode = NEW_LINE_WITH_TAB + getFunction(request.function(), List.of(), DO_BLOCK,
                        Utils.FunctionAddContext.FUNCTION_ADD).replace(NEW_LINE, NEW_LINE_WITH_TAB);
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
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new CommonSourceResponse();
                }
                Function function = request.function();
                LineRange lineRange = function.getCodedata().getLineRange();
                NonTerminalNode node = findNonTerminalNode(function.getCodedata(), document.get());
                if (!(node instanceof FunctionDefinitionNode functionDefinitionNode)) {
                    return new CommonSourceResponse();
                }
                NonTerminalNode parentNode = functionDefinitionNode.parent();
                if (!(parentNode instanceof ServiceDeclarationNode || parentNode instanceof ClassDefinitionNode)) {
                    return new CommonSourceResponse();
                }
                List<TextEdit> edits = new ArrayList<>();
                Utils.addFunctionAnnotationTextEdits(function, functionDefinitionNode, edits);

                String functionName = functionDefinitionNode.functionName().text().trim();
                LineRange nameRange = functionDefinitionNode.functionName().lineRange();
                String functionKind = function.getKind();
                boolean isRemote = functionKind.equals(KIND_REMOTE) || functionKind.equals(KIND_MUTATION);
                String newFunctionName = function.getName().getValue();
                if (isRemote && !functionName.equals(newFunctionName)) {
                    edits.add(new TextEdit(Utils.toRange(nameRange), newFunctionName));
                }

                if (!isRemote) {
                    if (!functionName.equals(function.getAccessor().getValue())) {
                        edits.add(new TextEdit(Utils.toRange(nameRange), function.getAccessor().getValue()));
                    }

                    NodeList<Node> path = functionDefinitionNode.relativeResourcePath();
                    if (Objects.nonNull(path) && !newFunctionName.equals(getPath(path))) {
                        LinePosition startPos = path.get(0).lineRange().startLine();
                        LinePosition endPos = path.get(path.size() - 1).lineRange().endLine();
                        LineRange pathLineRange = LineRange.from(lineRange.fileName(), startPos, endPos);
                        TextEdit pathEdit = new TextEdit(Utils.toRange(pathLineRange), newFunctionName);
                        edits.add(pathEdit);
                    }
                }

                LineRange signatureRange = functionDefinitionNode.functionSignature().lineRange();
                List<String> newStatusCodeTypesDef = new ArrayList<>();
                String functionSignature = getFunctionSignature(function, newStatusCodeTypesDef, false);
                edits.add(new TextEdit(Utils.toRange(signatureRange), functionSignature));

                if (!newStatusCodeTypesDef.isEmpty() && parentNode instanceof ServiceDeclarationNode serviceNode) {
                    String statusCodeResEdits = String.join(TWO_NEW_LINES, newStatusCodeTypesDef);
                    edits.add(new TextEdit(Utils.toRange(serviceNode.closeBraceToken().lineRange().endLine()),
                            NEW_LINE + statusCodeResEdits));
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
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                if (document.isEmpty() || semanticModel.isEmpty()) {
                    return new CommonSourceResponse();
                }
                ModulePartNode modulePartNode = document.get().syntaxTree().rootNode();
                LineRange lineRange = service.getCodedata().getLineRange();
                NonTerminalNode node = findNonTerminalNode(service.getCodedata(), document.get());
                if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
                    return new CommonSourceResponse();
                }

                ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
                addServiceAnnotationTextEdits(service, serviceNode, edits);

                Value basePathValue = service.getBasePath();
                if (Objects.nonNull(basePathValue) && basePathValue.isEnabledWithValue()) {
                    String basePath = basePathValue.getValue();
                    NodeList<Node> nodes = serviceNode.absoluteResourcePath();
                    String currentPath = getPath(nodes);
                    if (!currentPath.equals(basePath) && !nodes.isEmpty()) {
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
                    String currentPath = getPath(nodes);
                    if (!currentPath.equals(stringLiteralValue) && !nodes.isEmpty()) {
                        LinePosition startPos = nodes.get(0).lineRange().startLine();
                        LinePosition endPos = nodes.get(nodes.size() - 1).lineRange().endLine();
                        LineRange basePathLineRange = LineRange.from(lineRange.fileName(), startPos, endPos);
                        TextEdit basePathEdit = new TextEdit(Utils.toRange(basePathLineRange), stringLiteralValue);
                        edits.add(basePathEdit);
                    }
                }

                Value listener = service.getListener();
                boolean createDefaultListener = false;
                if (Objects.nonNull(listener) && listener.isEnabledWithValue()) {
                    createDefaultListener = ListenerUtil.createDefaultListener(service.getListener());

                    String listenerName = listener.getValue();
                    Optional<ExpressionNode> listenerExpression = getListenerExpression(serviceNode);
                    if (listenerExpression.isPresent()) {
                        LineRange listenerLineRange = listenerExpression.get().lineRange();
                        TextEdit listenerEdit = new TextEdit(Utils.toRange(listenerLineRange), listenerName);
                        edits.add(listenerEdit);
                    }
                }

                if (createDefaultListener) {
                    List<ImportDeclarationNode> importsList = modulePartNode.imports().stream().toList();
                    LinePosition listenerDeclaringLoc = importsList.isEmpty() ? lineRange.endLine() :
                            importsList.getLast().lineRange().endLine();
                    String listenerDeclarationStmt = ListenerUtil.getHttpDefaultListenerDeclarationStmt(
                            semanticModel.get(), document.get(), listenerDeclaringLoc);
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
                LineRange lineRange = listener.getCodedata().getLineRange();
                NonTerminalNode node = findNonTerminalNode(listener.getCodedata(), document.get());
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
                NonTerminalNode node = findNonTerminalNode(request.codedata(), document.get());
                if (!(node instanceof ClassDefinitionNode classDefinitionNode)) {
                    return new ServiceClassModelResponse();
                }
                ServiceClassUtil.ServiceClassContext context = ServiceClassUtil.ServiceClassContext
                        .valueOf(request.context());
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
     * Add an attribute to the given class or service.
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
                if (!(node instanceof ClassDefinitionNode || node instanceof ServiceDeclarationNode)) {
                    return new CommonSourceResponse();
                }
                LineRange functionLineRange;
                if (node instanceof ServiceDeclarationNode serviceDeclarationNode) {
                    functionLineRange = serviceDeclarationNode.openBraceToken().lineRange();
                } else {
                    ClassDefinitionNode classDefinitionNode = (ClassDefinitionNode) node;
                    functionLineRange = classDefinitionNode.openBrace().lineRange();
                }

                String functionNode = NEW_LINE_WITH_TAB + ServiceClassUtil.buildObjectFiledString(request.field());
                edits.add(new TextEdit(Utils.toRange(functionLineRange.endLine()), functionNode));
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    /**
     * Add an attribute of a class or a service.
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
                LineRange lineRange = request.field().codedata().getLineRange();
                NonTerminalNode node = findNonTerminalNode(request.field().codedata(), document.get());
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
                .getServiceDeclaration(name); // TODO: improve this to use a single query

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

    private static NonTerminalNode findNonTerminalNode(Codedata codedata, Document document) {
        SyntaxTree syntaxTree = document.syntaxTree();
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        TextDocument textDocument = syntaxTree.textDocument();
        LineRange lineRange = codedata.getLineRange();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        return modulePartNode.findNode(TextRange.from(start, end - start), true);
    }
}
