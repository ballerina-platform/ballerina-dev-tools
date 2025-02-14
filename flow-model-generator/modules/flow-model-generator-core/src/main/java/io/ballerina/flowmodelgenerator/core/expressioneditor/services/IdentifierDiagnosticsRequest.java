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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.projects.Document;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManagerProxy;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;
import org.eclipse.lsp4j.Diagnostic;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Handles diagnostic requests for identifier validation in the expression editor.
 *
 * @see DiagnosticsRequest
 * @since 2.0.0
 */
public class IdentifierDiagnosticsRequest extends DiagnosticsRequest {

    private static final String REDECLARED_SYMBOL = "redeclared symbol '%s'";
    private static final DiagnosticErrorCode REDECLARED_SYMBOL_ERROR_CODE = DiagnosticErrorCode.REDECLARED_SYMBOL;

    public IdentifierDiagnosticsRequest(ExpressionEditorContext context,
                                        WorkspaceManagerProxy workspaceManagerProxy) {
        super(context, workspaceManagerProxy);
    }

    @Override
    protected Node getParsedNode(String expression) {
        return NodeParser.parseBindingPattern(expression);
    }

    @Override
    protected Set<Diagnostic> getSemanticDiagnostics(ExpressionEditorContext context) {
        Optional<SemanticModel> semanticModel =
                workspaceManagerProxy.get(context.fileUri()).semanticModel(context.filePath());
        Optional<Document> document = workspaceManagerProxy.get(context.fileUri()).document(context.filePath());
        if (semanticModel.isEmpty() || document.isEmpty()) {
            return Set.of();
        }
        Set<Diagnostic> diagnostics = new HashSet<>();

        // Check for redeclared symbols
        boolean redeclaredSymbol =
                semanticModel.get().visibleSymbols(document.get(), context.info().startLine()).parallelStream()
                        .filter(symbol -> symbol.kind() == SymbolKind.VARIABLE)
                        .flatMap(symbol -> symbol.getName().stream())
                        .anyMatch(name -> name.equals(context.info().expression()));
        if (redeclaredSymbol) {
            String message = String.format(REDECLARED_SYMBOL, context.info().expression());
            diagnostics.add(CommonUtils.createDiagnostic(message, context.getExpressionLineRange(),
                    REDECLARED_SYMBOL_ERROR_CODE));
        }
        return diagnostics;
    }
}
