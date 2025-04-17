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
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;
import org.eclipse.lsp4j.Diagnostic;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Handles diagnostic requests for identifier validation in the expression editor.
 *
 * @see DiagnosticsRequest
 * @since 2.0.0
 */
public class IdentifierDiagnosticsRequest extends DiagnosticsRequest {

    private static final String REDECLARED_SYMBOL = "redeclared symbol '%s'";
    private static final DiagnosticErrorCode REDECLARED_SYMBOL_ERROR_CODE = DiagnosticErrorCode.REDECLARED_SYMBOL;

    public IdentifierDiagnosticsRequest(ExpressionEditorContext context) {
        super(context);
    }

    @Override
    protected Node getParsedNode(String text) {
        return NodeParser.parseBindingPattern(text);
    }

    @Override
    protected Set<Diagnostic> getSemanticDiagnostics(ExpressionEditorContext context) {
        ExpressionEditorContext.Property property = context.getProperty();
        String typeConstraint = property.valueTypeConstraint();
        String value = property.value();

        // Skip semantic checks for object scope
        if (Property.OBJECT_SCOPE.equals(typeConstraint)) {
            return Set.of();
        }

        // Obtain the semantic model
        Optional<SemanticModel> semanticModel = context.workspaceManager().semanticModel(context.filePath());
        if (semanticModel.isEmpty()) {
            return Set.of();
        }

        // Obtain the symbol stream based on the scope of the identifier
        Stream<Symbol> symbolStream;
        if (Property.GLOBAL_SCOPE.equals(typeConstraint)) {
            symbolStream = semanticModel.get().moduleSymbols().stream()
                    .filter(symbol -> symbol.kind() != SymbolKind.MODULE);
        } else {
            Optional<Document> document = context.workspaceManager().document(context.filePath());
            if (document.isEmpty()) {
                return Set.of();
            }
            symbolStream = semanticModel.get()
                    .visibleSymbols(document.get(), context.info().startLine()).parallelStream()
                    .filter(symbol -> symbol.kind() == SymbolKind.VARIABLE);
        }

        // Check for redeclared symbols
        Set<Diagnostic> diagnostics = new HashSet<>();
        String inputValue = context.info().expression();
        boolean redeclaredSymbol =
                symbolStream.anyMatch(symbol -> symbol.nameEquals(inputValue) && !symbol.nameEquals(value));
        if (redeclaredSymbol) {
            String message = String.format(REDECLARED_SYMBOL, inputValue);
            diagnostics.add(CommonUtils.createDiagnostic(message, context.getExpressionLineRange(),
                    REDECLARED_SYMBOL_ERROR_CODE));
        }
        return diagnostics;
    }
}
