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
import io.ballerina.flowmodelgenerator.core.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.core.TypesGenerator;
import io.ballerina.flowmodelgenerator.core.VisibleVariableTypesGenerator;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorCompletionRequest;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorDiagnosticsRequest;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorSignatureRequest;
import io.ballerina.flowmodelgenerator.extension.request.VisibleVariableTypeRequest;
import io.ballerina.flowmodelgenerator.extension.response.ExpressionEditorDiagnosticsResponse;
import io.ballerina.flowmodelgenerator.extension.response.ExpressionEditorTypeResponse;
import io.ballerina.flowmodelgenerator.extension.response.VisibleVariableTypesResponse;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.common.utils.PositionUtil;
import org.ballerinalang.langserver.commons.LanguageServerContext;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManagerProxy;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("expressionEditor")
public class ExpressionEditorService implements ExtendedLanguageServerService {

    private WorkspaceManagerProxy workspaceManagerProxy;
    private LanguageServer langServer;

    @Override
    public void init(LanguageServer langServer, WorkspaceManagerProxy workspaceManagerProxy,
                     LanguageServerContext serverContext) {
        this.workspaceManagerProxy = workspaceManagerProxy;
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
                this.workspaceManagerProxy.get().loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManagerProxy.get().semanticModel(filePath);
                Optional<Document> document = this.workspaceManagerProxy.get().document(filePath);
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
                Project project = this.workspaceManagerProxy.get().loadProject(filePath);
                SemanticModel semanticModel = this.workspaceManagerProxy.get().semanticModel(filePath).orElseGet(
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
            try {
                // Load the original project
                Path filePath = Path.of(request.filePath());
                String fileUri = URI.create("expr://" + filePath).toString();

                // Get the document
                Optional<Document> document = workspaceManagerProxy.get(fileUri).document(filePath);
                if (document.isEmpty()) {
                    return new SignatureHelp();
                }
                TextDocument oldTextDocument = document.get().textDocument();

                // Generate signature help using context
                ExpressionEditorContext context = new ExpressionEditorContext(workspaceManagerProxy.get(fileUri),
                        request.context(), filePath, document.get());
                context.generateStatement();

                // Generate the signature help params
                Position position = context.getCursorPosition();
                TextDocumentIdentifier identifier = new TextDocumentIdentifier(fileUri);
                SignatureHelpParams params =
                        new SignatureHelpParams(identifier, position, request.signatureHelpContext());

                // Get signature help from language server
                CompletableFuture<SignatureHelp> completableFuture =
                        langServer.getTextDocumentService().signatureHelp(params);
                SignatureHelp signatureHelp = completableFuture.join();

                // Restore original content
                context.applyContent(oldTextDocument);

                return signatureHelp;
            } catch (Throwable e) {
                return new SignatureHelp();
            }
        });
    }

    @JsonRequest
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            ExpressionEditorCompletionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Load the original project
                Path filePath = Path.of(request.filePath());
                String fileUri = URI.create("expr://" + filePath).toString();

                // Get the document
                Optional<Document> document = workspaceManagerProxy.get(fileUri).document(filePath);
                if (document.isEmpty()) {
                    return Either.forLeft(List.of());
                }
                TextDocument oldTextDocument = document.get().textDocument();

                // Generate completions using context
                ExpressionEditorContext context = new ExpressionEditorContext(workspaceManagerProxy.get(fileUri),
                        request.context(), filePath, document.get());
                context.generateStatement();

                // Generate the completion params
                Position position = context.getCursorPosition();
                TextDocumentIdentifier identifier = new TextDocumentIdentifier(fileUri);
                CompletionParams params = new CompletionParams(identifier, position, request.completionContext());

                // Get completions from language server
                CompletableFuture<Either<List<CompletionItem>, CompletionList>> completableFuture =
                        langServer.getTextDocumentService().completion(params);
                Either<List<CompletionItem>, CompletionList> completions = completableFuture.join();

                // Restore original content
                context.applyContent(oldTextDocument);

                return completions;
            } catch (Throwable e) {
                return Either.forLeft(List.of());
            }
        });
    }

    @JsonRequest
    public CompletableFuture<ExpressionEditorDiagnosticsResponse> diagnostics(
            ExpressionEditorDiagnosticsRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ExpressionEditorDiagnosticsResponse response = new ExpressionEditorDiagnosticsResponse();
            try {
                // Load the original project
                Path filePath = Path.of(request.filePath());
                String fileUri = URI.create("expr://" + filePath).toString();

                // Get the document
                Optional<Document> document = workspaceManagerProxy.get(fileUri).document(filePath);
                if (document.isEmpty()) {
                    return response;
                }
                TextDocument oldTextDocument = document.get().textDocument();

                // Generate the diagnostics
                ExpressionEditorContext context = new ExpressionEditorContext(workspaceManagerProxy.get(fileUri),
                        request.context(), filePath, document.get());
                LineRange lineRange = context.generateStatement();
                Optional<Module> module = workspaceManagerProxy.get(fileUri).module(filePath);
                if (module.isEmpty()) {
                    return response;
                }
                List<Diagnostic> diagnostics = module.get().getCompilation().diagnostics().diagnostics().stream()
                        .filter(diagnostic -> PositionUtil.isWithinLineRange(diagnostic.location().lineRange(),
                                lineRange))
                        .map(CommonUtils::transformBallerinaDiagnostic)
                        .toList();
                context.applyContent(oldTextDocument);
                response.setDiagnostics(diagnostics);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

}
