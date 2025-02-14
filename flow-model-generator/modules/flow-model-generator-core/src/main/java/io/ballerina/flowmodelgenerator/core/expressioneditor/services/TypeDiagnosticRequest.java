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
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.flowmodelgenerator.core.TypesGenerator;
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManagerProxy;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;
import org.eclipse.lsp4j.Diagnostic;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Handles diagnostic requests for type descriptor validation in the expression editor.
 *
 * @see DiagnosticsRequest
 * @since 2.0.0
 */
public class TypeDiagnosticRequest extends DiagnosticsRequest {

    private static final String UNDEFINED_TYPE = "undefined type '%s'";
    private static final String INVALID_SUBTYPE = "expected a subtype of '%s', but found '%s'";
    private static final DiagnosticErrorCode UNKNOWN_TYPE_ERROR_CODE = DiagnosticErrorCode.UNKNOWN_TYPE;

    public TypeDiagnosticRequest(ExpressionEditorContext context, String fileUri,
                                 WorkspaceManagerProxy workspaceManagerProxy) {
        super(context, fileUri, workspaceManagerProxy);
    }

    @Override
    protected Node getParsedNode(String expression) {
        return NodeParser.parseTypeDescriptor(expression);
    }

    @Override
    protected Set<Diagnostic> getSemanticDiagnostics(ExpressionEditorContext context) {
        Optional<SemanticModel> semanticModel = workspaceManagerProxy.get(fileUri).semanticModel(context.filePath());
        Optional<Document> document = workspaceManagerProxy.get(fileUri).document(context.filePath());
        if (semanticModel.isEmpty() || document.isEmpty()) {
            return Set.of();
        }
        Set<Diagnostic> diagnostics = new HashSet<>();
        TypesGenerator typesGenerator = TypesGenerator.getInstance();

        // Get the builtin type symbol
        Optional<TypeSymbol> typeSymbol =
                typesGenerator.getTypeSymbol(semanticModel.get(), context.info().expression());

        // Get the type definition symbol if it is not a builtin type
        if (typeSymbol.isEmpty()) {
            typeSymbol =
                    semanticModel.get().visibleSymbols(document.get(), context.info().startLine()).parallelStream()
                            .filter(symbol -> symbol.kind() == SymbolKind.TYPE_DEFINITION &&
                                    symbol.nameEquals(context.info().expression()))
                            .map(symbol -> ((TypeDefinitionSymbol) symbol).typeDescriptor())
                            .findFirst();
        }

        // Check for undefined types
        if (typeSymbol.isEmpty()) {
            String message = String.format(UNDEFINED_TYPE, context.info().expression());
            diagnostics.add(CommonUtils.createDiagnostic(message, context.getExpressionLineRange(),
                    UNKNOWN_TYPE_ERROR_CODE));
            return diagnostics;
        }

        // Check if the type is a subtype of the type constraint
        String typeConstraint = context.getProperty().valueTypeConstraint().toString();
        Optional<TypeSymbol> typeConstraintTypeSymbol =
                typesGenerator.getTypeSymbol(semanticModel.get(), typeConstraint);
        if (typeConstraintTypeSymbol.isPresent()) {
            if (!typeSymbol.get().subtypeOf(typeConstraintTypeSymbol.get())) {
                String message = String.format(INVALID_SUBTYPE, typeConstraint, context.info().expression());
                diagnostics.add(CommonUtils.createDiagnostic(message, context.getExpressionLineRange(),
                        "", DiagnosticSeverity.ERROR));
            }
        }
        return diagnostics;
    }
}
