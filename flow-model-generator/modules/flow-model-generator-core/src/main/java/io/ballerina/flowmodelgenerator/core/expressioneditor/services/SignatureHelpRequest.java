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

package io.ballerina.flowmodelgenerator.core.expressioneditor.services;

import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpContext;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a request for signature help in the expression editor.
 * This class extends DebouncedExpressionEditorRequest to handle signature help requests with debouncing functionality.
 *
 * @since 2.0.0
 */
public class SignatureHelpRequest extends DebouncedExpressionEditorRequest<SignatureHelp> {

    private final String fileUri;
    private final SignatureHelpContext signatureHelpContext;
    private final TextDocumentService textDocumentService;

    public SignatureHelpRequest(WorkspaceManager workspaceManager,
                                Path filePath,
                                ExpressionEditorContext.Info context,
                                String fileUri,
                                SignatureHelpContext signatureHelpContext,
                                TextDocumentService textDocumentService) {
        super(workspaceManager, filePath, context);
        this.fileUri = fileUri;
        this.signatureHelpContext = signatureHelpContext;
        this.textDocumentService = textDocumentService;
    }

    @Override
    public SignatureHelp getResponse(ExpressionEditorContext context, LineRange lineRange) {
        Position position = context.getCursorPosition();
        TextDocumentIdentifier identifier = new TextDocumentIdentifier(fileUri);
        SignatureHelpParams params = new SignatureHelpParams(identifier, position, signatureHelpContext);
        CompletableFuture<SignatureHelp> future = textDocumentService.signatureHelp(params);
        return future.join();
    }

    @Override
    public String getKey() {
        return "signatureHelp";
    }

    @Override
    public long getDelay() {
        return 100;
    }
}
