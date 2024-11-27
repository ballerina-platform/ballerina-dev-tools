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

import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.TextDocument;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Generates the context for the copilot service.
 *
 * @since 2.0.0
 */
public class CopilotContextGenerator {

    private final WorkspaceManager workspaceManager;
    private final Path filePath;
    private final LinePosition position;

    private String prefix;
    private String suffix;
    private final Set<String> imports;

    private static final int INDENT_SPACES = 4;

    public CopilotContextGenerator(WorkspaceManager workspaceManager, Path filePath, LinePosition position) {
        this.workspaceManager = workspaceManager;
        this.filePath = filePath;
        this.position = position;
        this.imports = new LinkedHashSet<>();
    }

    public void generate() {
        try {
            this.workspaceManager.loadProject(filePath);
            Document document = this.workspaceManager.document(filePath).orElseThrow();
            TextDocument textDocument = document.textDocument();
            int textPosition = textDocument.textPositionFrom(position);
            char[] charArray = textDocument.toCharArray();

            ModulePartNode rootNode = document.syntaxTree().rootNode();
            Token token = rootNode.findToken(textPosition);

            int start = processImports(document);
            Path projectPath = this.workspaceManager.projectRoot(filePath);

            suffix = new String(charArray, textPosition, charArray.length - textPosition);
            prefix = String.join(System.lineSeparator(), imports) +
                    getDocumentContent(projectPath, "data_mappings.bal") +
                    getDocumentContent(projectPath, "types.bal") +
                    getDocumentContent(projectPath, "connections.bal") +
                    new String(charArray, start, textPosition - start) +
                    generatePrefixTrailingSpace(token, textDocument);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException(e);
        }
    }

    private String getDocumentContent(Path projectPath, String fileName) {
        try {
            Document document = this.workspaceManager.document(projectPath.resolve(fileName)).orElseThrow();
            TextDocument textDocument = document.textDocument();
            int start = processImports(document);
            char[] charArray = textDocument.toCharArray();
            return new String(charArray, start, charArray.length - start);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private int processImports(Document document) {
        ModulePartNode rootNode = document.syntaxTree().rootNode();
        NodeList<ImportDeclarationNode> imports = rootNode.imports();
        imports.forEach(importDeclarationNode -> this.imports.add(importDeclarationNode.toSourceCode().strip()));
        TextDocument textDocument = document.textDocument();
        return imports.isEmpty() ? 0 :
                textDocument.textPositionFrom(imports.get(imports.size() - 1).lineRange().endLine());
    }

    private String generatePrefixTrailingSpace(Token token, TextDocument textDocument) {
        String indent = switch (token.kind()) {
            case CLOSE_BRACE_TOKEN -> {
                int offset = token.lineRange().startLine().offset();
                yield " ".repeat(offset);
            }
            case SEMICOLON_TOKEN -> {
                NonTerminalNode parent = token.parent();
                while (!(parent instanceof StatementNode) && parent != null) {
                    parent = parent.parent();
                }
                if (parent == null) {
                    yield "";
                }
                int offset = parent.lineRange().startLine().offset();
                yield " ".repeat(Math.max(0, offset));
            }
            case OPEN_BRACE_TOKEN -> {
                String line = textDocument.line(token.lineRange().startLine().line()).text();
                int offset = -1;
                for (int i = 0; i < line.length(); i++) {
                    if (!Character.isWhitespace(line.charAt(i))) {
                        offset = i;
                        break;
                    }
                }
                yield " ".repeat(offset + INDENT_SPACES);
            }
            default -> "";
        };
        return System.lineSeparator() + indent;
    }

    public String prefix() {
        return prefix;
    }

    public String suffix() {
        return suffix;
    }
}
