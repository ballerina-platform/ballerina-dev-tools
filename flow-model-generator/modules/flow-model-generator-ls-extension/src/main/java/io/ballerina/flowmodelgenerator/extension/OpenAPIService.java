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

import io.ballerina.flowmodelgenerator.core.OpenAPIClientGenerator;
import io.ballerina.flowmodelgenerator.extension.request.OpenAPIClientGenerationRequest;
import io.ballerina.flowmodelgenerator.extension.request.OpenAPIGeneratedModulesRequest;
import io.ballerina.flowmodelgenerator.extension.response.OpenAPIClientGenerationResponse;
import io.ballerina.flowmodelgenerator.extension.response.OpenAPIGeneratedModulesResponse;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("openAPIService")
public class OpenAPIService implements ExtendedLanguageServerService {

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    @JsonRequest
    public CompletableFuture<OpenAPIClientGenerationResponse> genClient(OpenAPIClientGenerationRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            OpenAPIClientGenerationResponse response = new OpenAPIClientGenerationResponse();
            try {
                OpenAPIClientGenerator openAPIClientGenerator =
                        new OpenAPIClientGenerator(Path.of(req.openApiContractPath()), Path.of(req.projectPath()));
                response.setSource(openAPIClientGenerator.genClient(req.module()));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<OpenAPIGeneratedModulesResponse> getModules(OpenAPIGeneratedModulesRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            OpenAPIGeneratedModulesResponse response = new OpenAPIGeneratedModulesResponse();
            try {
                OpenAPIClientGenerator openAPIClientGenerator =
                        new OpenAPIClientGenerator(null, Path.of(req.projectPath()));
                response.setModules(openAPIClientGenerator.getModules());
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }
}
