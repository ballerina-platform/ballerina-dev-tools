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
import com.google.gson.JsonObject;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleDescriptor;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.Position;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents the context for the expression editor.
 *
 * @since 2.0.0
 */
public class ExpressionEditorContext {

    private static final Gson gson = new Gson();
    private final Info info;
    private final FlowNode flowNode;
    private final WorkspaceManager workspaceManager;
    private final Path filePath;
    private final List<ImportDeclarationNode> imports;

    // State variables
    private final Document document;
    private int expressionOffset;
    private LineRange statementLineRange;

    public ExpressionEditorContext(WorkspaceManager workspaceManager, Info info, Path filePath, Document document) {
        this.workspaceManager = workspaceManager;
        this.info = info;
        this.filePath = filePath;
        this.flowNode = gson.fromJson(info.node(), FlowNode.class);
        this.document = document;

        SyntaxTree syntaxTree = document.syntaxTree();
        imports = syntaxTree.rootNode().kind() == SyntaxKind.MODULE_PART
                ? ((ModulePartNode) syntaxTree.rootNode()).imports().stream().toList()
                : List.of();
    }

    public Optional<Property> getProperty() {
        if (info.property() == null || info.property().isEmpty()) {
            return Optional.empty();
        }
        if (info.branch() == null || info.branch().isEmpty()) {
            return flowNode.getProperty(info.property());
        }
        return flowNode.getBranch(info.branch()).flatMap(branch -> branch.getProperty(info.property()));
    }

    public boolean isNodeKind(List<NodeKind> nodeKinds) {
        if (flowNode.codedata() == null) {
            return false;
        }
        return nodeKinds.contains(flowNode.codedata().node());
    }

    public LinePosition getStartLine() {
        if (imports.isEmpty()) {
            return info.startLine();
        }

        // Obtain the line position of the latest import statement
        LinePosition importEndLine = imports.get(imports.size() - 1).lineRange().endLine();

        if (CommonUtils.isLinePositionAfter(info.startLine(), importEndLine)) {
            return info.startLine();
        }
        return importEndLine;
    }

    public Info info() {
        return info;
    }

    // TODO: Check how we can use SourceBuilder in place of this method
    private Optional<TextEdit> getImport() {
        String org = flowNode.codedata().org();
        String module = flowNode.codedata().module();

        if (org == null || module == null || org.equals(CommonUtil.BALLERINA_ORG_NAME) &&
                CommonUtil.PRE_DECLARED_LANG_LIBS.contains(module)) {
            return Optional.empty();
        }
        return getImport(CommonUtils.getImportStatement(org, module, module));
    }

    private Optional<TextEdit> getImport(String importStatement) {
        try {
            this.workspaceManager.loadProject(filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            return Optional.empty();
        }

        // Check if the import statement represents the current module
        Optional<Module> currentModule = this.workspaceManager.module(filePath);
        if (currentModule.isPresent()) {
            ModuleDescriptor descriptor = currentModule.get().descriptor();
            if (CommonUtils.getImportStatement(descriptor.org().toString(), descriptor.packageName().value(),
                    descriptor.name().toString()).equals(importStatement)) {
                return Optional.empty();
            }
        }

        // Check if the import statement already exists
        boolean importExists = imports.stream().anyMatch(importDeclarationNode -> {
            String importText = importDeclarationNode.toSourceCode();
            int importIndex = importText.indexOf("import ");
            int semicolonIndex = importText.indexOf(";");
            if (importIndex >= 0 && semicolonIndex >= 0) {
                String stmt = importText.substring(importIndex + 7, semicolonIndex);
                return stmt.trim().equals(importStatement);
            }
            return false;
        });

        // Generate the import statement if not exists
        if (!importExists) {
            String stmt = new SourceBuilder.TokenBuilder(null)
                    .keyword(SyntaxKind.IMPORT_KEYWORD)
                    .name(flowNode.codedata().getImportSignature())
                    .endOfStatement()
                    .build(false);
            TextEdit textEdit = TextEdit.from(TextRange.from(0, 0), stmt);
            return Optional.of(textEdit);
        }
        return Optional.empty();
    }

    /**
     * Generates a Ballerina statement based on the availability of the type, and applies it to the document. Based on
     * the availability of the type, the statement will be in the format: `<type>? _ = <expr>;`.
     *
     * @return the line range of the generated statement.
     */
    public LineRange generateStatement() {
        String prefix;
        Optional<Property> optionalProperty = getProperty();
        List<TextEdit> textEdits = new ArrayList<>();
        if (optionalProperty.isPresent()) {
            Property property = optionalProperty.get();
            if (property.valueTypeConstraint() != null) {
                prefix = String.format("%s _ = ", property.valueTypeConstraint());
            } else {
                prefix = "_ = ";
            }

            if (property.codedata() != null) {
                String importStatements = property.codedata().importStatements();
                if (importStatements != null && !importStatements.isEmpty()) {
                    List.of(importStatements.split(",")).forEach(importStmt -> {
                        getImport(importStmt).ifPresent(textEdits::add);
                    });
                }
            }
        } else {
            prefix = "_ = ";
        }

        if (isNodeKind(List.of(NodeKind.NEW_CONNECTION, NodeKind.FUNCTION_CALL, NodeKind.REMOTE_ACTION_CALL,
                NodeKind.RESOURCE_ACTION_CALL))) {
            getImport().ifPresent(textEdits::add);
        }

        String statement = String.format("%s%s;%n", prefix, info.expression());
        this.expressionOffset = prefix.length();

        TextDocument textDocument = document.textDocument();
        int textPosition = textDocument.textPositionFrom(info.startLine());

        textEdits.add(TextEdit.from(TextRange.from(textPosition, 0), statement));
        TextDocument newTextDocument = textDocument
                .apply(TextDocumentChange.from(textEdits.toArray(new TextEdit[0])));
        applyContent(newTextDocument);

        LinePosition startLine = info.startLine();
        LinePosition endLineRange = LinePosition.from(startLine.line(),
                startLine.offset() + statement.length());
        this.statementLineRange = LineRange.from(filePath.toString(), startLine, endLineRange);
        return statementLineRange;
    }

    /**
     * Gets the cursor position within the generated statement.
     *
     * @return the cursor position as a Position object
     */
    public Position getCursorPosition() {
        return new Position(statementLineRange.startLine().line(),
                statementLineRange.startLine().offset() + info.offset() + expressionOffset);
    }

    /**
     * Applies the content of the given TextDocument to the current document.
     *
     * @param textDocument The TextDocument containing the new content
     */
    public void applyContent(TextDocument textDocument) {
        document.modify()
                .withContent(String.join(System.lineSeparator(), textDocument.textLines()))
                .apply();
    }

    public Iterable<Diagnostic> syntaxDiagnostics() {
        return document.syntaxTree().diagnostics();
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
}
