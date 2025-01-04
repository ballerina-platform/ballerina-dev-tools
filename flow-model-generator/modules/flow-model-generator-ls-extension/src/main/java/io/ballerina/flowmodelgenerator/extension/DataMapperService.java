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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.DataMapManager;
import io.ballerina.flowmodelgenerator.extension.request.DataMapperAddElementRequest;
import io.ballerina.flowmodelgenerator.extension.request.DataMapperModelRequest;
import io.ballerina.flowmodelgenerator.extension.request.DataMapperQueryConvertRequest;
import io.ballerina.flowmodelgenerator.extension.request.DataMapperSourceRequest;
import io.ballerina.flowmodelgenerator.extension.request.DataMapperTypesRequest;
import io.ballerina.flowmodelgenerator.extension.request.DataMapperVisualizeRequest;
import io.ballerina.flowmodelgenerator.extension.response.DataMapperAddElementResponse;
import io.ballerina.flowmodelgenerator.extension.response.DataMapperModelResponse;
import io.ballerina.flowmodelgenerator.extension.response.DataMapperSourceResponse;
import io.ballerina.flowmodelgenerator.extension.response.DataMapperTypesResponse;
import io.ballerina.flowmodelgenerator.extension.response.DataMapperVisualizeResponse;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("dataMapper")
public class DataMapperService implements ExtendedLanguageServerService {

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
    public CompletableFuture<DataMapperTypesResponse> types(DataMapperTypesRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            DataMapperTypesResponse response = new DataMapperTypesResponse();
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }

                DataMapManager dataMapManager = new DataMapManager(this.workspaceManager, semanticModel.get(),
                        document.get());
                response.setType(dataMapManager.getTypes(request.flowNode(), request.propertyKey()));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<DataMapperModelResponse> mappings(DataMapperModelRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            DataMapperModelResponse response = new DataMapperModelResponse();
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }

                DataMapManager dataMapManager = new DataMapManager(this.workspaceManager, semanticModel.get(),
                        document.get());
                response.setMappingsModel(dataMapManager.getMappings(request.flowNode(), request.position(),
                        request.propertyKey(), Path.of(request.filePath()), request.targetField(), project));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<DataMapperSourceResponse> getSource(DataMapperSourceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            DataMapperSourceResponse response = new DataMapperSourceResponse();
            try {
                DataMapManager dataMapManager = new DataMapManager(null, null, null);
                response.setSource(dataMapManager.getSource(request.mappings(), request.flowNode(),
                        request.targetField()));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<DataMapperSourceResponse> convertToQuery(DataMapperQueryConvertRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            DataMapperSourceResponse response = new DataMapperSourceResponse();
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }
                DataMapManager dataMapManager = new DataMapManager(null, null, document.get());
                response.setSource(dataMapManager.getQuery(request.flowNode(), request.targetField(),
                        Path.of(request.filePath()), request.position(), project));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<DataMapperVisualizeResponse> visualizable(DataMapperVisualizeRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            DataMapperVisualizeResponse response = new DataMapperVisualizeResponse();
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }
                DataMapManager dataMapManager = new DataMapManager(workspaceManager, semanticModel.get(),
                        document.get());
                response.setVisualizableProperties(dataMapManager.getVisualizableProperties(request.flowNode(),
                        project, filePath, request.position()));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<DataMapperAddElementResponse> addElement(DataMapperAddElementRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            DataMapperAddElementResponse response = new DataMapperAddElementResponse();
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }
                DataMapManager dataMapManager = new DataMapManager(workspaceManager, semanticModel.get(),
                        document.get());
                response.setSource(dataMapManager.addElement(request.flowNode(), request.propertyKey(),
                        Path.of(request.filePath()), request.targetField(), project, request.position()));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }
}
