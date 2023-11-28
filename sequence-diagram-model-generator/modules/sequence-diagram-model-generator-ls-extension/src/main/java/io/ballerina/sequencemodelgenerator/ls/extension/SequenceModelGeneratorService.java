/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.sequencemodelgenerator.ls.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.projects.Project;
import io.ballerina.sequencemodelgenerator.core.ModelGenerator;
import io.ballerina.sequencemodelgenerator.core.exception.SequenceModelGenerationException;
import io.ballerina.sequencemodelgenerator.core.model.SequenceModel;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.ballerina.sequencemodelgenerator.core.Constants.EMPTY_SEMANTIC_MODEL_MSG;
import static io.ballerina.sequencemodelgenerator.core.Constants.ISSUE_IN_MODEL_GENERATION;

/**
 * Extended language server service for the sequence diagram model generator service.
 *
 * @since 2201.8.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("sequenceModelGeneratorService")
public class SequenceModelGeneratorService implements ExtendedLanguageServerService {
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
    public CompletableFuture<SequenceDiagramServiceResponse>
        getSequenceDiagramModel(SequenceDiagramServiceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            SequenceDiagramServiceResponse response = new SequenceDiagramServiceResponse();
            try {
                Path filePath = Path.of(request.getFilePath());
                Project project = getCurrentProject(filePath);
                if (this.workspaceManager.semanticModel(filePath).isEmpty()) {
                    ModelDiagnostic modelDiagnostic = new ModelDiagnostic(true,
                            EMPTY_SEMANTIC_MODEL_MSG);
                    response.setModelDiagnostic(modelDiagnostic);
                } else {
                    SemanticModel semanticModel = this.workspaceManager.semanticModel(filePath).get();
                    ModelGenerator modelGenerator = new ModelGenerator();
                    SequenceModel sequenceModel = modelGenerator.getSequenceDiagramModel(project,
                            request.getLineRange(), semanticModel);
                    Gson gson = new GsonBuilder().create();
                    JsonElement sequenceModelJson = gson.toJsonTree(sequenceModel);
                    response.setSequenceDiagramModel(sequenceModelJson);
                }
            } catch (WorkspaceDocumentException | EventSyncException e) {
                ModelDiagnostic modelDiagnostic = new ModelDiagnostic(true,
                        e.getMessage());
                response.setModelDiagnostic(modelDiagnostic);
            } catch (SequenceModelGenerationException e) {
                ModelDiagnostic modelDiagnostic = new ModelDiagnostic(true,
                        String.format(ISSUE_IN_MODEL_GENERATION, e.getMessage()));
                response.setModelDiagnostic(modelDiagnostic);
            }
            return response;
        });
    }

    private Project getCurrentProject(Path path) throws WorkspaceDocumentException, EventSyncException {
        Optional<Project> project = workspaceManager.project(path);
        if (project.isEmpty()) {
            return workspaceManager.loadProject(path);
        }
        return project.get();
    }

}
