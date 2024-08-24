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
import io.ballerina.flowmodelgenerator.core.AvailableNodesGenerator;
import io.ballerina.flowmodelgenerator.core.ConnectorGenerator;
import io.ballerina.flowmodelgenerator.core.DeleteNodeGenerator;
import io.ballerina.flowmodelgenerator.core.ModelGenerator;
import io.ballerina.flowmodelgenerator.core.NodeTemplateGenerator;
import io.ballerina.flowmodelgenerator.core.SourceGenerator;
import io.ballerina.flowmodelgenerator.core.SuggestedComponentService;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelAvailableNodesRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelGeneratorRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelGetConnectorsRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelNodeTemplateRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelSourceGeneratorRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelSuggestedGenerationRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowNodeDeleteRequest;
import io.ballerina.flowmodelgenerator.extension.request.SuggestedComponentRequest;
import io.ballerina.flowmodelgenerator.extension.response.FlowModelAvailableNodesResponse;
import io.ballerina.flowmodelgenerator.extension.response.FlowModelGeneratorResponse;
import io.ballerina.flowmodelgenerator.extension.response.FlowModelGetConnectorsResponse;
import io.ballerina.flowmodelgenerator.extension.response.FlowModelNodeTemplateResponse;
import io.ballerina.flowmodelgenerator.extension.response.FlowModelSourceGeneratorResponse;
import io.ballerina.flowmodelgenerator.extension.response.FlowNodeDeleteResponse;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Represents the extended language server service for the flow model generator service.
 *
 * @since 1.4.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("flowDesignService")
