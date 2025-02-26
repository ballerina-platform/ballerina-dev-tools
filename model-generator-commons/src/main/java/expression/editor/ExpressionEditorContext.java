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

package expression.editor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManagerProxy;
import org.eclipse.lsp4j.Position;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Represents the context for the expression editor.
 *
 * @since 2.0.0
 */
public abstract class ExpressionEditorContext {

    protected static final Gson gson = new Gson();
    protected final WorkspaceManagerProxy workspaceManagerProxy;
    protected final String fileUri;
    protected final Info info;
    protected final Path filePath;
    protected final DocumentContext documentContext;

    // State variables
    protected int expressionOffset;
    protected LineRange statementLineRange;
    protected boolean propertyInitialized;

    public ExpressionEditorContext(WorkspaceManagerProxy workspaceManagerProxy, String fileUri, Info info,
                                   Path filePath) {
        this.workspaceManagerProxy = workspaceManagerProxy;
        this.fileUri = fileUri;
        this.info = info;
        this.filePath = filePath;
        this.propertyInitialized = false;
        this.documentContext = new DocumentContext(workspaceManagerProxy, fileUri, filePath);
    }

    public ExpressionEditorContext(WorkspaceManagerProxy workspaceManagerProxy, String fileUri, Path filePath,
                                   Document document) {
        this.workspaceManagerProxy = workspaceManagerProxy;
        this.fileUri = fileUri;
        this.filePath = filePath;
        this.info = null;
        this.propertyInitialized = false;
        this.documentContext = new DocumentContext(workspaceManagerProxy, fileUri, filePath, document);
    }

    public LinePosition getStartLine() {
        List<ImportDeclarationNode> imports = documentContext.imports();
        if (imports.isEmpty()) {
            return info.startLine();
        }

        // Obtain the line position of the latest import statement
        LinePosition importEndLine = imports.getLast().lineRange().endLine();

        if (CommonUtils.isLinePositionAfter(info.startLine(), importEndLine)) {
            return info.startLine();
        }
        return importEndLine;
    }

    public Info info() {
        return info;
    }

    // TODO: Check how we can use SourceBuilder in place of this method
    protected abstract Optional<TextEdit> getImport();

    public abstract Optional<TextEdit> getImport(String importStatement);

    /**
     * Generates a Ballerina statement based on the availability of the type, and applies it to the document. Based on
     * the availability of the type, the statement will be in the format: `<type>? _ = <expr>;`.
     *
     * @return the line range of the generated statement.
     */
    public abstract LineRange generateStatement();

    public LineRange getExpressionLineRange() {
        LinePosition startLine = info().startLine();
        LinePosition endLine = LinePosition.from(startLine.line(), startLine.offset() + info().expression().length());
        Path fileName = filePath.getFileName();
        return LineRange.from(fileName == null ? "" : fileName.toString(), startLine, endLine);
    }

    /**
     * Gets the cursor position within the generated statement.
     *
     * @return the cursor position as a Position object
     */
    public Position getCursorPosition() {
        if (statementLineRange == null || info == null) {
            throw new IllegalStateException("Statement line range not initialized. Call generateStatement() first.");
        }
        return new Position(statementLineRange.startLine().line(),
                statementLineRange.startLine().offset() + info.offset() + expressionOffset);
    }

    /**
     * Applies a list of text edits to the current document and updates the content.
     *
     * @param textEdits the list of text edits to be applied
     */
    public void applyTextEdits(List<TextEdit> textEdits) {
        TextDocument newTextDocument = documentContext.document().textDocument()
                .apply(TextDocumentChange.from(textEdits.toArray(new TextEdit[0])));
        applyContent(newTextDocument);
    }

    /**
     * Applies the content of the given TextDocument to the current document.
     *
     * @param textDocument The TextDocument containing the new content
     */
    public void applyContent(TextDocument textDocument) {
        documentContext.document().modify()
                .withContent(String.join(System.lineSeparator(), textDocument.textLines()))
                .apply();
    }

    public String fileUri() {
        return fileUri;
    }

    public TextDocument textDocument() {
        return documentContext.document().textDocument();
    }

    public Path filePath() {
        return filePath;
    }

    public WorkspaceManager workspaceManager() {
        return workspaceManagerProxy.get(fileUri);
    }

    /**
     * Represents the json format of the expression editor context.
     *
     * @param expression The modified expression
     * @param startLine  The start line of the node
     * @param offset     The offset of cursor compared to the start of the expression
     * @param node       The node which contains the expression
     * @param branch     The branch of the expression if exists
     * @param property   The property of the expression
     */
    public record Info(String expression, LinePosition startLine, int offset, JsonObject node,
                       String branch, String property) {
    }

    /**
     * Encapsulates document and import related context with lazy loading capabilities.
     *
     * @since 2.0.0
     */
    public static class DocumentContext {

        private final WorkspaceManagerProxy workspaceManagerProxy;
        private final String fileUri;
        private final Path filePath;

        private Document document;
        private List<ImportDeclarationNode> imports;

        public DocumentContext(WorkspaceManagerProxy workspaceManagerProxy, String fileUri, Path filePath) {
            this(workspaceManagerProxy, fileUri, filePath, null);
        }

        public DocumentContext(WorkspaceManagerProxy workspaceManagerProxy, String fileUri, Path filePath,
                               Document document) {
            this.workspaceManagerProxy = workspaceManagerProxy;
            this.fileUri = fileUri;
            this.filePath = filePath;
            this.document = document;
        }

        public Document document() {
            if (document == null) {
                Optional<Document> optionalDocument = workspaceManagerProxy.get(fileUri).document(filePath);
                if (optionalDocument.isEmpty()) {
                    throw new IllegalStateException("Document not found for the given path: " + filePath);
                }
                document = optionalDocument.get();
            }
            return document;
        }

        public List<ImportDeclarationNode> imports() {
            if (imports == null) {
                if (document == null) {
                    document();
                }
                SyntaxTree syntaxTree = document.syntaxTree();
                imports = syntaxTree.rootNode().kind() == SyntaxKind.MODULE_PART
                        ? ((ModulePartNode) syntaxTree.rootNode()).imports().stream().toList()
                        : List.of();

            }
            return imports;
        }
    }
}
