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

package io.ballerina.flowmodelgenerator.extension;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.TypesManager;
import io.ballerina.flowmodelgenerator.core.converters.JsonToRecordMapper;
import io.ballerina.flowmodelgenerator.extension.request.JsonToRecordRequest;
import io.ballerina.flowmodelgenerator.extension.response.JsonToRecordResponse;
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

/**
 * The extended service for the JsonToBalRecord endpoint.
 *
 * @since 2.0.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("jsonToRecordTypes")
public class JsonToRecordConverterService implements ExtendedLanguageServerService {
    private WorkspaceManager workspaceManager;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        ExtendedLanguageServerService.super.init(langServer, workspaceManager);
        this.workspaceManager = workspaceManager;
    }

    @Override
    public Class<?> getRemoteInterface() {
        return getClass();
    }

    @JsonRequest
    public CompletableFuture<JsonToRecordResponse> convert(JsonToRecordRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            JsonToRecordResponse response = new JsonToRecordResponse();

            String jsonString = request.getJsonString();
            String recordName = request.getRecordName();
            String prefix = request.getPrefix();
            boolean isRecordTypeDesc = request.getIsRecordTypeDesc();
            boolean isClosed = request.getIsClosed();
            boolean forceFormatRecordFields = request.getForceFormatRecordFields();
            boolean isNullAsOptional = request.getIsNullAsOptional();

            try {
                Path filePath = Path.of(request.getFilePathUri());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                if (document.isEmpty() || semanticModel.isEmpty()) {
                    return response;
                }
                TypesManager typesManager = new TypesManager(document.get());
                JsonToRecordMapper jsonToRecordMapper = new JsonToRecordMapper(recordName, prefix, project,
                        document.get(), filePath, typesManager, semanticModel.get());
                response.setTypes(jsonToRecordMapper.convert(jsonString, isRecordTypeDesc, isClosed,
                        forceFormatRecordFields, workspaceManager, isNullAsOptional));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @Override
    public String getName() {
        return "jsonToRecordTypes";
    }
}
