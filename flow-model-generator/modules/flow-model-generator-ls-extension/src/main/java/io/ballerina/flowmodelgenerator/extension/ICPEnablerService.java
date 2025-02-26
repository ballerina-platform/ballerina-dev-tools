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

import com.google.gson.Gson;
import io.ballerina.flowmodelgenerator.extension.request.CreateFilesRequest;
import io.ballerina.flowmodelgenerator.extension.response.CommonSourceResponse;
import io.ballerina.flowmodelgenerator.extension.response.CreateFilesResponse;
import io.ballerina.flowmodelgenerator.extension.response.ICPEnabledResponse;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for enabling ICP.
 *
 * @since 2.0.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("icpEnabler")
public class ICPEnablerService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @JsonRequest
    public CompletableFuture<ICPEnabledResponse> isIcpEnabled(CreateFilesRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ICPEnabledResponse response = new ICPEnabledResponse();
            try {
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<CommonSourceResponse> addICP(CreateFilesRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            CommonSourceResponse response = new CommonSourceResponse();
            try {
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }
}
