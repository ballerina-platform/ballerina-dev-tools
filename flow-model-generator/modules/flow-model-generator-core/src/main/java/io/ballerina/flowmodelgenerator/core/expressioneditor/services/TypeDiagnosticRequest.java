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
import io.ballerina.compiler.api.Types;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
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

    public TypeDiagnosticRequest(ExpressionEditorContext context) {
        super(context);
    }

    @Override
    protected Node getParsedNode(String text) {
        return NodeParser.parseTypeDescriptor(text);
    }

    @Override
    protected Set<Diagnostic> getSemanticDiagnostics(ExpressionEditorContext context) {
        Optional<SemanticModel> semanticModel =
                context.workspaceManager().semanticModel(context.filePath());
        Optional<Document> document = context.workspaceManager().document(context.filePath());
        if (semanticModel.isEmpty() || document.isEmpty()) {
            return Set.of();
        }
        Set<Diagnostic> diagnostics = new HashSet<>();

        // Check for undefined types
        Types types = semanticModel.get().types();
        Optional<TypeSymbol> typeSymbol = types.getType(document.get(), context.info().expression());
        if (typeSymbol.isEmpty()) {
            String message = String.format(UNDEFINED_TYPE, context.info().expression());
            diagnostics.add(CommonUtils.createDiagnostic(message, context.getExpressionLineRange(),
                    UNKNOWN_TYPE_ERROR_CODE));
            return diagnostics;
        }

        // Check if the type is a subtype of the type constraint
        String typeConstraint = context.getProperty().valueTypeConstraint();
        if (typeConstraint == null) {
            return diagnostics;
        }
        Optional<TypeSymbol> typeConstraintTypeSymbol = types.getType(document.get(), typeConstraint);
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
