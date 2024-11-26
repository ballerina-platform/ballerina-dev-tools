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
import io.ballerina.compiler.syntax.tree.IdentifierToken;
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
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public ExpressionEditorContext(WorkspaceManager workspaceManager, Info info, Path filePath) {
        this.workspaceManager = workspaceManager;
        this.info = info;
        this.filePath = filePath;
        this.flowNode = gson.fromJson(info.node(), FlowNode.class);

        Document document;
        try {
            this.workspaceManager.loadProject(filePath);
            document = workspaceManager.document(filePath).orElseThrow();
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException("Error while loading the project");
        }
        SyntaxTree syntaxTree = document.syntaxTree();
        imports = syntaxTree.rootNode().kind() == SyntaxKind.MODULE_PART ?
                ((ModulePartNode) syntaxTree.rootNode()).imports().stream().toList() :
                List.of();
    }

    public Optional<Property> getProperty() {
        if (info.branch() == null || info.branch().isEmpty()) {
            return flowNode.getProperty(info.property());
        }
        return flowNode.getBranch(info.branch()).flatMap(branch -> branch.getProperty(info.property()));
    }

    public boolean isNodeKind(List<NodeKind> nodeKinds) {
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
    public Optional<TextEdit> getImport() {
        String org = flowNode.codedata().org();
        String module = flowNode.codedata().module();

        if (org == null || module == null || org.equals(CommonUtil.BALLERINA_ORG_NAME) &&
                CommonUtil.PRE_DECLARED_LANG_LIBS.contains(module)) {
            return Optional.empty();
        }
        try {
            this.workspaceManager.loadProject(filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            return Optional.empty();
        }

        Optional<Module> currentModule = this.workspaceManager.module(filePath);
        if (currentModule.isPresent()) {
            ModuleDescriptor descriptor = currentModule.get().descriptor();
            if (descriptor.org().value().equals(org) && descriptor.name().toString().equals(module)) {
                return Optional.empty();
            }
        }

        boolean importExists = imports.stream().anyMatch(importDeclarationNode -> {
            String moduleName = importDeclarationNode.moduleName().stream()
                    .map(IdentifierToken::text)
                    .collect(Collectors.joining("."));
            return importDeclarationNode.orgName().isPresent() &&
                    org.equals(importDeclarationNode.orgName().get().orgName().text()) &&
                    module.equals(moduleName);
        });

        // Add the import statement
        if (!importExists) {
            String importStatement = new SourceBuilder.TokenBuilder(null)
                    .keyword(SyntaxKind.IMPORT_KEYWORD)
                    .name(flowNode.codedata().getImportSignature())
                    .endOfStatement()
                    .build(false);
            TextEdit textEdit = TextEdit.from(TextRange.from(0, 0), importStatement);
            return Optional.of(textEdit);
        }
        return Optional.empty();
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
