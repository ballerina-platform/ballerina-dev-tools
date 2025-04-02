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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.ConfigVariablesManager;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariablesGetRequest;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariablesUpdateRequest;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariablesResponse;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariablesUpdateResponse;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("configEditor")
public class ConfigEditorService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;
    private Gson gson;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
        this.gson = new Gson();
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariablesResponse> getConfigVariables(ConfigVariablesGetRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariablesResponse response = new ConfigVariablesResponse();
            try {
                Path projectFolder = Path.of(request.projectPath());
                List<Path> filePaths = new ArrayList<>();
                Files.walkFileTree(projectFolder, new FileReader(filePaths));

                Map<Document, SemanticModel> documentSemanticModelMap = new HashMap<>();
                for (Path filePath : filePaths) {
                    this.workspaceManager.loadProject(filePath);
                    Optional<Document> document = this.workspaceManager.document(filePath);
                    Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                    if (document.isEmpty() || semanticModel.isEmpty()) {
                        return response;
                    }
                    documentSemanticModelMap.put(document.get(), semanticModel.get());
                }

                ConfigVariablesManager configVariablesManager = new ConfigVariablesManager();
                response.setConfigVariables(configVariablesManager.get(documentSemanticModelMap));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariablesUpdateResponse> updateConfigVariables(ConfigVariablesUpdateRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariablesUpdateResponse response = new ConfigVariablesUpdateResponse();
            try {
                FlowNode configVariable = gson.fromJson(req.configVariable(), FlowNode.class);
                String variableFileName = configVariable.codedata().lineRange().fileName();
                Path configFilePath = Path.of(req.configFilePath());
                Project project = this.workspaceManager.loadProject(configFilePath);

                Path variableFilePath = null;
                for (Module module : project.currentPackage().modules()) {
                    for (DocumentId documentId : module.documentIds()) {
                        Document document = module.document(documentId);
                        if (document.name().equals(variableFileName)) {
                            variableFilePath = project.sourceRoot().resolve(document.syntaxTree().filePath());
                        }
                    }
                }
                if (Objects.isNull(variableFilePath)) {
                    return response;
                }

                Optional<Document> document = this.workspaceManager.document(variableFilePath);
                if (document.isEmpty()) {
                    return response;
                }

                ConfigVariablesManager configVariablesManager = new ConfigVariablesManager();
                JsonElement textEdits = configVariablesManager.update(document.get(), variableFilePath, configVariable);
                response.setTextEdits(textEdits);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    private static class FileReader extends SimpleFileVisitor<Path> {

        List<Path> filePaths;

        public FileReader(List<Path> filePaths) {
            this.filePaths = filePaths;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file.toString().endsWith(".bal")) {
                filePaths.add(file);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
