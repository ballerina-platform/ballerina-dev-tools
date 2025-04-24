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

package io.ballerina.servicemodelgenerator.extension.diagnostics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.ModuleName;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorService;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.request.FunctionModifierRequest;
import io.ballerina.servicemodelgenerator.extension.request.FunctionSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceDesignerDiagnosticRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceModifierRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceSourceRequest;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.servicemodelgenerator.extension.diagnostics.ResourceFunctionFormValidator.Context.ADD_RESOURCE;
import static io.ballerina.servicemodelgenerator.extension.diagnostics.ResourceFunctionFormValidator.Context.UPDATE_RESOURCE;
import static io.ballerina.servicemodelgenerator.extension.diagnostics.ServiceValidator.validateBasePath;

/**
 * Diagnostics handler for the service model generator.
 *
 * @since 2.3.0
 */
public class DiagnosticsHandler {

    final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final WorkspaceManager workspaceManager;

    public DiagnosticsHandler(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public JsonElement getDiagnostics(ServiceDesignerDiagnosticRequest request) throws WorkspaceDocumentException,
            EventSyncException {
        switch (request.operation()) {
            case "addService" -> {
                ServiceSourceRequest serviceRequest = gson.fromJson(request.request(), ServiceSourceRequest.class);
                validateBasePath(serviceRequest.service());
                return gson.toJsonTree(serviceRequest);
            }
            case "updateService" -> {
                ServiceModifierRequest serviceRequest = gson.fromJson(request.request(), ServiceModifierRequest.class);
                validateBasePath(serviceRequest.service());
                return gson.toJsonTree(serviceRequest);
            }
            case "addResource" -> {
                FunctionSourceRequest function = gson.fromJson(request.request(), FunctionSourceRequest.class);
                Path filePath = Path.of(function.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return request.request();
                }

                Package currentPackage = project.currentPackage();
                Module module = currentPackage.module(ModuleName.from(currentPackage.packageName()));
                ModuleId moduleId = module.moduleId();
                SemanticModel semanticModel = PackageUtil.getCompilation(currentPackage).getSemanticModel(moduleId);

                NonTerminalNode node = findNonTerminalNode(function.codedata(), document.get());
                if (!(node instanceof ServiceDeclarationNode serviceDeclarationNode)) {
                    return request.request();
                }

                new ResourceFunctionFormValidator(ADD_RESOURCE, semanticModel, document.get()).validate(
                        function.function(), serviceDeclarationNode);
                return gson.toJsonTree(function);
            }
            case "updateFunction" -> {
                FunctionModifierRequest function = gson.fromJson(request.request(), FunctionModifierRequest.class);
                Path filePath = Path.of(function.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return request.request();
                }
                NonTerminalNode node = findNonTerminalNode(function.function().getCodedata(), document.get());
                if (!(node instanceof FunctionDefinitionNode functionDefinitionNode)) {
                    return request.request();
                }

                Package currentPackage = project.currentPackage();
                Module module = currentPackage.module(ModuleName.from(currentPackage.packageName()));
                ModuleId moduleId = module.moduleId();
                SemanticModel semanticModel = PackageUtil.getCompilation(currentPackage).getSemanticModel(moduleId);

                while (!(node instanceof ServiceDeclarationNode serviceDeclarationNode)) {
                    if (node == null) {
                        return request.request();
                    }
                    node = node.parent();
                }

                ServiceModelGeneratorService.ModuleAndServiceType moduleAndServiceType = ServiceModelGeneratorService.
                        deriveServiceType(serviceDeclarationNode, semanticModel);

                if ("http".equals(moduleAndServiceType.moduleName())
                        && "Service".equals(moduleAndServiceType.serviceType())) {
                    new ResourceFunctionFormValidator(UPDATE_RESOURCE, semanticModel, document.get()).validate(
                            function.function(), serviceDeclarationNode,
                            functionDefinitionNode, function.function().getCodedata()
                    );
                }
                return gson.toJsonTree(function);
            }
        }
        return new JsonObject();
    }

    public static NonTerminalNode findNonTerminalNode(Codedata codedata, Document document) {
        SyntaxTree syntaxTree = document.syntaxTree();
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        TextDocument textDocument = syntaxTree.textDocument();
        LineRange lineRange = codedata.getLineRange();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        return modulePartNode.findNode(TextRange.from(start, end - start), true);
    }
}
