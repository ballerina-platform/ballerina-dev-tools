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

import com.google.gson.JsonArray;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.TypesGenerator;
import io.ballerina.flowmodelgenerator.core.VisibleVariableTypesGenerator;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorCompletionRequest;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorDiagnosticsRequest;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorSignatureRequest;
import io.ballerina.flowmodelgenerator.extension.request.VisibleVariableTypeRequest;
import io.ballerina.flowmodelgenerator.extension.response.ExpressionEditorDiagnosticsResponse;
import io.ballerina.flowmodelgenerator.extension.response.ExpressionEditorTypeResponse;
import io.ballerina.flowmodelgenerator.extension.response.VisibleVariableTypesResponse;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.common.utils.PositionUtil;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("expressionEditor")
public class ExpressionEditorService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;
    private LanguageServer langServer;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
        this.langServer = langServer;
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    @JsonRequest
    public CompletableFuture<VisibleVariableTypesResponse> visibleVariableTypes(VisibleVariableTypeRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            VisibleVariableTypesResponse response = new VisibleVariableTypesResponse();
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (semanticModel.isEmpty() || document.isEmpty()) {
                    return response;
                }

                VisibleVariableTypesGenerator visibleVariableTypesGenerator = new VisibleVariableTypesGenerator(
                        semanticModel.get(), document.get(), request.position());
                JsonArray visibleVariableTypes = visibleVariableTypesGenerator.getVisibleVariableTypes();
                response.setCategories(visibleVariableTypes);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<ExpressionEditorTypeResponse> types(VisibleVariableTypeRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ExpressionEditorTypeResponse response = new ExpressionEditorTypeResponse();
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                SemanticModel semanticModel = this.workspaceManager.semanticModel(filePath).orElseGet(
                        () -> project.currentPackage().getDefaultModule().getCompilation().getSemanticModel());

                TypesGenerator typesGenerator = new TypesGenerator(semanticModel);
                response.setTypes(typesGenerator.getTypes());
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<SignatureHelp> signatureHelp(ExpressionEditorSignatureRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Path projectPath = null;
            try {
                // Load the original project
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                projectPath = this.workspaceManager.projectRoot(filePath);

                // Load the shadowed project
                ProjectCacheManager projectCacheManager =
                        ProjectCacheManager.InstanceHandler.getInstance(projectPath);
                projectCacheManager.copyContent();
                Path destination = projectCacheManager.getDestination(filePath);

                FileEvent fileEvent = new FileEvent(destination.toUri().toString(), FileChangeType.Changed);
                DidChangeWatchedFilesParams didChangeWatchedFilesParams =
                        new DidChangeWatchedFilesParams(List.of(fileEvent));
                this.langServer.getWorkspaceService().didChangeWatchedFiles(didChangeWatchedFilesParams);
                this.workspaceManager.loadProject(destination);

                // Get the document
                Optional<Document> document = this.workspaceManager.document(destination);
                if (document.isEmpty()) {
                    return new SignatureHelp();
                }
                TextDocument textDocument = document.get().textDocument();

                // Determine the cursor position
                int textPosition = textDocument.textPositionFrom(request.startLine());
                String statement = String.format("%s;%n", request.expression());
                TextEdit textEdit = TextEdit.from(TextRange.from(textPosition, 0), statement);
                TextDocument newTextDocument =
                        textDocument.apply(TextDocumentChange.from(List.of(textEdit).toArray(new TextEdit[0])));
                projectCacheManager.writeContent(newTextDocument, filePath);
                document.get().modify()
                        .withContent(String.join(System.lineSeparator(), newTextDocument.textLines()))
                        .apply();

                // Generate the signature help params
                Position position =
                        new Position(request.startLine().line(), request.startLine().offset() + request.offset());
                TextDocumentIdentifier identifier = new TextDocumentIdentifier(destination.toUri().toString());
                SignatureHelpParams params = new SignatureHelpParams(identifier, position, request.context());

                // Get the signature help
                CompletableFuture<SignatureHelp> completableFuture =
                        langServer.getTextDocumentService().signatureHelp(params);
                SignatureHelp signatureHelp = completableFuture.join();
                projectCacheManager.deleteContent();
                return signatureHelp;
            } catch (Throwable e) {
                return new SignatureHelp();
            } finally {
                if (projectPath != null) {
                    ProjectCacheManager.InstanceHandler.release(projectPath);
                }
            }
        });
    }

    @JsonRequest
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            ExpressionEditorCompletionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Path projectPath = null;
            try {
                // Load the original project
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                projectPath = this.workspaceManager.projectRoot(filePath);

                // Load the shadowed project
                ProjectCacheManager projectCacheManager =
                        ProjectCacheManager.InstanceHandler.getInstance(projectPath);
                projectCacheManager.copyContent();
                Path destination = projectCacheManager.getDestination(filePath);

                FileEvent fileEvent = new FileEvent(destination.toUri().toString(), FileChangeType.Changed);
                DidChangeWatchedFilesParams didChangeWatchedFilesParams =
                        new DidChangeWatchedFilesParams(List.of(fileEvent));
                this.langServer.getWorkspaceService().didChangeWatchedFiles(didChangeWatchedFilesParams);
                this.workspaceManager.loadProject(destination);

                // Get the document
                Optional<Document> document = this.workspaceManager.document(destination);
                if (document.isEmpty()) {
                    return Either.forLeft(List.of());
                }
                TextDocument textDocument = document.get().textDocument();

                // Determine the cursor position
                int textPosition = textDocument.textPositionFrom(request.startLine());
                String statement = String.format("_ = %s;%n", request.expression());
                TextEdit textEdit = TextEdit.from(TextRange.from(textPosition, 0), statement);
                TextDocument newTextDocument =
                        textDocument.apply(TextDocumentChange.from(List.of(textEdit).toArray(new TextEdit[0])));
                projectCacheManager.writeContent(newTextDocument, filePath);
                document.get().modify()
                        .withContent(String.join(System.lineSeparator(), newTextDocument.textLines()))
                        .apply();

                // Generate the completion params
                Position position =
                        new Position(request.startLine().line(), request.startLine().offset() + 4 + request.offset());
                TextDocumentIdentifier identifier = new TextDocumentIdentifier(destination.toUri().toString());
                CompletionParams params = new CompletionParams(identifier, position, request.context());

                // Get the completions
                CompletableFuture<Either<List<CompletionItem>, CompletionList>> completableFuture =
                        langServer.getTextDocumentService().completion(params);
                Either<List<CompletionItem>, CompletionList> completions = completableFuture.join();
                projectCacheManager.deleteContent();
                return completions;
            } catch (Throwable e) {
                return Either.forLeft(List.of());
            } finally {
                if (projectPath != null) {
                    ProjectCacheManager.InstanceHandler.release(projectPath);
                }
            }
        });
    }

    @JsonRequest
    public CompletableFuture<ExpressionEditorDiagnosticsResponse> diagnostics(
            ExpressionEditorDiagnosticsRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ExpressionEditorDiagnosticsResponse response = new ExpressionEditorDiagnosticsResponse();
            Path projectPath = null;
            try {
                // Load the original project
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                projectPath = this.workspaceManager.projectRoot(filePath);

                // Load the shadowed project
                ProjectCacheManager projectCacheManager =
                        ProjectCacheManager.InstanceHandler.getInstance(projectPath);
                projectCacheManager.copyContent();
                Path destination = projectCacheManager.getDestination(filePath);

                FileEvent fileEvent = new FileEvent(destination.toUri().toString(), FileChangeType.Changed);
                DidChangeWatchedFilesParams didChangeWatchedFilesParams =
                        new DidChangeWatchedFilesParams(List.of(fileEvent));
                this.langServer.getWorkspaceService().didChangeWatchedFiles(didChangeWatchedFilesParams);
                this.workspaceManager.loadProject(destination);

                // Get the document
                Optional<Document> document = this.workspaceManager.document(destination);
                if (document.isEmpty()) {
                    return response;
                }
                TextDocument textDocument = document.get().textDocument();

                ExpressionEditorContext context = request.context();
                // Determine the cursor position
                int textPosition = textDocument.textPositionFrom(context.startLine());

                String type = context.getProperty()
                        .flatMap(property -> Optional.ofNullable(property.valueTypeConstraint()))
                        .map(Object::toString)
                        .orElse("");
                String statement;
                if (type.isEmpty()) {
                    statement = String.format("_ = %s;%n", context.expression());
                } else {
                    statement = String.format("%s _ = %s;%n", type, context.expression());
                }
                LinePosition endLineRange = LinePosition.from(context.startLine().line(),
                        context.startLine().offset() + statement.length());
                LineRange lineRange = LineRange.from(request.filePath(), context.startLine(), endLineRange);

                TextEdit textEdit = TextEdit.from(TextRange.from(textPosition, 0), statement);
                TextDocument newTextDocument =
                        textDocument.apply(TextDocumentChange.from(List.of(textEdit).toArray(new TextEdit[0])));
                projectCacheManager.writeContent(newTextDocument, filePath);
                document.get().modify()
                        .withContent(String.join(System.lineSeparator(), newTextDocument.textLines()))
                        .apply();

                Optional<Module> module = workspaceManager.module(destination);
                if (module.isEmpty()) {
                    return response;
                }
                List<Diagnostic> diagnostics = module.get().getCompilation().diagnostics().diagnostics().stream()
                        .filter(diagnostic -> PositionUtil.isWithinLineRange(diagnostic.location().lineRange(),
                                lineRange))
                        .map(CommonUtils::transformBallerinaDiagnostic)
                        .toList();
                projectCacheManager.deleteContent();
                response.setDiagnostics(diagnostics);
            } catch (Throwable e) {
                response.setError(e);
            } finally {
                if (projectPath != null) {
                    ProjectCacheManager.InstanceHandler.release(projectPath);
                }
            }
            return response;
        });
    }
}
