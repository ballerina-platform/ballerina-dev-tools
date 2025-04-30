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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.flowmodelgenerator.core.AvailableNodesGenerator;
import io.ballerina.flowmodelgenerator.core.CopilotContextGenerator;
import io.ballerina.flowmodelgenerator.core.DeleteNodeHandler;
import io.ballerina.flowmodelgenerator.core.EnclosedNodeFinder;
import io.ballerina.flowmodelgenerator.core.ErrorHandlerGenerator;
import io.ballerina.flowmodelgenerator.core.ModelGenerator;
import io.ballerina.flowmodelgenerator.core.NodeTemplateGenerator;
import io.ballerina.flowmodelgenerator.core.OpenApiServiceGenerator;
import io.ballerina.flowmodelgenerator.core.SourceGenerator;
import io.ballerina.flowmodelgenerator.core.SuggestedComponentService;
import io.ballerina.flowmodelgenerator.core.SuggestedModelGenerator;
import io.ballerina.flowmodelgenerator.core.analyzers.function.ModuleNodeAnalyzer;
import io.ballerina.flowmodelgenerator.core.search.SearchCommand;
import io.ballerina.flowmodelgenerator.core.utils.FileSystemUtils;
import io.ballerina.flowmodelgenerator.extension.request.ComponentDeleteRequest;
import io.ballerina.flowmodelgenerator.extension.request.CopilotContextRequest;
import io.ballerina.flowmodelgenerator.extension.request.EnclosedFuncDefRequest;
import io.ballerina.flowmodelgenerator.extension.request.FilePathRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelAvailableNodesRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelGeneratorRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelNodeTemplateRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelSourceGeneratorRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelSuggestedGenerationRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowNodeDeleteRequest;
import io.ballerina.flowmodelgenerator.extension.request.FunctionDefinitionRequest;
import io.ballerina.flowmodelgenerator.extension.request.OpenAPIServiceGenerationRequest;
import io.ballerina.flowmodelgenerator.extension.request.SearchRequest;
import io.ballerina.flowmodelgenerator.extension.request.ServiceFieldNodesRequest;
import io.ballerina.flowmodelgenerator.extension.request.SuggestedComponentRequest;
import io.ballerina.flowmodelgenerator.extension.response.ComponentDeleteResponse;
import io.ballerina.flowmodelgenerator.extension.response.CopilotContextResponse;
import io.ballerina.flowmodelgenerator.extension.response.EnclosedFuncDefResponse;
import io.ballerina.flowmodelgenerator.extension.response.FlowModelAvailableNodesResponse;
import io.ballerina.flowmodelgenerator.extension.response.FlowModelGeneratorResponse;
import io.ballerina.flowmodelgenerator.extension.response.FlowModelNodeTemplateResponse;
import io.ballerina.flowmodelgenerator.extension.response.FlowModelSourceGeneratorResponse;
import io.ballerina.flowmodelgenerator.extension.response.FlowNodeDeleteResponse;
import io.ballerina.flowmodelgenerator.extension.response.FunctionDefinitionResponse;
import io.ballerina.flowmodelgenerator.extension.response.OpenApiServiceGenerationResponse;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.LSClientLogger;
import org.ballerinalang.langserver.commons.LanguageServerContext;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the extended language server service for the flow model generator service.
 *
 * @since 1.4.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("flowDesignService")
