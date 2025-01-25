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

package io.ballerina.testmanagerservice.extension;

import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.testmanagerservice.extension.request.TestsDiscoveryRequest;
import io.ballerina.testmanagerservice.extension.response.FileTestsDiscoveryResponse;
import io.ballerina.testmanagerservice.extension.response.ProjectTestsDiscoveryResponse;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the extended language server service for the test manager service.
 *
 * @since 2.0.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("testManagerService")
public class TestManagerService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }


    /**
     * Discovers tests in a file.
     *
     * @param request the request to discover tests in a file
     * @return the response to discover tests in a file
     */
    @JsonRequest
    public CompletableFuture<FileTestsDiscoveryResponse> discoverInFile(TestsDiscoveryRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                io.ballerina.projects.Package currentPackage = project.currentPackage();
                return FileTestsDiscoveryResponse.from();
            } catch (Throwable e) {
                return FileTestsDiscoveryResponse.from(e);
            }
        });
    }

    /**
     * Discovers tests in a project.
     *
     * @param request the request to discover tests in a project
     * @return the response to discover tests in a project
     */
    @JsonRequest
    public CompletableFuture<ProjectTestsDiscoveryResponse> discoverInProject(TestsDiscoveryRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                io.ballerina.projects.Package currentPackage = project.currentPackage();
                Module defaultModule = currentPackage.getDefaultModule();
                ModuleTestDetailsHolder moduleTestDetailsHolder = new ModuleTestDetailsHolder();
                for (DocumentId documentId : defaultModule.testDocumentIds()) {
                    TestFunctionsFinder testFunctionsFinder = new TestFunctionsFinder(
                            defaultModule.document(documentId), moduleTestDetailsHolder);
                    testFunctionsFinder.find();
                }
                return ProjectTestsDiscoveryResponse.from(moduleTestDetailsHolder.getGroupsToFunctions());
            } catch (Throwable e) {
                return ProjectTestsDiscoveryResponse.from(e);
            }
        });
    }
}
