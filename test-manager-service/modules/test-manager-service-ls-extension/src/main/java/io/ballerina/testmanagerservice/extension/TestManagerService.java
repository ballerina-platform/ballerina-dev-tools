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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.testmanagerservice.extension.request.AddTestFunctionRequest;
import io.ballerina.testmanagerservice.extension.request.GetTestFunctionRequest;
import io.ballerina.testmanagerservice.extension.request.TestsDiscoveryRequest;
import io.ballerina.testmanagerservice.extension.request.UpdateTestFunctionRequest;
import io.ballerina.testmanagerservice.extension.response.CommonSourceResponse;
import io.ballerina.testmanagerservice.extension.response.GetTestFunctionResponse;
import io.ballerina.testmanagerservice.extension.response.TestsDiscoveryResponse;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public CompletableFuture<TestsDiscoveryResponse> discoverInFile(TestsDiscoveryRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    throw new RuntimeException("Test document not found: " + filePath);
                }
                ModuleTestDetailsHolder moduleTestDetailsHolder = new ModuleTestDetailsHolder();
                TestFunctionsFinder testFunctionsFinder = new TestFunctionsFinder(document.get(),
                        moduleTestDetailsHolder);
                testFunctionsFinder.find();
                return TestsDiscoveryResponse.from(moduleTestDetailsHolder.getGroupsToFunctions());
            } catch (Throwable e) {
                return TestsDiscoveryResponse.from(e);
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
    public CompletableFuture<TestsDiscoveryResponse> discoverInProject(TestsDiscoveryRequest request) {
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
                return TestsDiscoveryResponse.from(moduleTestDetailsHolder.getGroupsToFunctions());
            } catch (Throwable e) {
                return TestsDiscoveryResponse.from(e);
            }
        });
    }

    /**
     * Get the test function model for the given test function.
     *
     * @param request the request to get the test function model
     * @return the response to get the test function model
     */
    @JsonRequest
    public CompletableFuture<GetTestFunctionResponse> getTestFunction(GetTestFunctionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                if (document.isEmpty() || semanticModel.isEmpty()) {
                    return GetTestFunctionResponse.get();
                }
                ModulePartNode modulePartNode = document.get().syntaxTree().rootNode();
                Optional<FunctionDefinitionNode> matchingFunc = modulePartNode.members().stream()
                        .filter(mem -> mem instanceof FunctionDefinitionNode)
                        .map(mem -> (FunctionDefinitionNode) mem)
                        .filter(mem -> mem.functionName().text().trim().equals(request.functionName()))
                        .findFirst();

                return matchingFunc.map(functionDefinitionNode -> GetTestFunctionResponse.from(
                                Utils.getTestFunctionModel(functionDefinitionNode, semanticModel.get())))
                        .orElseGet(GetTestFunctionResponse::get);
            } catch (Throwable e) {
                return GetTestFunctionResponse.from(e);
            }
        });
    }

    /**
     * Add a test function to the given test function.
     *
     * @param request the request to get the test function model
     * @return the response to get the test function model
     */
    @JsonRequest
    public CompletableFuture<CommonSourceResponse> addTestFunction(AddTestFunctionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new CommonSourceResponse();
                }
                ModulePartNode modulePartNode = document.get().syntaxTree().rootNode();
                LineRange lineRange = modulePartNode.lineRange();
                List<TextEdit> edits = new ArrayList<>();
                if (!Utils.isTestModuleImportExists(modulePartNode)) {
                    edits.add(new TextEdit(Utils.toRange(lineRange.startLine()), Constants.IMPORT_TEST_STMT));
                }
                String function = Utils.getTestFunctionTemplate(request.function());
                edits.add(new TextEdit(Utils.toRange(lineRange.endLine()), function));
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }

    /**
     * Update the test function model for the given test function.
     *
     * @param request the request to get the test function model
     * @return the response to get the test function model
     */
    @JsonRequest
    public CompletableFuture<CommonSourceResponse> updateTestFunction(UpdateTestFunctionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new CommonSourceResponse();
                }
                TextDocument textDocument = document.get().syntaxTree().textDocument();
                ModulePartNode modulePartNode = document.get().syntaxTree().rootNode();
                LineRange lineRange = request.function()
                        .codedata().lineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (!(node instanceof FunctionDefinitionNode functionDefinitionNode)) {
                    return new CommonSourceResponse();
                }

                List<TextEdit> edits = new ArrayList<>();
                String functionName = functionDefinitionNode.functionName().text().trim();
                LineRange nameRange = functionDefinitionNode.functionName().lineRange();
                if (!functionName.equals(request.function().functionName().value())) {
                    edits.add(new TextEdit(Utils.toRange(nameRange),
                            request.function().functionName().value().toString()));
                }

                LineRange signatureRange = functionDefinitionNode.functionSignature().lineRange();
                String functionSignature = Utils.buildFunctionSignature(request.function());
                edits.add(new TextEdit(Utils.toRange(signatureRange), functionSignature));
                return new CommonSourceResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new CommonSourceResponse(e);
            }
        });
    }
}
