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
import io.ballerina.compiler.syntax.tree.ChildNodeList;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates error handlers for function definitions that does not contain a global error handler.
 *
 * @since 1.4.0
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
        try {
            workspaceManager.loadProject(filePath);
            Document document = workspaceManager.document(filePath).orElseThrow();
            rootNode = document.syntaxTree().rootNode();
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException("Failed to get the document", e);
        }
        FunctionVisitor functionVisitor = new FunctionVisitor();
        rootNode.accept(functionVisitor);
        List<TextEdit> textEdits = functionVisitor.getTextEdits();
        return gson.toJsonTree(Map.of(filePath, textEdits));
    }

    private static class FunctionVisitor extends NodeVisitor {

        private static final String prefix = "do {\n";
        private static final String suffix = "\n} on fail error e {\n\n}";
        private final List<TextEdit> textEdits;

        public FunctionVisitor() {
            this.textEdits = new ArrayList<>();
        }

        @Override
        public void visit(FunctionDefinitionNode functionDefinitionNode) {
            // Check if the function already contains a global error handler
            FunctionBodyNode functionBodyNode = functionDefinitionNode.functionBody();
            ChildNodeList children = functionBodyNode.children();
            if (children.size() == 3 && children.get(1).kind() == SyntaxKind.DO_STATEMENT) {
                return;
            }

            // Generate the text edits
            LineRange childLineRange = functionBodyNode.lineRange();
            textEdits.add(new TextEdit(CommonUtils.toRange(childLineRange.startLine()), prefix));
            textEdits.add(new TextEdit(CommonUtils.toRange(childLineRange.endLine()), suffix));
        }

        public List<TextEdit> getTextEdits() {
            return textEdits;
        }
    }
}
