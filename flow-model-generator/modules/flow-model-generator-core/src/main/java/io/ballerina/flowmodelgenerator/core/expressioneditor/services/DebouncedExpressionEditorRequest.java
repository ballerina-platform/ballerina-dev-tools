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
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Abstract base class representing a debounced expression editor request. This class provides a template for handling
 * expression editor API requests with debouncing functionality. It manages the lifecycle of modifying and reverting
 * document content while processing expression editor requests. The class implements {@link Callable} to enable
 * asynchronous execution.
 *
 * @param <T> The type of response that will be returned by this request
 * @since 2.0.0
 */
public abstract class DebouncedExpressionEditorRequest<T> implements Callable<T> {

    private final WorkspaceManager workspaceManager;
    private final Path filePath;
    private final ExpressionEditorContext.Info contextInfo;

    public DebouncedExpressionEditorRequest(WorkspaceManager workspaceManager, Path filePath,
                                            ExpressionEditorContext.Info context) {
        this.workspaceManager = workspaceManager;
        this.filePath = filePath;
        this.contextInfo = context;
    }

    /**
     * Returns the response based on the provided expression editor context and line range. This method is implemented
     * by each expression editor API to determine how to generate the appropriate response for the current context.
     *
     * @param context   The expression editor context containing relevant information for processing
     * @param lineRange The affected line range from the template statement
     * @return The response of type T specific to the expression editor API
     */
    public abstract T getResponse(ExpressionEditorContext context, LineRange lineRange);

    /**
     * Returns the unique key associated with the expression editor API request. This key is utilized by the debouncer
     * to manage and debounce related API requests.
     *
     * @return A unique string identifier for the expression editor API
     */
    public abstract String getKey();

    /**
     * Returns the delay in milliseconds to be used for debouncing the API request.
     *
     * @return The delay in milliseconds
     */
    public abstract long getDelay();

    @Override
    public T call() throws Exception {
        // Capture the first state of the document
        Optional<Document> document = workspaceManager.document(filePath);
        if (document.isEmpty()) {
            throw new IllegalStateException("Document not found: " + filePath);
        }
        TextDocument oldTextDocument = document.get().textDocument();

        // Write the statement and generate the response
        ExpressionEditorContext context = new ExpressionEditorContext(workspaceManager,
                contextInfo, filePath, document.get());
        LineRange lineRange = context.generateStatement();
        T response = getResponse(context, lineRange);

        // Revert the document to the previous state
        context.applyContent(oldTextDocument);
        return response;
    }
}
