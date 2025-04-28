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
import io.ballerina.compiler.syntax.tree.BracedExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;
import org.eclipse.lsp4j.Diagnostic;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Handles diagnostic requests for lv expressions validation in the expression editor.
 *
 * @see DiagnosticsRequest
 * @since 2.0.0
 */
public class LvExpressionDiagnosticRequest extends DiagnosticsRequest {

    private static final String INVALID_LHS = "Invalid expression for a variable reference '%s'";
    private static final DiagnosticErrorCode INVALID_EXPRESSION_CODE = DiagnosticErrorCode.INVALID_EXPR_STATEMENT;
    private static final DiagnosticErrorCode UNDERSCORE_NOT_ALLOWED_CODE =
            DiagnosticErrorCode.UNDERSCORE_NOT_ALLOWED_AS_IDENTIFIER;

    public LvExpressionDiagnosticRequest(ExpressionEditorContext context) {
        super(context);
    }

    @Override
    protected Node getParsedNode(String text) {
        return NodeParser.parseExpression(text);
    }

    @Override
    protected Set<Diagnostic> getSyntaxDiagnostics(ExpressionEditorContext context) {
        Node parsedNode = getParsedNode(context.info().expression());
        if (parsedNode.hasDiagnostics()) {
            return StreamSupport.stream(parsedNode.diagnostics().spliterator(), true)
                    .map(CommonUtils::transformBallerinaDiagnostic)
                    .collect(Collectors.toSet());
        }
        if (isValidLVExpr((ExpressionNode) parsedNode)) {
            return Set.of();
        } else {
            String message = String.format(INVALID_LHS, context.info().expression());
            return Set.of(
                    CommonUtils.createDiagnostic(message, context.getExpressionLineRange(), INVALID_EXPRESSION_CODE));
        }
    }

    private boolean isValidLVExpr(ExpressionNode expression) {
        return switch (expression.kind()) {
            case SIMPLE_NAME_REFERENCE,
                 QUALIFIED_NAME_REFERENCE,
                 LIST_BINDING_PATTERN,
                 MAPPING_BINDING_PATTERN,
                 ERROR_BINDING_PATTERN,
                 WILDCARD_BINDING_PATTERN -> true;
            case FIELD_ACCESS -> isValidLVMemberExpr(((FieldAccessExpressionNode) expression).expression());
            case INDEXED_EXPRESSION -> isValidLVMemberExpr(((IndexedExpressionNode) expression).containerExpression());
            default -> expression.isMissing();
        };
    }

    private boolean isValidLVMemberExpr(ExpressionNode expression) {
        return switch (expression.kind()) {
            case SIMPLE_NAME_REFERENCE,
                 QUALIFIED_NAME_REFERENCE -> true;
            case FIELD_ACCESS -> isValidLVMemberExpr(((FieldAccessExpressionNode) expression).expression());
            case INDEXED_EXPRESSION -> isValidLVMemberExpr(((IndexedExpressionNode) expression).containerExpression());
            case BRACED_EXPRESSION -> isValidLVMemberExpr(((BracedExpressionNode) expression).expression());
            default -> expression.isMissing();
        };
    }

    @Override
    protected Set<Diagnostic> getSemanticDiagnostics(ExpressionEditorContext context) {
        LineRange lineRange = context.generateStatement();
        Optional<SemanticModel> semanticModel =
                context.workspaceManager().semanticModel(context.filePath());
        return semanticModel.map(model -> model.diagnostics(lineRange).stream()
                .filter(diagnostic -> diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR
                        && !UNDERSCORE_NOT_ALLOWED_CODE.diagnosticId().equals(diagnostic.diagnosticInfo().code()))
                .map(CommonUtils::transformBallerinaDiagnostic)
                .collect(Collectors.toSet())).orElseGet(Set::of);
    }
}
