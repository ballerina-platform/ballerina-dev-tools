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

import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ImportOrgNameNode;
import io.ballerina.compiler.syntax.tree.ImportPrefixNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.flowmodelgenerator.extension.request.CreateFilesRequest;
import io.ballerina.flowmodelgenerator.extension.response.CommonSourceResponse;
import io.ballerina.flowmodelgenerator.extension.response.ICPEnabledResponse;
import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.toml.semantic.ast.TomlKeyValueNode;
import io.ballerina.toml.semantic.ast.TomlTableNode;
import io.ballerina.toml.semantic.ast.TopLevelNode;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for enabling ICP.
 *
 * @since 2.0.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("icpEnabler")
public class ICPEnablerService implements ExtendedLanguageServerService {

    private static final String BALLERINAX = "ballerinax";
    private static final String MODULE_NAME = "wso2.controlplane";

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
                Path filePath = Path.of(request.projectPath());
                Project project = this.workspaceManager.loadProject(filePath);
                Package pkg = project.currentPackage();
                Module defaultModule = pkg.getDefaultModule();
                Collection<DocumentId> documentIds = defaultModule.documentIds();
                boolean hasCorrectImport = false;
                for (DocumentId documentId : documentIds) {
                    if (hasCorrectImport) {
                        break;
                    }
                    Document document = defaultModule.document(documentId);
                    ModulePartNode root = document.syntaxTree().rootNode();
                    NodeList<ImportDeclarationNode> imports = root.imports();
                    for (ImportDeclarationNode importNode : imports) {
                        if (validOrg(importNode) && validModuleName(importNode) && validPrefix(importNode)) {
                            hasCorrectImport = true;
                            break;
                        }
                    }
                }
                if (!hasCorrectImport) {
                    response.setEnabled(false);
                    return response;
                }
                Optional<BallerinaToml> ballerinaToml = pkg.ballerinaToml();
                if (ballerinaToml.isEmpty()) {
                    throw new RuntimeException("Ballerina.toml not found");
                }
                TomlTableNode tomlTableNode = ballerinaToml.get().tomlAstNode();
                TopLevelNode topLevelNode = tomlTableNode.entries().get("build-options");
                if (topLevelNode instanceof TomlTableNode buildOptions) {
                    TopLevelNode icpNode = buildOptions.entries().get("remoteManagement");
                    if (icpNode instanceof TomlKeyValueNode keyValueNode) {
                        String value = keyValueNode.value().toNativeValue().toString();
                        if (value.trim().equals("true")) {
                            response.setEnabled(true);
                            return response;
                        }
                    }
                }
                response.setEnabled(false);
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

    private static boolean validOrg(ImportDeclarationNode importNode) {
        Optional<ImportOrgNameNode> importOrgNameNode = importNode.orgName();
        return importOrgNameNode.isPresent() && importOrgNameNode.get().orgName().text().trim().equals(BALLERINAX);
    }

    private static boolean validModuleName(ImportDeclarationNode importNode) {
        SeparatedNodeList<IdentifierToken> identifierTokens = importNode.moduleName();
        return identifierTokens.stream().map(Node::toSourceCode).map(String::trim)
                .collect(Collectors.joining(".")).equals(MODULE_NAME);
    }

    private static boolean validPrefix(ImportDeclarationNode importNode) {
        Optional<ImportPrefixNode> prefix = importNode.prefix();
        return prefix.isPresent() && prefix.get().prefix().text().trim().equals("_");
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }
}
