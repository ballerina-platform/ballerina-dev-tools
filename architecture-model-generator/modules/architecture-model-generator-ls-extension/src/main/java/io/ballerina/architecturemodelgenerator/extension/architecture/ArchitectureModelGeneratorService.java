/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.architecturemodelgenerator.extension.architecture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.ballerina.architecturemodelgenerator.core.ArchitectureModel;
import io.ballerina.architecturemodelgenerator.core.ArchitectureModelBuilder;
import io.ballerina.architecturemodelgenerator.core.diagnostics.ArchitectureModelException;
import io.ballerina.architecturemodelgenerator.core.diagnostics.DiagnosticMessage;
import io.ballerina.architecturemodelgenerator.core.diagnostics.DiagnosticUtils;
import io.ballerina.architecturemodelgenerator.extension.Utils;
import io.ballerina.projects.Project;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The extended service for generation solution architecture model.
 *
 * @since 2201.2.2
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("projectDesignService")
public class ArchitectureModelGeneratorService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @Override
    public Class<?> getRemoteInterface() {
        return getClass();
    }

    @JsonRequest
    public CompletableFuture<ArchitectureModelResponse> getProjectComponentModels
            (ArchitectureModelRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            ArchitectureModelResponse response = new ArchitectureModelResponse();
            Map<String, JsonObject> componentModelMap = new HashMap<>();
            for (String documentUri : request.getDocumentUris()) {
                Path path = Path.of(documentUri);
                try {
                    Project project = getCurrentProject(path);
                    if (!Utils.modelAlreadyExists(componentModelMap, project.currentPackage())) {
                        ArchitectureModelBuilder architectureModelBuilder = new ArchitectureModelBuilder();
                        ArchitectureModel projectModel = architectureModelBuilder
                                .constructComponentModel(project.currentPackage());
                        Gson gson = new GsonBuilder().serializeNulls().create();
                        JsonObject componentModelJson = (JsonObject) gson.toJsonTree(projectModel);
                        componentModelMap.put(projectModel.getId(), componentModelJson);
                    }
                } catch (ArchitectureModelException | WorkspaceDocumentException | EventSyncException e) {
                    // todo : Improve error messages
                    DiagnosticMessage message = DiagnosticMessage.ballerinaProjectNotFound(documentUri);
                    response.addDiagnostics
                            (DiagnosticUtils.getDiagnosticResponse(List.of(message), response.getDiagnostics()));
                } catch (Exception e) {
                    DiagnosticMessage message = DiagnosticMessage.failedToResolveBallerinaPackage(
                            e.getMessage(), Arrays.toString(e.getStackTrace()), documentUri);
                    response.addDiagnostics
                            (DiagnosticUtils.getDiagnosticResponse(List.of(message), response.getDiagnostics()));
                }
            }
            response.setComponentModels(componentModelMap);
            return response;
        });
    }

    private Project getCurrentProject(Path path) throws ArchitectureModelException, WorkspaceDocumentException,
            EventSyncException {

        Optional<Project> project = workspaceManager.project(path);
        if (project.isEmpty()) {
            return workspaceManager.loadProject(path);
        }
        return project.get();
    }
}