public class FlowModelGeneratorService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;
    private LSClientLogger lsClientLogger;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager,
                     LanguageServerContext serverContext) {
        this.workspaceManager = workspaceManager;
        this.lsClientLogger = LSClientLogger.getInstance(serverContext);
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    @JsonRequest
    public CompletableFuture<FlowModelGeneratorResponse> getFlowModel(FlowModelGeneratorRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            FlowModelGeneratorResponse response = new FlowModelGeneratorResponse();
            try {
                Path filePath = Path.of(request.filePath());

                // Obtain the semantic model and the document
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }
                // TODO: Check how we can delegate this to the model generator
                Path projectPath = this.workspaceManager.projectRoot(filePath);
                Optional<Document> dataMappingsDoc = getDocumentFromFile(projectPath, "data_mappings.bal");
                Optional<Document> functionsDoc = getDocumentFromFile(projectPath, "functions.bal");

                // Generate the flow design model
                ModelGenerator modelGenerator = new ModelGenerator(project, semanticModel.get(), filePath);
                response.setFlowDesignModel(
                        modelGenerator.getFlowModel(document.get(), request.lineRange(),
                                dataMappingsDoc.orElse(null),
                                functionsDoc.orElse(null)));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<FlowModelGeneratorResponse> getSuggestedFlowModel(
            FlowModelSuggestedGenerationRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            FlowModelGeneratorResponse response = new FlowModelGeneratorResponse();
            try {
                Path filePath = Path.of(request.filePath());

                // Obtain the semantic model and the document
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }
                // TODO: Check how we can delegate this to the model generator
                Path projectPath = this.workspaceManager.projectRoot(filePath);
                Optional<Document> dataMappingsDoc = getDocumentFromFile(projectPath, "data_mappings.bal");
                Optional<Document> functionsDoc = getDocumentFromFile(projectPath, "functions.bal");

                // Generate the flow design model
                ModelGenerator modelGenerator = new ModelGenerator(project, semanticModel.get(), filePath);
                JsonElement oldFlowModel =
                        modelGenerator.getFlowModel(document.get(), request.lineRange(),
                                dataMappingsDoc.orElse(null),
                                functionsDoc.orElse(null));

                // Create a temporary directory for the in-memory cache
                Project newProject = project.duplicate();
                DocumentId documentId = newProject.documentId(filePath);
                Module newModule = newProject.currentPackage().module(documentId.moduleId());
                SemanticModel newSemanticModel =
                        PackageUtil.getCompilation(newProject).getSemanticModel(newModule.moduleId());
                Document newDocument = newModule.document(documentId);
                if (newSemanticModel == null || newDocument == null) {
                    return response;
                }
                Optional<Document> newDataMappingsDoc;
                Optional<Document> newFunctionsDoc;
                try {
                    DocumentId dataMappingDocId = newProject.documentId(projectPath.resolve("data_mappings.bal"));
                    Module dataMappingModule = newProject.currentPackage().module(dataMappingDocId.moduleId());
                    newDataMappingsDoc = Optional.of(dataMappingModule.document(dataMappingDocId));

                    DocumentId functionsDocId = newProject.documentId(projectPath.resolve("functions.bal"));
                    Module functionsModule = newProject.currentPackage().module(functionsDocId.moduleId());
                    newFunctionsDoc = Optional.of(functionsModule.document(functionsDocId));
                } catch (Throwable e) {
                    newDataMappingsDoc = Optional.empty();
                    newFunctionsDoc = Optional.empty();
                }

                TextDocument textDocument = newDocument.textDocument();
                int textPosition = textDocument.textPositionFrom(request.position());

                TextEdit textEdit = TextEdit.from(TextRange.from(textPosition, 0), request.text());
                TextDocument newTextDocument =
                        textDocument.apply(TextDocumentChange.from(List.of(textEdit).toArray(new TextEdit[0])));
                Document newDoc = newDocument.modify()
                        .withContent(String.join(System.lineSeparator(), newTextDocument.textLines()))
                        .apply();

                int end = textDocument.textPositionFrom(request.endLine());
                LineRange endLineRange = LineRange.from(request.lineRange().fileName(), request.lineRange().startLine(),
                        newTextDocument.linePositionFrom(end + request.text().length()));

                ModelGenerator suggestedModelGenerator =
                        new ModelGenerator(newProject, PackageUtil.getCompilation(newProject)
                                .getSemanticModel(newDoc.module().moduleId()), filePath);
                JsonElement newFlowModel = suggestedModelGenerator.getFlowModel(newDoc,
                        endLineRange, newDataMappingsDoc.orElse(null), newFunctionsDoc.orElse(null));

                LinePosition endPosition = newTextDocument.linePositionFrom(textPosition + request.text().length());
                LineRange newLineRange =
                        LineRange.from(getRelativePath(projectPath, filePath), request.position(), endPosition);

                JsonArray newNodes = newFlowModel.getAsJsonObject().getAsJsonArray("nodes");
                SuggestedModelGenerator suggestedNodesGenerator =
                        new SuggestedModelGenerator(newDoc, newLineRange, newSemanticModel);
                suggestedNodesGenerator.markSuggestedNodes(newNodes, 1);
                if (!suggestedNodesGenerator.hasSuggestedNodes()) {
                    newFlowModel.getAsJsonObject().add("nodes", new JsonArray());
                }
                response.setFlowDesignModel(newFlowModel);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<JsonObject> getSuggestedComponents(SuggestedComponentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject response = new JsonObject();
            try {
                String fileContent = request.content();
                Path tempDir = Files.createTempDirectory("single-file-project");
                Path tempFilePath = tempDir.resolve("file.bal");
                Files.writeString(tempFilePath, fileContent);

                SuggestedComponentService suggestedComponentService = new SuggestedComponentService();
                Project project = this.workspaceManager.loadProject(tempFilePath);
                return suggestedComponentService.getPackageComponent(project);
            } catch (Throwable e) {
                return response;
            }
        });
    }

    @JsonRequest
    public CompletableFuture<FlowModelSourceGeneratorResponse> getSourceCode(FlowModelSourceGeneratorRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            FlowModelSourceGeneratorResponse response = new FlowModelSourceGeneratorResponse();
            try {
                SourceGenerator sourceGenerator =
                        new SourceGenerator(workspaceManager, Path.of(request.filePath()));
                response.setTextEdits(sourceGenerator.toSourceCode(request.flowNode(), lsClientLogger));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<FlowModelAvailableNodesResponse> getAvailableNodes(
            FlowModelAvailableNodesRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            FlowModelAvailableNodesResponse response = new FlowModelAvailableNodesResponse();
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }

                AvailableNodesGenerator availableNodesGenerator =
                        new AvailableNodesGenerator(semanticModel.get(), document.get(), project.currentPackage());
                response.setCategories(
                        availableNodesGenerator.getAvailableNodes(request.position()));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<FlowModelNodeTemplateResponse> getNodeTemplate(FlowModelNodeTemplateRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            FlowModelNodeTemplateResponse response = new FlowModelNodeTemplateResponse();
            try {
                NodeTemplateGenerator generator = new NodeTemplateGenerator(lsClientLogger);
                Path filePath = Path.of(request.filePath());
                JsonElement nodeTemplate =
                        generator.getNodeTemplate(workspaceManager, filePath, request.position(), request.id());
                response.setFlowNode(nodeTemplate);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<FlowModelGeneratorResponse> getModuleNodes(FilePathRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            FlowModelGeneratorResponse response = new FlowModelGeneratorResponse();
            try {
                Path filePath = Path.of(request.filePath());

                // Obtain the semantic model and the document
                Project project = this.workspaceManager.loadProject(filePath);
                SemanticModel semanticModel = FileSystemUtils.getSemanticModel(workspaceManager, filePath);

                // Generate the flow design model
                ModelGenerator modelGenerator = new ModelGenerator(project, semanticModel, filePath);
                response.setFlowDesignModel(modelGenerator.getModuleNodes());
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<FlowModelGeneratorResponse> getServiceNodes(ServiceFieldNodesRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            FlowModelGeneratorResponse response = new FlowModelGeneratorResponse();
            try {
                Path filePath = Path.of(request.filePath());

                // Obtain the semantic model and the document
                Project project = this.workspaceManager.loadProject(filePath);
                SemanticModel semanticModel = FileSystemUtils.getSemanticModel(workspaceManager, filePath);

                // Generate the flow design model
                ModelGenerator modelGenerator = new ModelGenerator(project, semanticModel, filePath);
                response.setFlowDesignModel(modelGenerator.getServiceFieldNodes(request.linePosition()));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<CopilotContextResponse> getCopilotContext(CopilotContextRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            CopilotContextResponse response = new CopilotContextResponse();
            try {
                Path filePath = Path.of(request.filePath());
                CopilotContextGenerator connectorGenerator =
                        new CopilotContextGenerator(workspaceManager, filePath, request.position());
                connectorGenerator.generate();
                response.setPrefix(connectorGenerator.prefix());
                response.setSuffix(connectorGenerator.suffix());
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    @Deprecated
    // TODO: Need to remove this API and usages must be migrated to `deleteComponent(ComponentDeleteRequest request)`
    public CompletableFuture<FlowNodeDeleteResponse> deleteFlowNode(FlowNodeDeleteRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            FlowNodeDeleteResponse response = new FlowNodeDeleteResponse();
            try {
                Path filePath = Path.of(request.filePath());
                DeleteNodeHandler deleteNodeHandler = new DeleteNodeHandler(request.flowNode(), filePath);
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }
                response.setTextEdits(
                        deleteNodeHandler.getTextEditsToDeletedNode(document.get(), project));
            } catch (Throwable e) {
                //TODO: Handle errors generated by the flow model generator service.
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<ComponentDeleteResponse> deleteComponent(ComponentDeleteRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ComponentDeleteResponse response = new ComponentDeleteResponse();
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }
                response.setTextEdits(DeleteNodeHandler.getTextEditsToDeletedNode(
                        request.component(), filePath, document.get(), project
                ));
            } catch (Throwable e) {
                //TODO: Handle errors generated by the flow model generator service.
                response.setError(e);
            }

            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<OpenApiServiceGenerationResponse> generateServiceFromOpenApiContract(
            OpenAPIServiceGenerationRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            OpenApiServiceGenerationResponse response = new OpenApiServiceGenerationResponse();
            try {
                Path openApiContractPath = Path.of(request.openApiContractPath());
                Path projectPath = Path.of(request.projectPath());
                OpenApiServiceGenerator openApiServiceGenerator = new OpenApiServiceGenerator(openApiContractPath,
                        projectPath, workspaceManager);
                response.setTextEdits(openApiServiceGenerator.generateService(request.name(), request.listeners()));
            } catch (Throwable e) {
                //TODO: Handle errors generated by the flow model generator service.
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<FlowModelSourceGeneratorResponse> addErrorHandler(FilePathRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            FlowModelSourceGeneratorResponse response = new FlowModelSourceGeneratorResponse();
            try {
                ErrorHandlerGenerator errorHandlerGenerator =
                        new ErrorHandlerGenerator(workspaceManager, Path.of(request.filePath()));
                response.setTextEdits(errorHandlerGenerator.getTextEdits());
            } catch (Throwable e) {
                //TODO: Handle errors generated by the flow model generator service.
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<EnclosedFuncDefResponse> getEnclosedFunctionDef(EnclosedFuncDefRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            EnclosedFuncDefResponse response = new EnclosedFuncDefResponse();
            try {
                Path path = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(path);
                Optional<Document> document = this.workspaceManager.document(path);
                if (document.isEmpty()) {
                    return response;
                }
                EnclosedNodeFinder enclosedNodeFinder =
                        new EnclosedNodeFinder(document.get(), request.position(), request.findClass());
                LineRange enclosedRange = enclosedNodeFinder.findEnclosedNode();
                response.setFilePath(project.sourceRoot().resolve(enclosedRange.fileName()).toString());
                response.setStartLine(enclosedRange.startLine());
                response.setEndLine(enclosedRange.endLine());
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<FunctionDefinitionResponse> functionDefinition(FunctionDefinitionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            FunctionDefinitionResponse response = new FunctionDefinitionResponse();
            try {
                // Load the project
                Path projectPath = Path.of(request.projectPath());
                Project project = this.workspaceManager.loadProject(projectPath);

                // Find the document containing the function definition
                Path documentPath = project.kind() == ProjectKind.SINGLE_FILE_PROJECT ? projectPath :
                        projectPath.resolve(request.fileName());
                Optional<Document> optDocument = this.workspaceManager.document(documentPath);
                Optional<SemanticModel> optSemanticModel = this.workspaceManager.semanticModel(documentPath);
                if (optDocument.isEmpty() || optSemanticModel.isEmpty()) {
                    return response;
                }
                Document document = optDocument.get();

                // Analyze the module part nodes to find the respective function definition
                ModuleNodeAnalyzer moduleNodeAnalyzer =
                        new ModuleNodeAnalyzer(ModuleInfo.from(document.module().descriptor()), optSemanticModel.get());
                ModulePartNode rootNode = document.syntaxTree().rootNode();
                Optional<JsonElement> function = moduleNodeAnalyzer.findFunction(rootNode, request.functionName());
                function.ifPresent(response::setFunctionDefinition);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<FlowModelAvailableNodesResponse> search(SearchRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            FlowModelAvailableNodesResponse response = new FlowModelAvailableNodesResponse();
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                SearchCommand.Kind searchKind = SearchCommand.Kind.valueOf(request.searchKind());
                LineRange position = request.position();
                if (request.position() != null) {
                    position = LineRange.from(
                            Optional.ofNullable(filePath.getFileName()).map(Path::toString).orElse(""),
                            request.position().startLine(),
                            request.position().endLine());
                }

                Path projectPath = this.workspaceManager.projectRoot(filePath);
                Optional<Document> functionsDoc = getDocumentFromFile(projectPath, "functions.bal");

                SearchCommand command = SearchCommand.from(searchKind, project, position, request.queryMap(),
                        functionsDoc.orElse(null));
                response.setCategories(command.execute());
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    private static String getRelativePath(Path projectPath, Path filePath) {
        if (projectPath == null || filePath == null) {
            return "";
        }
        if (projectPath.equals(filePath)) {
            Path fileName = filePath.getFileName();
            return fileName != null ? fileName.toString() : "";
        }
        Path relativePath = projectPath.relativize(filePath);
        return relativePath.toString();
    }

    private Optional<Document> getDocumentFromFile(Path projectPath, String fileName) {
        try {
            return this.workspaceManager.document(projectPath.resolve(fileName));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }
}
