/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ChildNodeList;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates error handlers for function definitions that does not contain a global error handler.
 *
 * @since 2.0.0
 */
public class ErrorHandlerGenerator {

    private final WorkspaceManager workspaceManager;
    private final Path filePath;
    private final Gson gson;

    public ErrorHandlerGenerator(WorkspaceManager workspaceManager, Path filePath) {
        this.workspaceManager = workspaceManager;
        this.filePath = filePath;
        this.gson = new Gson();
    }

    public JsonElement getTextEdits() {
        ModulePartNode rootNode;
        SemanticModel semanticModel;
        try {
            workspaceManager.loadProject(filePath);
            Document document = workspaceManager.document(filePath).orElseThrow();
            semanticModel = workspaceManager.semanticModel(filePath).orElseThrow();
            rootNode = document.syntaxTree().rootNode();
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException("Failed to get the document", e);
        }
        FunctionVisitor functionVisitor = new FunctionVisitor(semanticModel);
        rootNode.accept(functionVisitor);
        List<TextEdit> textEdits = functionVisitor.getTextEdits();
        return gson.toJsonTree(Map.of(filePath, textEdits));
    }

    private static class FunctionVisitor extends NodeVisitor {

        private static final String prefix = "do {\n";
        private static final String suffix = "\n} on fail var e {\n   return e;\n}";
        private final List<TextEdit> textEdits;
        private final SemanticModel semanticModel;
        private final TypeSymbol errorTypeSymbol;

        public FunctionVisitor(SemanticModel semanticModel) {
            this.textEdits = new ArrayList<>();
            this.semanticModel = semanticModel;
            this.errorTypeSymbol = semanticModel.types().ERROR;
        }

        @Override
        public void visit(FunctionDefinitionNode functionDefinitionNode) {
            // Check if the function already contains a global error handler
            FunctionBodyNode functionBodyNode = functionDefinitionNode.functionBody();

            // Ignore if the function body is an expression bodied function
            if (functionBodyNode.kind() == SyntaxKind.EXPRESSION_FUNCTION_BODY) {
                return;
            }

            ChildNodeList children = functionBodyNode.children();
            if (children.size() == 3 && children.get(1).kind() == SyntaxKind.DO_STATEMENT) {
                return;
            }

            // Append error type to the signature if not exists
            if (hasNoReturnError(functionDefinitionNode)) {
                FunctionSignatureNode functionSignatureNode = functionDefinitionNode.functionSignature();
                Optional<ReturnTypeDescriptorNode> returnTypeDescriptorNode = functionSignatureNode.returnTypeDesc();
                if (returnTypeDescriptorNode.isEmpty()) {
                    addTextEdit(functionSignatureNode.lineRange().endLine(), " returns error?");
                } else {
                    addTextEdit(returnTypeDescriptorNode.get().type().lineRange().endLine(), "|error");
                }
            }

            // Generate the text edits for the error handler
            LineRange childLineRange = CommonUtils.getLineRangeOfBlockNode(functionBodyNode);
            addTextEdit(childLineRange.startLine(), prefix);
            addTextEdit(childLineRange.endLine(), suffix);
        }

        private boolean hasNoReturnError(FunctionDefinitionNode functionDefinitionNode) {
            return semanticModel.symbol(functionDefinitionNode)
                    .filter(symbol -> symbol.kind() == SymbolKind.FUNCTION)
                    .map(symbol -> ((FunctionSymbol) symbol).typeDescriptor().returnTypeDescriptor())
                    .map(returnType -> returnType.isEmpty() || !errorTypeSymbol.subtypeOf(returnType.get()))
                    .orElse(true);
        }

        private void addTextEdit(LinePosition position, String text) {
            textEdits.add(new TextEdit(CommonUtils.toRange(position), text));
        }

        public List<TextEdit> getTextEdits() {
            return textEdits;
        }
    }
}