public class FlowModelGeneratorService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
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
                this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }
                // TODO: Check how we can delegate this to the model generator
                Path projectPath = this.workspaceManager.projectRoot(filePath);
                Optional<Document> dataMappingsDoc;
                try {
                    dataMappingsDoc = this.workspaceManager.document(projectPath.resolve("data_mappings.bal"));
                } catch (Throwable e) {
                    dataMappingsDoc = Optional.empty();
                }

                // Generate the flow design model
                ModelGenerator modelGenerator = new ModelGenerator(semanticModel.get(), document.get(),
                        request.lineRange(), filePath, dataMappingsDoc.orElse(null));
                response.setFlowDesignModel(modelGenerator.getFlowModel());
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
                this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }
                // TODO: Check how we can delegate this to the model generator
                Path projectPath = this.workspaceManager.projectRoot(filePath);
                Optional<Document> dataMappingsDoc;
                try {
                    dataMappingsDoc = this.workspaceManager.document(projectPath.resolve("data_mappings.bal"));
                } catch (Throwable e) {
                    dataMappingsDoc = Optional.empty();
                }

                // Generate the flow design model
                ModelGenerator modelGenerator = new ModelGenerator(semanticModel.get(), document.get(),
                        request.lineRange(), filePath, dataMappingsDoc.orElse(null));
                JsonElement oldFlowModel = modelGenerator.getFlowModel();

                // Create a temporary directory for the in-memory cache
                Path tempDir = Files.createTempDirectory("project-cache");
                Path destinationDir = tempDir.resolve(projectPath.getFileName());

                if (Files.isDirectory(projectPath)) {
                    try (Stream<Path> paths = Files.walk(projectPath)) {
                        paths.forEach(source -> {
                            try {
                                Files.copy(source, destinationDir.resolve(projectPath.relativize(source)),
                                        StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to copy project directory to cache", e);
                            }
                        });
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to walk project directory", e);
                    }
                } else {
                    Files.copy(projectPath, destinationDir, StandardCopyOption.REPLACE_EXISTING);
                }

                Path destination = destinationDir.resolve(projectPath.relativize(projectPath.resolve(filePath)));
                this.workspaceManager.loadProject(destination);
                Optional<SemanticModel> newSemanticModel = this.workspaceManager.semanticModel(destination);
                Optional<Document> newDocument = this.workspaceManager.document(destination);
                if (newSemanticModel.isEmpty() || newDocument.isEmpty()) {
                    return response;
                }
                Path newProjectPath = this.workspaceManager.projectRoot(destination);
                Optional<Document> newDataMappingsDoc;
                try {
                    newDataMappingsDoc = this.workspaceManager.document(newProjectPath.resolve("data_mappings.bal"));
                } catch (Throwable e) {
                    newDataMappingsDoc = Optional.empty();
                }

                TextDocument textDocument = newDocument.get().textDocument();
                int textPosition = textDocument.textPositionFrom(request.position());
                TextEdit textEdit = TextEdit.from(TextRange.from(textPosition, 0), request.text());
                TextDocument apply =
                        textDocument.apply(TextDocumentChange.from(List.of(textEdit).toArray(new TextEdit[0])));
                Document newDoc = newDocument.get().modify()
                        .withContent(String.join(System.lineSeparator(), apply.textLines()))
                        .apply();
                ModelGenerator suggestedModelGenerator =
                        new ModelGenerator(newDoc.module().getCompilation().getSemanticModel(), newDoc,
                                request.lineRange(), destination, newDataMappingsDoc.orElse(null));
                JsonElement newFlowModel = suggestedModelGenerator.getFlowModel();

                JsonArray oldNodes = oldFlowModel.getAsJsonObject().getAsJsonArray("nodes");
                JsonArray newNodes = newFlowModel.getAsJsonObject().getAsJsonArray("nodes");
                markSuggestedNodes(oldNodes, newNodes, 1);
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
                SourceGenerator sourceGenerator = new SourceGenerator(workspaceManager, Path.of(request.filePath()));
                response.setTextEdits(sourceGenerator.toSourceCode(request.flowNode()));
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
                this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }

                AvailableNodesGenerator availableNodesGenerator =
                        new AvailableNodesGenerator(semanticModel.get(), document.get());
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
                NodeTemplateGenerator generator = new NodeTemplateGenerator();
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
    public CompletableFuture<FlowModelGetConnectorsResponse> getConnectors(FlowModelGetConnectorsRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            FlowModelGetConnectorsResponse response = new FlowModelGetConnectorsResponse();
            try {
                ConnectorGenerator connectorGenerator = new ConnectorGenerator();
                response.setCategories(connectorGenerator.getConnectors(request.keyword()));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<FlowNodeDeleteResponse> deleteFlowNode(FlowNodeDeleteRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            FlowNodeDeleteResponse response = new FlowNodeDeleteResponse();
            try {
                Path filePath = Path.of(request.filePath());
                DeleteNodeGenerator deleteNodeGenerator = new DeleteNodeGenerator(request.flowNode(), filePath);
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }
                response.setTextEditsToDelete(
                        deleteNodeGenerator.getTextEditsToDeletedNode(document.get(), project));
            } catch (Throwable e) {
                //TODO: Handle errors generated by the flow model generator service.
                response.setError(e);
            }
            return response;
        });
    }

    private static void markSuggestedNodes(JsonArray oldNodes, JsonArray newNodes, int startIndex) {
        int oldIndex = startIndex;
        int newIndex = startIndex;

        while (oldIndex < oldNodes.size() && newIndex < newNodes.size()) {
            JsonObject oldNode = oldNodes.get(oldIndex).getAsJsonObject();
            JsonObject newNode = newNodes.get(newIndex).getAsJsonObject();

            if (getSourceText(oldNode).equals(getSourceText(newNode))) {
                newNode.addProperty("suggested", false);
                oldIndex++;
                newIndex++;
                continue;
            }

            boolean oldNodeHasBranches = oldNode.has("branches");
            boolean newNodeHasBranches = newNode.has("branches");
            if (oldNodeHasBranches != newNodeHasBranches) {
                newNode.addProperty("suggested", true);
                newIndex++;
                continue;
            }

            if (oldNodeHasBranches) {
                markBranches(oldNode.getAsJsonArray("branches"), newNode.getAsJsonArray("branches"));
                oldIndex++;
                newIndex++;
            }
        }

        while (newIndex < newNodes.size()) {
            newNodes.get(newIndex).getAsJsonObject().addProperty("suggested", true);
            newIndex++;
        }
    }

    private static void markBranches(JsonArray oldBranches, JsonArray newBranches) {
        for (int i = 0; i < newBranches.size(); i++) {
            JsonObject newBranch = newBranches.get(i).getAsJsonObject();
            String newLabel = newBranch.get("label").getAsString();
            boolean labelMatched = false;

            for (int j = 0; j < oldBranches.size(); j++) {
                JsonObject oldBranch = oldBranches.get(j).getAsJsonObject();
                if (oldBranch.get("label").getAsString().equals(newLabel)) {
                    markSuggestedNodes(oldBranch.getAsJsonArray("children"), newBranch.getAsJsonArray("children"), 0);
                    labelMatched = true;
                    break;
                }
            }

            if (!labelMatched) {
                newBranch.addProperty("suggested", true);
            }
        }
    }

    private static String getSourceText(JsonObject oldNode) {
        return oldNode.getAsJsonObject("codedata").get("sourceCode").getAsString();
    }

}
