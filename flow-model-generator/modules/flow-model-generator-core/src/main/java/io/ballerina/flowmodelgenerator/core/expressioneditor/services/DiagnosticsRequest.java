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

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManagerProxy;
import org.eclipse.lsp4j.Diagnostic;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Represents a request to retrieve diagnostics for a given file and context. This class extends
 * DebouncedExpressionEditorRequest to handle diagnostic information for Ballerina expressions with debouncing
 * capability.
 *
 * @since 2.0.0
 */
public abstract class DiagnosticsRequest extends DebouncedExpressionEditorRequest<DiagnosticsRequest.Diagnostics> {

    protected final String fileUri;
    protected final WorkspaceManagerProxy workspaceManagerProxy;

    public DiagnosticsRequest(ExpressionEditorContext context, String fileUri,
                              WorkspaceManagerProxy workspaceManagerProxy) {
        super(context);
        this.fileUri = fileUri;
        this.workspaceManagerProxy = workspaceManagerProxy;
    }

    public static DiagnosticsRequest from(ExpressionEditorContext context,
                                          String fileUri,
                                          WorkspaceManagerProxy workspaceManagerProxy) {
        Property property = context.getProperty();
        if (property == null) {
            throw new IllegalArgumentException("Property cannot be null");
        }

        return switch (Property.ValueType.valueOf(property.valueType())) {
            case EXPRESSION -> new ExpressionDiagnosticsRequest(context, fileUri, workspaceManagerProxy);
            case IDENTIFIER -> new IdentifierDiagnosticsRequest(context, fileUri, workspaceManagerProxy);
            case TYPE -> new TypeDiagnosticRequest(context, fileUri, workspaceManagerProxy);
            default -> throw new IllegalArgumentException("Unsupported property type: " + property.valueType());
        };
    }

    protected abstract Node getParsedNode(String expression);

    protected abstract Set<Diagnostic> getSemanticDiagnostics(ExpressionEditorContext context);

    private Set<Diagnostic> getSyntaxDiagnostics(ExpressionEditorContext context) {
        Node parsedNode = getParsedNode(context.info().expression());
        return StreamSupport.stream(parsedNode.diagnostics().spliterator(), true)
                .map(CommonUtils::transformBallerinaDiagnostic)
                .collect(Collectors.toSet());
    }

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
