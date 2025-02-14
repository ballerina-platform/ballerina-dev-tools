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
import io.ballerina.flowmodelgenerator.core.TypesGenerator;
import io.ballerina.flowmodelgenerator.core.VisibleVariableTypesGenerator;
import io.ballerina.flowmodelgenerator.core.expressioneditor.Debouncer;
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.core.expressioneditor.services.CompletionRequest;
import io.ballerina.flowmodelgenerator.core.expressioneditor.services.DiagnosticsRequest;
import io.ballerina.flowmodelgenerator.core.expressioneditor.services.SignatureHelpRequest;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorCompletionRequest;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorDiagnosticsRequest;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorSignatureRequest;
import io.ballerina.flowmodelgenerator.extension.request.FunctionCallTemplateRequest;
import io.ballerina.flowmodelgenerator.extension.request.ImportModuleRequest;
import io.ballerina.flowmodelgenerator.extension.request.VisibleVariableTypeRequest;
import io.ballerina.flowmodelgenerator.extension.response.ExpressionEditorTypeResponse;
import io.ballerina.flowmodelgenerator.extension.response.FunctionCallTemplateResponse;
import io.ballerina.flowmodelgenerator.extension.response.SuccessResponse;
import io.ballerina.flowmodelgenerator.extension.response.VisibleVariableTypesResponse;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.TextEdit;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.LanguageServerContext;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManagerProxy;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.SignatureHelp;
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
                response.setTypes(TypesGenerator.getInstance().getTypes(semanticModel));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<SignatureHelp> signatureHelp(ExpressionEditorSignatureRequest request) {
        String fileUri = CommonUtils.getExprUri(request.filePath());
        return Debouncer.getInstance().debounce(new SignatureHelpRequest(
                new ExpressionEditorContext(
                        workspaceManagerProxy.get(fileUri),
                        request.context(),
                        Path.of(request.filePath())
                ),
                fileUri,
                request.signatureHelpContext(),
                langServer.getTextDocumentService()));
    }

    @JsonRequest
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            ExpressionEditorCompletionRequest request) {
        String fileUri = CommonUtils.getExprUri(request.filePath());
        return Debouncer.getInstance().debounce(new CompletionRequest(
                new ExpressionEditorContext(
                        workspaceManagerProxy.get(fileUri),
                        request.context(),
                        Path.of(request.filePath())
                ),
                fileUri,
                request.completionContext(),
                langServer.getTextDocumentService()));
    }

    @JsonRequest
    public CompletableFuture<DiagnosticsRequest.Diagnostics> diagnostics(
            ExpressionEditorDiagnosticsRequest request) {
        String fileUri = CommonUtils.getExprUri(request.filePath());
        return Debouncer.getInstance().debounce(DiagnosticsRequest.from(
                new ExpressionEditorContext(
                        workspaceManagerProxy.get(fileUri),
                        request.context(),
                        Path.of(request.filePath())
                ),
                fileUri,
                workspaceManagerProxy
        ));
    }

    @JsonRequest
    public CompletableFuture<FunctionCallTemplateResponse> functionCallTemplate(FunctionCallTemplateRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            FunctionCallTemplateResponse response = new FunctionCallTemplateResponse();
            try {
                Codedata codedata = request.codedata();
                String template;
                switch (request.kind()) {
                    case CURRENT:
                        template = codedata.symbol();
                        break;
                    case IMPORTED:
                        template = codedata.getModulePrefix() + ":" + codedata.symbol();
                        break;
                    case AVAILABLE:
                        String fileUri = CommonUtils.getExprUri(request.filePath());
                        Optional<Document> document =
                                workspaceManagerProxy.get(fileUri).document(Path.of(request.filePath()));

                        if (document.isPresent()) {
                            String importStatement = codedata.getImportSignature();
                            Document doc = document.get();
                            ExpressionEditorContext expressionEditorContext = new ExpressionEditorContext(
                                    workspaceManagerProxy.get(fileUri), Path.of(request.filePath()), doc);
                            Optional<TextEdit> importTextEdit = expressionEditorContext.getImport(importStatement);
                            importTextEdit.ifPresent(
                                    textEdit -> expressionEditorContext.applyTextEdits(List.of(textEdit)));
                        }
                        template = codedata.getModulePrefix() + ":" + codedata.symbol();
                        break;
                    default:
                        response.setError(new IllegalArgumentException("Invalid kind: " + request.kind() +
                                ". Expected kinds are: CURRENT, IMPORTED, AVAILABLE."));
                        return response;
                }
                response.setTemplate(template + "(${1})");
            } catch (Exception e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<SuccessResponse> importModule(ImportModuleRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            SuccessResponse response = new SuccessResponse();
            try {
                String fileUri = CommonUtils.getExprUri(request.filePath());
                Optional<Document> document = workspaceManagerProxy.get(fileUri).document(Path.of(request.filePath()));
                if (document.isPresent()) {
                    ExpressionEditorContext expressionEditorContext = new ExpressionEditorContext(
                            workspaceManagerProxy.get(fileUri),
                            Path.of(request.filePath()), document.get());
                    String importStatement = request.importStatement()
                            .replaceFirst("^import\\s+", "")
                            .replaceAll(";\\n$", "");
                    Optional<TextEdit> importTextEdit = expressionEditorContext
                            .getImport(importStatement);
                    importTextEdit.ifPresent(textEdit -> expressionEditorContext.applyTextEdits(List.of(textEdit)));
                    response.setSuccess(true);
                }
            } catch (Exception e) {
                response.setError(e);
                response.setSuccess(false);
            }
            return response;
        });
    }
}
