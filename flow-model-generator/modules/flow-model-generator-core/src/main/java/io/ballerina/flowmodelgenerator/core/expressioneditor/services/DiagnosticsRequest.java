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
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManagerProxy;
import org.eclipse.lsp4j.Diagnostic;

import java.nio.file.Path;
import java.util.Set;

/**
 * Represents a request to retrieve diagnostics for a given file and context.
 * This class extends DebouncedExpressionEditorRequest to handle diagnostic information
 * for Ballerina expressions with debouncing capability.
 * 
 */
public abstract class DiagnosticsRequest extends DebouncedExpressionEditorRequest<DiagnosticsRequest.Diagnostics> {

    public DiagnosticsRequest(WorkspaceManager workspaceManager,
                              Path filePath,
                              ExpressionEditorContext.Info context) {
        super(workspaceManager, filePath, context);
    }

    public static DiagnosticsRequest from(WorkspaceManager workspaceManager,
                                          Path filePath,
                                          ExpressionEditorContext.Info context,
                                          String fileUri,
                                          WorkspaceManagerProxy workspaceManagerProxy) {
        return new ExpressionDiagnosticsRequest(workspaceManager, filePath, context, fileUri, workspaceManagerProxy);
    }

    protected abstract Set<Diagnostic> getSyntaxDiagnostics(ExpressionEditorContext context);

    protected abstract Set<Diagnostic> getSemanticDiagnostics(ExpressionEditorContext context);

    @Override
    public Diagnostics getResponse(ExpressionEditorContext context) {
        // Check for syntax errors
        Set<Diagnostic> syntaxDiagnostics = getSyntaxDiagnostics(context);
        if (!syntaxDiagnostics.isEmpty()) {
            return new Diagnostics(syntaxDiagnostics);
        }

        // Check for semantic errors
        return new Diagnostics(getSemanticDiagnostics(context));
    }

    @Override
    public String getKey() {
        return "diagnostics";
    }

    @Override
    public long getDelay() {
        return 350;
    }

    public record Diagnostics(Set<Diagnostic> diagnostics) {
    }
}
