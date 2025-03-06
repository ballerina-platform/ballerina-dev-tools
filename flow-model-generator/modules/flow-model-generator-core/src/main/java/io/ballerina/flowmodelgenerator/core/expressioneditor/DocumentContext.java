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

package io.ballerina.flowmodelgenerator.core.expressioneditor;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManagerProxy;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Encapsulates document and import related context with lazy loading capabilities.
 *
 * @since 2.0.0
 */
public class DocumentContext {

    private final WorkspaceManagerProxy workspaceManagerProxy;
    private final String inputFileUri;
    private final Path inputFilePath;

    private String fileUri;
    private Path filePath;
    private Project project;
    private Document document;
    private Module module;
    private SemanticModel semanticModel;
    private List<ImportDeclarationNode> imports;
    private WorkspaceManager workspaceManager;
    private boolean initialized;

    public DocumentContext(WorkspaceManagerProxy workspaceManagerProxy, Path filePath) {
        this(workspaceManagerProxy, null, filePath, null);
    }

    public DocumentContext(WorkspaceManagerProxy workspaceManagerProxy, String fileUri, Path filePath) {
        this(workspaceManagerProxy, fileUri, filePath, null);
    }

    public DocumentContext(WorkspaceManagerProxy workspaceManagerProxy, String fileUri, Path filePath,
                           Document document) {
        this.workspaceManagerProxy = workspaceManagerProxy;
        this.inputFileUri = fileUri;
        this.inputFilePath = filePath;
        this.document = document;
        this.initialized = false;
    }

    public WorkspaceManager workspaceManager() {
        if (workspaceManager == null) {
            fileUri = inputFileUri == null ? CommonUtils.getExprUri(inputFilePath.toString()) : inputFileUri;
            workspaceManager = workspaceManagerProxy.get(fileUri);
        }
        return workspaceManager;
    }

    public Optional<Project> project() {
        if (project != null) {
            return Optional.of(project);
        }
        try {
            project = workspaceManager().loadProject(inputFilePath);
            return Optional.of(project);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public Optional<Module> module() {
        if (module != null) {
            return Optional.of(module);
        }
        initialize();
        Optional<Module> optModule = workspaceManager().module(filePath);
        optModule.ifPresent(mod -> module = mod);
        return optModule;
    }

    public Optional<SemanticModel> semanticModel() {
        if (semanticModel != null) {
            return Optional.of(semanticModel);
        }
        initialize();
        Optional<SemanticModel> optSemanticModel = workspaceManager().semanticModel(filePath);
        optSemanticModel.ifPresent(model -> semanticModel = model);
        return optSemanticModel;
    }

    public String fileUri() {
        if (fileUri == null) {
            initialize();
        }
        return fileUri;
    }

    public Document document() {
        if (document == null) {
            fileUri();
        }
        return document;
    }

    public Path filePath() {
        if (filePath == null) {
            initialize();
        }
        return filePath;
    }

    public List<ImportDeclarationNode> imports() {
        if (imports == null) {
            initialize();
            SyntaxTree syntaxTree = document.syntaxTree();
            imports = syntaxTree.rootNode().kind() == SyntaxKind.MODULE_PART
                    ? ((ModulePartNode) syntaxTree.rootNode()).imports().stream().toList()
                    : List.of();

        }
        return imports;
    }

    private void initialize() {
        if (initialized) {
            return;
        }
        // Check if the document exists
        Optional<Document> inputDoc = CommonUtils.getDocument(workspaceManager(), inputFilePath);
        if (inputDoc.isPresent()) {
            document = inputDoc.get();
            filePath = inputFilePath;
            fileUri = fileUri == null ? CommonUtils.getExprUri(filePath.toString()) : inputFileUri;
            return;
        }

        // Generate the reserved file if not exists
        Optional<Module> optModule = workspaceManager().module(inputFilePath);
        if (optModule.isPresent()) {
            module = optModule.get();
        } else {
            // Get the default module if not exists
            Optional<Project> project = project();
            if (project.isEmpty()) {
                throw new IllegalStateException("Project not found for the file: " + inputFilePath);
            }
            module = project.get().currentPackage().getDefaultModule();
        }

        // If the file is not found, it defaults to the end of a random file. Although we can create a
        // private document using the project API, this approach is not feasible because the
        // BallerinaWorkspaceManager is tightly coupled with the file system.
        // Get the first document ID from the module
        Collection<DocumentId> documentIds = module.documentIds();
        if (documentIds.isEmpty()) {
            throw new IllegalStateException("No documents found in the module: " + module.moduleName());
        }
        DocumentId documentId = documentIds.iterator().next();
        document = module.document(documentId);
        filePath = inputFilePath.resolve(document.name());
        fileUri = CommonUtils.getExprUri(filePath.toString());
    }
}
