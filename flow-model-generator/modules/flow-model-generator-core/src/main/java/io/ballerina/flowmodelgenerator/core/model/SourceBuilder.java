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

package io.ballerina.flowmodelgenerator.core.model;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.formatter.core.FormattingTreeModifier;
import org.ballerinalang.formatter.core.options.FormattingOptions;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SourceBuilder {

    private TokenBuilder tokenBuilder;
    public final FlowNode flowNode;
    private final WorkspaceManager workspaceManager;
    private final Path filePath;
    private final Map<Path, List<TextEdit>> textEditsMap;

    public SourceBuilder(FlowNode flowNode, WorkspaceManager workspaceManager, Path filePath) {
        this.tokenBuilder = new TokenBuilder(this);
        this.textEditsMap = new HashMap<>();
        this.flowNode = flowNode;
        this.workspaceManager = workspaceManager;
        this.filePath = filePath;
    }

    public TokenBuilder token() {
        return tokenBuilder;
    }

    public SourceBuilder newVariable() {
        return newVariable(Property.DATA_TYPE_KEY);
    }

    public SourceBuilder newVariable(String typeKey) {
        Optional<Property> type = flowNode.getProperty(typeKey);
        Optional<Property> variable = flowNode.getProperty(Property.VARIABLE_KEY);

        if (type.isPresent() && variable.isPresent()) {
            tokenBuilder.expressionWithType(type.get(), variable.get())
                    .keyword(SyntaxKind.EQUAL_TOKEN);
        }
        return this;
    }

    public SourceBuilder textEdit(boolean isExpression, String fileName) {
        try {
            workspaceManager.loadProject(filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException(e);
        }
        Path resolvedPath = workspaceManager.projectRoot(filePath).resolve(fileName);
        Document document = workspaceManager.document(resolvedPath).orElseThrow();
        SyntaxTree syntaxTree = document.syntaxTree();
        LineRange lineRange = syntaxTree.rootNode().lineRange();

        // Add the current source to the end of the file
        textEdit(isExpression, resolvedPath, CommonUtils.toRange(lineRange.endLine()));

        acceptImport(resolvedPath);
        return this;
    }

    public SourceBuilder acceptImport(Path resolvedPath) {
        String org = flowNode.codedata().org();
        String module = flowNode.codedata().module();

        try {
            this.workspaceManager.loadProject(filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            return this;
        }
        // TODO: Check how we can only use this logic once compared to the textEdit(fileName) method
        Document document = workspaceManager.document(resolvedPath).orElseThrow();
        SyntaxTree syntaxTree = document.syntaxTree();
        LineRange lineRange = syntaxTree.rootNode().lineRange();

        if (org == null || module == null) {
            return this;
        }

        boolean importExists = syntaxTree.rootNode().kind() == SyntaxKind.MODULE_PART &&
                ((ModulePartNode) syntaxTree.rootNode()).imports().stream()
                        .anyMatch(importDeclarationNode -> importDeclarationNode.orgName().isPresent() &&
                                org.equals(importDeclarationNode.orgName().get().orgName().text()) &&
                                module.equals(importDeclarationNode.moduleName().get(0).text()));

        // Add the import statement
        if (!importExists) {
            tokenBuilder
                    .keyword(SyntaxKind.IMPORT_KEYWORD)
                    .name(flowNode.codedata().getImportSignature())
                    .endOfStatement();
            textEdit(false, resolvedPath, CommonUtils.toRange(lineRange.startLine()));
        }
        return this;
    }

    public SourceBuilder acceptImport() {
        return acceptImport(filePath);
    }

    public Optional<Symbol> getTypeSymbol(String typeName) {
        try {
            workspaceManager.loadProject(filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException(e);
        }
        SemanticModel semanticModel = workspaceManager.semanticModel(filePath).orElseThrow();
        return semanticModel.moduleSymbols().stream().filter(
                symbol -> symbol.kind() == SymbolKind.TYPE_DEFINITION && symbol.getName().isPresent() &&
                        symbol.getName().get().equals(typeName)).findFirst();
    }

    public SourceBuilder typedBindingPattern() {
        return typedBindingPattern(Property.DATA_TYPE_KEY);
    }

    public SourceBuilder typedBindingPattern(String typeKey) {
        Optional<Property> type = flowNode.getProperty(typeKey);
        Optional<Property> variable = flowNode.getProperty(Property.VARIABLE_KEY);

        if (type.isPresent() && variable.isPresent()) {
            tokenBuilder.expressionWithType(type.get(), variable.get());
        }
        return this;
    }

    public SourceBuilder body(List<FlowNode> flowNodes) {
        tokenBuilder.openBrace();
        children(flowNodes);
        tokenBuilder.closeBrace();
        return this;
    }

    public SourceBuilder children(List<FlowNode> flowNodes) {
        for (FlowNode node : flowNodes) {
            SourceBuilder sourceBuilder = new SourceBuilder(node, workspaceManager, filePath);
            Map<Path, List<TextEdit>> textEdits =
                    NodeBuilder.getNodeFromKind(node.codedata().node()).toSource(sourceBuilder);
            List<TextEdit> filePathTextEdits = textEdits.get(filePath);
            tokenBuilder.name(filePathTextEdits.get(filePathTextEdits.size() - 1).getNewText());
        }
        return this;
    }

    /**
     * Adds an <code>on fail</code> block to the provided <code>SourceBuilder</code>.
     * <pre>{@code
     *
     *     on fail <errorType> <errorVariable> {
     *          <statement>...
     *     }
     * }</pre>
     */
    public SourceBuilder onFailure() {
        Optional<Branch> optOnFailureBranch = flowNode.getBranch(Branch.ON_FAILURE_LABEL);
        if (optOnFailureBranch.isEmpty()) {
            return this;
        }
        Branch onFailureBranch = optOnFailureBranch.get();

        // Build the keywords
        tokenBuilder
                .keyword(SyntaxKind.ON_KEYWORD)
                .keyword(SyntaxKind.FAIL_KEYWORD);

        // Build the parameters
        Optional<Property> onErrorType = onFailureBranch.getProperty(Property.ON_ERROR_TYPE_KEY);
        Optional<Property> onErrorValue = onFailureBranch.getProperty(Property.ON_ERROR_VARIABLE_KEY);
        if (onErrorType.isPresent() && onErrorValue.isPresent()) {
            tokenBuilder.expressionWithType(onErrorType.get(), onErrorValue.get());
        }

        // Build the body
        body(onFailureBranch.children());
        return this;
    }

    /**
     * Adds function arguments to the provided <code>SourceBuilder</code>. This method processes the properties of the
     * <code>flowNode</code> and adds them as arguments to the <code>tokenBuilder</code>. it skips properties that are
     * either empty or have default values.
     *
     * <pre>{@code
     *  (<mandatory-arg>..., <named_arg>=<default-value>...);
     * }</pre>
     *
     * @param nodeTemplate      The <code>FlowNode</code> instance containing the template properties.
     * @param ignoredProperties A set of property keys to be ignored during the processing.
     */
    public SourceBuilder functionParameters(FlowNode nodeTemplate, Set<String> ignoredProperties) {
        tokenBuilder.keyword(SyntaxKind.OPEN_PAREN_TOKEN);
        Set<String> keys = new LinkedHashSet<>(nodeTemplate.properties().keySet());
        keys.removeAll(ignoredProperties);

        boolean hasEmptyParam = false;
        boolean firstParamAdded = false;
        for (String key : keys) {
            Optional<Property> property = flowNode.getProperty(key);
            Optional<Property> templateProperty = nodeTemplate.getProperty(key);

            if (property.isEmpty() || templateProperty.isEmpty() || property.get().value() == null ||
                    (property.get().optional() && property.get().value().equals(templateProperty.get().value()))) {
                hasEmptyParam = true;
                continue;
            }

            if (firstParamAdded) {
                tokenBuilder.keyword(SyntaxKind.COMMA_TOKEN);
            } else {
                firstParamAdded = true;
            }

            if (hasEmptyParam) {
                tokenBuilder
                        .name(key)
                        .keyword(SyntaxKind.EQUAL_TOKEN);
                hasEmptyParam = false;
            }

            tokenBuilder.expression(property.get());
        }

        tokenBuilder
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();
        return this;
    }

    public SourceBuilder textEdit(boolean isExpression) {
        return textEdit(isExpression, filePath, CommonUtils.toRange(flowNode.codedata().lineRange()));
    }

    public SourceBuilder textEdit(boolean isExpression, Path filePath, Range range) {
        String text = token().build(isExpression);
        tokenBuilder = new TokenBuilder(this);

        List<TextEdit> textEdits = textEditsMap.get(filePath);
        if (textEdits == null) {
            textEdits = new ArrayList<>();
        }
        textEdits.add(0, new TextEdit(range, text));
        textEditsMap.put(filePath, textEdits);

        return this;
    }

    public Map<Path, List<TextEdit>> build() {
        return textEditsMap;
    }

    public static class TokenBuilder extends FacetedBuilder<SourceBuilder> {

        private static final String WHITE_SPACE = " ";

        private static final FormattingTreeModifier
                treeModifier = new FormattingTreeModifier(FormattingOptions.builder().build(), (LineRange) null);
        private final StringBuilder sb;

        protected TokenBuilder(SourceBuilder parentBuilder) {
            super(parentBuilder);
            sb = new StringBuilder();
        }

        public TokenBuilder keyword(SyntaxKind keyword) {
            sb.append(keyword.stringValue()).append(WHITE_SPACE);
            return this;
        }

        public TokenBuilder name(String name) {
            sb.append(name);
            return this;
        }

        public TokenBuilder comment(String comment) {
            sb.append(comment);
            return this;
        }

        public TokenBuilder expression(Property property) {
            sb.append(property.toSourceCode());
            return this;
        }

        public TokenBuilder expressionWithType(Property type, Property variable) {
            sb.append(type.toSourceCode()).append(WHITE_SPACE).append(variable.toSourceCode()).append(WHITE_SPACE);
            return this;
        }

        public TokenBuilder expressionWithType(Property property) {
            sb.append(property.valueType()).append(WHITE_SPACE).append(property.toSourceCode());
            return this;
        }

        public TokenBuilder whiteSpace() {
            sb.append(WHITE_SPACE);
            return this;
        }

        public TokenBuilder openBrace() {
            sb.append(SyntaxKind.OPEN_BRACE_TOKEN.stringValue()).append(System.lineSeparator());
            return this;
        }

        public TokenBuilder closeBrace() {
            sb.append(WHITE_SPACE)
                    .append(SyntaxKind.CLOSE_BRACE_TOKEN.stringValue())
                    .append(System.lineSeparator());
            return this;
        }

        public TokenBuilder endOfStatement() {
            sb.append(SyntaxKind.SEMICOLON_TOKEN.stringValue()).append(System.lineSeparator());
            return this;
        }

        public String build(boolean isExpression) {
            String outputStr = sb.toString();
            Node modifiedNode = isExpression ? NodeParser.parseExpression(outputStr).apply(treeModifier) :
                    NodeParser.parseStatement(outputStr).apply(treeModifier);
            return modifiedNode.toSourceCode().strip();
        }
    }
}
