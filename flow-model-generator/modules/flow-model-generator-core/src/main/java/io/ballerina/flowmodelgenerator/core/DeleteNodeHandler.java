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
import com.google.gson.JsonObject;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.ElseBlockNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.common.utils.PositionUtil;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates text edits for the nodes that are requested to delete.
 *
 * @since 1.4.0
 */
public class DeleteNodeHandler {

    private static final Gson gson = new Gson();
    private final FlowNode nodeToDelete;
    private final Path filePath;

    public DeleteNodeHandler(JsonElement nodeToDelete, Path filePath) {
        this.nodeToDelete = new Gson().fromJson(nodeToDelete, FlowNode.class);
        this.filePath = filePath;
    }

    @Deprecated
    public JsonElement getTextEditsToDeletedNode(Document document, Project project) {
        LineRange lineRange = nodeToDelete.codedata().lineRange();
        return getTextEditsToDeletedNode(lineRange, filePath, document, project);
    }

    public static JsonElement getTextEditsToDeletedNode(JsonElement node, Path filePath,
                                                        Document document, Project project) {
        return getTextEditsToDeletedNode(getNodeLineRange(node), filePath, document, project);
    }

    private static JsonElement getTextEditsToDeletedNode(LineRange lineRange, Path filePath,
                                                         Document document, Project project) {
        TextDocument textDocument = document.textDocument();
        int startTextPosition = textDocument.textPositionFrom(lineRange.startLine());
        int endTextPosition = textDocument.textPositionFrom(lineRange.endLine());

        io.ballerina.tools.text.TextEdit te = io.ballerina.tools.text.TextEdit.from(TextRange.from(startTextPosition,
                endTextPosition - startTextPosition), "");
        TextDocument apply = textDocument
                .apply(TextDocumentChange.from(List.of(te).toArray(new io.ballerina.tools.text.TextEdit[0])));
        Document modifiedDoc =
                project.duplicate().currentPackage().module(document.module().moduleId())
                        .document(document.documentId()).modify().withContent(String.join(System.lineSeparator(),
                                apply.textLines())).apply();
        ModulePartNode modulePartNode = modifiedDoc.syntaxTree().rootNode();
        NodeList<ImportDeclarationNode> imports = modulePartNode.imports();

        List<TextEdit> textEdits = new ArrayList<>();
        DiagnosticResult diagnostics = modifiedDoc.module().getCompilation().diagnostics();
        for (Diagnostic diagnostic : diagnostics.diagnostics()) {
            DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
            if (diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR &&
                    diagnosticInfo.code().equals(DiagnosticErrorCode.UNUSED_MODULE_PREFIX.diagnosticId())) {
                ImportDeclarationNode importNode = getUnusedImport(diagnostic.location().lineRange(), imports);
                TextEdit deleteImportTextEdit = new TextEdit(CommonUtils.toRange(importNode.lineRange()), "");
                textEdits.add(deleteImportTextEdit);
            }
        }

        LineRange nodeRangeToDelete = checkElseToDelete(document, startTextPosition, endTextPosition);
        if (nodeRangeToDelete == null) {
            nodeRangeToDelete = lineRange;
        }
        TextEdit textEdit = new TextEdit(CommonUtils.toRange(nodeRangeToDelete), "");
        textEdits.add(textEdit);
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        textEditsMap.put(filePath, textEdits);
        return gson.toJsonTree(textEditsMap);
    }

    private static LineRange getNodeLineRange(JsonElement node) {
        FlowNode nodeToDelete = gson.fromJson(node, FlowNode.class);
        if (nodeToDelete.codedata() != null) {
            return nodeToDelete.codedata().lineRange();
        }

        // Assume that the node has the following attributes: startLine, startColumn, endLine, endColumn
        JsonObject jsonObject = node.getAsJsonObject();
        LinePosition startLinePosition = LinePosition.from(
                jsonObject.get("startLine").getAsInt(), jsonObject.get("startColumn").getAsInt());
        LinePosition endLinePosition = LinePosition.from(
                jsonObject.get("endLine").getAsInt(), jsonObject.get("endColumn").getAsInt());
        return LineRange.from(jsonObject.get("filePath").getAsString(), startLinePosition, endLinePosition);
    }

    private static ImportDeclarationNode getUnusedImport(LineRange diagnosticLocation,
                                                         NodeList<ImportDeclarationNode> imports) {
        for (ImportDeclarationNode importNode : imports) {
            if (PositionUtil.isWithinLineRange(diagnosticLocation, importNode.lineRange())) {
                return importNode;
            }
        }
        throw new IllegalStateException("There should be an import node");
    }

    private static LineRange checkElseToDelete(Document document, int nodeStart, int nodeEnd) {
        ModulePartNode modulePartNode = document.syntaxTree().rootNode();
        NonTerminalNode node = modulePartNode.findNode(TextRange.from(nodeStart, nodeEnd - nodeStart)).parent();
        if (node != null && node.kind() == SyntaxKind.BLOCK_STATEMENT) {
            BlockStatementNode blockStatementNode = (BlockStatementNode) node;
            if (blockStatementNode.statements().size() == 1) {
                NonTerminalNode parent = node.parent();
                if (parent.kind() == SyntaxKind.ELSE_BLOCK) {
                    return parent.lineRange();
                }
                if (parent.kind() == SyntaxKind.IF_ELSE_STATEMENT) {
                    IfElseStatementNode ifElseStmt = (IfElseStatementNode) parent;
                    NonTerminalNode p = ifElseStmt.parent();
                    if (p != null && p.kind() == SyntaxKind.ELSE_BLOCK) {
                        ElseBlockNode elseBlock = (ElseBlockNode) p;
                        return LineRange.from(parent.lineRange().fileName(), elseBlock.lineRange().startLine(),
                                ifElseStmt.ifBody().lineRange().endLine());
                    }
                }
            }
        }
        return null;
    }
}
