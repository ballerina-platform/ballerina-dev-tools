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
import io.ballerina.flowmodelgenerator.core.expressioneditor.DocumentContext;
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.core.expressioneditor.services.CompletionRequest;
import io.ballerina.flowmodelgenerator.core.expressioneditor.services.DiagnosticsRequest;
import io.ballerina.flowmodelgenerator.core.expressioneditor.services.SignatureHelpRequest;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorCompletionRequest;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorDiagnosticsRequest;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorSignatureRequest;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorTypesRequest;
import io.ballerina.flowmodelgenerator.extension.request.FunctionCallTemplateRequest;
import io.ballerina.flowmodelgenerator.extension.request.ImportModuleRequest;
import io.ballerina.flowmodelgenerator.extension.request.VisibleVariableTypeRequest;
import io.ballerina.flowmodelgenerator.extension.response.FunctionCallTemplateResponse;
import io.ballerina.flowmodelgenerator.extension.response.ImportModuleResponse;
import io.ballerina.flowmodelgenerator.extension.response.VisibleVariableTypesResponse;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleDependency;
import io.ballerina.projects.ModuleDescriptor;
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
                DocumentContext documentContext = new DocumentContext(workspaceManagerProxy, filePath);
                Optional<SemanticModel> semanticModel = documentContext.semanticModel();
                Document document = documentContext.document();
                if (semanticModel.isEmpty()) {
                    return response;
                }

                VisibleVariableTypesGenerator visibleVariableTypesGenerator = new VisibleVariableTypesGenerator(
                        semanticModel.get(), document, CommonUtils.getPosition(request.position(), document));
                JsonArray visibleVariableTypes = visibleVariableTypesGenerator.getVisibleVariableTypes();
                response.setCategories(visibleVariableTypes);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> types(ExpressionEditorTypesRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                DocumentContext documentContext = new DocumentContext(workspaceManagerProxy, filePath);
                return TypesGenerator.getInstance()
                        .getTypes(documentContext, request.typeConstraint(), request.position());
            } catch (Throwable e) {
                return Either.forRight(new CompletionList());
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SignatureHelp> signatureHelp(ExpressionEditorSignatureRequest request) {
        String fileUri = CommonUtils.getExprUri(request.filePath());
        return Debouncer.getInstance().debounce(new SignatureHelpRequest(
                new ExpressionEditorContext(
                        workspaceManagerProxy,
                        fileUri,
                        request.context(),
                        Path.of(request.filePath())
                ),
                request.signatureHelpContext(),
                langServer.getTextDocumentService()));
    }

    @JsonRequest
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            ExpressionEditorCompletionRequest request) {
        String fileUri = CommonUtils.getExprUri(request.filePath());
        return Debouncer.getInstance().debounce(new CompletionRequest(
                new ExpressionEditorContext(
                        workspaceManagerProxy,
                        fileUri,
                        request.context(),
                        Path.of(request.filePath())
                ),
                request.completionContext(),
                langServer.getTextDocumentService()));
    }

    @JsonRequest
    public CompletableFuture<DiagnosticsRequest.Diagnostics> diagnostics(ExpressionEditorDiagnosticsRequest request) {
        String fileUri = CommonUtils.getExprUri(request.filePath());
        return Debouncer.getInstance().debounce(DiagnosticsRequest.from(
                new ExpressionEditorContext(
                        workspaceManagerProxy,
                        fileUri,
                        request.context(),
                        Path.of(request.filePath())
                )));
    }

    @JsonRequest
    public CompletableFuture<FunctionCallTemplateResponse> functionCallTemplate(FunctionCallTemplateRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            FunctionCallTemplateResponse response = new FunctionCallTemplateResponse();
            try {
                Codedata codedata = request.codedata();
                String template;
                switch (request.kind()) {
                    case CURRENT -> template = codedata.symbol();
                    case IMPORTED -> template = codedata.getModulePrefix() + ":" + codedata.symbol();
                    case AVAILABLE -> {
                        String fileUri = CommonUtils.getExprUri(request.filePath());
                        String importStatement = codedata.getImportSignature();
                        ExpressionEditorContext expressionEditorContext = new ExpressionEditorContext(
                                workspaceManagerProxy,
                                fileUri,
                                Path.of(request.filePath()),
                                null);
                        Optional<TextEdit> importTextEdit = expressionEditorContext.getImport(importStatement);
                        importTextEdit.ifPresent(
                                textEdit -> expressionEditorContext.applyTextEdits(List.of(textEdit)));
                        template = codedata.getModulePrefix() + ":" + codedata.symbol();
                    }
                    default -> {
                        response.setError(new IllegalArgumentException("Invalid kind: " + request.kind() +
                                ". Expected kinds are: CURRENT, IMPORTED, AVAILABLE."));
                        return response;
                    }
                }
                // TODO: Fix this after revamping the API
                if (request.searchKind() != null && request.searchKind().equals("TYPE")) {
                    response.setTemplate(template);
                } else {
                    response.setTemplate(template + "(${1})");
                }

            } catch (Exception e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<ImportModuleResponse> importModule(ImportModuleRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ImportModuleResponse response = new ImportModuleResponse();
            try {
                String fileUri = CommonUtils.getExprUri(request.filePath());
                Path filePath = Path.of(request.filePath());
                ExpressionEditorContext expressionEditorContext = new ExpressionEditorContext(
                        workspaceManagerProxy,
                        fileUri,
                        filePath,
                        null);
                String importStatement = request.importStatement()
                        .replaceFirst("^import\\s+", "")
                        .replaceAll(";\\n$", "");
                Optional<TextEdit> importTextEdit = expressionEditorContext.getImport(importStatement);
                importTextEdit.ifPresent(textEdit -> expressionEditorContext.applyTextEdits(List.of(textEdit)));

                // Obtain the module details
                String[] split = importStatement.split("/");
                Module module = expressionEditorContext.documentContext().module().orElseThrow();
                module.packageInstance().getCompilation();
                module.packageInstance().getResolution();
                ModuleDescriptor descriptor = module.moduleDependencies().stream()
                        .map(ModuleDependency::descriptor)
                        .filter(moduleDependencyDescriptor ->
                                moduleDependencyDescriptor.org().value().equals(split[0]) &&
                                        moduleDependencyDescriptor.packageName().value().equals(split[1]))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Module not found for: " + importStatement));
                response.setPrefix(CommonUtils.getDefaultModulePrefix(descriptor.packageName().value()));
                response.setModuleId(CommonUtils.constructModuleId(descriptor));
            } catch (Exception e) {
                response.setError(e);
            }
            return response;
        });
    }
}
