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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorCompletionRequest;
import io.ballerina.flowmodelgenerator.extension.request.VisibleVariableTypeRequest;
import io.ballerina.flowmodelgenerator.extension.response.VisibleVariableTypesResponse;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
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
                List<Type> list = semanticModel.get().visibleSymbols(document.get(), request.position()).stream()
                        .filter(symbol -> symbol.kind() == SymbolKind.VARIABLE)
                        .map(Type::fromSemanticSymbol).toList();
                response.setTypes(list);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            ExpressionEditorCompletionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Load the project
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Path projectPath = this.workspaceManager.projectRoot(filePath);

                // Create a temporary directory and load the project
                ProjectCacheManager projectCacheManager = new ProjectCacheManager(projectPath, filePath);
                projectCacheManager.createTempDirectory();
                Path destination = projectCacheManager.getDestination();
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
                projectCacheManager.writeContent(newTextDocument);
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
                projectCacheManager.deleteCache();
                return completions;
            } catch (Throwable e) {
                return Either.forLeft(List.of());
            }
        });
    }
}
