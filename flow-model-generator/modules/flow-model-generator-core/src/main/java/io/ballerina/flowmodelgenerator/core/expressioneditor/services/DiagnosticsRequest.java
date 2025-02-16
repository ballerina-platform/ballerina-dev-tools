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
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.PositionUtil;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManagerProxy;
import org.eclipse.lsp4j.Diagnostic;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents a request to retrieve diagnostics for a given file and context.
 * This class extends DebouncedExpressionEditorRequest to handle diagnostic information
 * for Ballerina expressions with debouncing capability.
 * 
 */
public class DiagnosticsRequest extends DebouncedExpressionEditorRequest<DiagnosticsRequest.Diagnostics> {

    private final String fileUri;
    private final WorkspaceManagerProxy workspaceManagerProxy;
    private final Path filePath;

    public DiagnosticsRequest(WorkspaceManager workspaceManager,
                              Path filePath,
                              ExpressionEditorContext.Info context,
                              String fileUri,
                              WorkspaceManagerProxy workspaceManagerProxy) {
        super(workspaceManager, filePath, context);
        this.fileUri = fileUri;
        this.workspaceManagerProxy = workspaceManagerProxy;
        this.filePath = filePath;
    }

    @Override
    public Diagnostics getResponse(ExpressionEditorContext context, LineRange lineRange) {
        // Get the semantic model for the file
        Optional<SemanticModel> semanticModelOpt = workspaceManagerProxy.get(fileUri).semanticModel(filePath);
        if (semanticModelOpt.isEmpty()) {
            return new Diagnostics(Set.of());
        }
        SemanticModel semanticModel = semanticModelOpt.get();

        // Get diagnostics from the semantic model and syntax tree
        Set<Diagnostic> diagnostics = Stream.concat(
                semanticModel.diagnostics().stream(),
                StreamSupport.stream(context.syntaxDiagnostics().spliterator(), false))
                .filter(diagnostic -> PositionUtil.isWithinLineRange(diagnostic.location().lineRange(), lineRange))
                .map(CommonUtils::transformBallerinaDiagnostic)
                .collect(Collectors.toSet());
        return new Diagnostics(diagnostics);
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
