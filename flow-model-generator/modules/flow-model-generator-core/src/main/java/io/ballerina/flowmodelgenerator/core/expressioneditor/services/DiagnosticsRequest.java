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
import org.eclipse.lsp4j.Diagnostic;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Represents a request to retrieve diagnostics for a given file and context. This class extends
 * DebouncedExpressionEditorRequest to handle diagnostic information for Ballerina expressions with debouncing
 * capability. This is an abstract class that sets the common functionality to handle syntax and semantic errors for an
 * expression editor. It provides the basic processing workflow to parse the input expression, extract syntax errors,
 * and then, if no syntax issues are found, retrieve semantic diagnostics.
 *
 * @since 2.0.0
 */
public abstract class DiagnosticsRequest extends DebouncedExpressionEditorRequest<DiagnosticsRequest.Diagnostics> {

    public DiagnosticsRequest(ExpressionEditorContext context) {
        super(context);
    }

    public static DiagnosticsRequest from(ExpressionEditorContext context) {
        Property property = context.getProperty();
        if (property == null) {
            throw new IllegalArgumentException("Property cannot be null");
        }

        return switch (Property.ValueType.valueOf(property.valueType())) {
            case EXPRESSION -> new ExpressionDiagnosticsRequest(context);
            case IDENTIFIER -> new IdentifierDiagnosticsRequest(context);
            case TYPE -> new TypeDiagnosticRequest(context);
            default -> throw new IllegalArgumentException("Unsupported property type: " + property.valueType());
        };
    }

    /**
     * Parses the provided text and returns the corresponding ST node.
     *
     * @param text the text to be parsed
     * @return the parsed ST node
     */
    protected abstract Node getParsedNode(String text);

    /**
     * Retrieves the set of semantic diagnostics for the given text.
     *
     * @param context the expression editor context to analyze for semantic issues
     * @return a set of diagnostics representing semantic errors
     */
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
