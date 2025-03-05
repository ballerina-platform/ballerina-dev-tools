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
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleDescriptor;
import io.ballerina.projects.ProjectException;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.formatter.core.FormattingTreeModifier;
import org.ballerinalang.formatter.core.options.FormattingOptions;
import org.ballerinalang.langserver.common.utils.CommonUtil;
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
import java.util.stream.Collectors;

public class SourceBuilder {

    private TokenBuilder tokenBuilder;
    public final FlowNode flowNode;
    public final WorkspaceManager workspaceManager;
    public final Path filePath;
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
        return newVariable(Property.TYPE_KEY);
    }

    public SourceBuilder newVariableWithInferredType() {
        Optional<Property> optionalType = flowNode.getProperty(Property.TYPE_KEY);
        Optional<Property> variable = flowNode.getProperty(Property.VARIABLE_KEY);

        if (optionalType.isEmpty() || variable.isEmpty()) {
            return this;
        }

        // Derive the type name form the inferred type
        Property type = optionalType.get();
        String typeName = type.value().toString();
        if (flowNode.codedata().inferredReturnType() != null) {
            Optional<Property> inferredParam = flowNode.properties().values().stream()
                    .filter(property -> property.codedata() != null && property.codedata().kind() != null &&
                            property.codedata().kind().equals(ParameterData.Kind.PARAM_FOR_TYPE_INFER.name()))
                    .findFirst();
            if (inferredParam.isPresent()) {
                String returnType = flowNode.codedata().inferredReturnType();
                String inferredType = inferredParam.get().value().toString();
                String inferredTypeDef = inferredParam.get()
                        .metadata().label();
                typeName = returnType.replace(inferredTypeDef, inferredType);
            }
        }

        tokenBuilder.expressionWithType(typeName, variable.get()).keyword(SyntaxKind.EQUAL_TOKEN);
        return this;
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

    public SourceBuilder textEdit(boolean isExpression, String fileName, boolean allowEdits) {
        Path resolvedPath = workspaceManager.projectRoot(filePath).resolve(fileName);
        LineRange flowNodeLineRange = flowNode.codedata().lineRange();
        if (flowNodeLineRange != null && allowEdits) {
            LinePosition startLine = flowNodeLineRange.startLine();
            LinePosition endLine = flowNodeLineRange.endLine();

            if (startLine.line() != 0 || startLine.offset() != 0 || endLine.line() != 0 || endLine.offset() != 0) {
                textEdit(isExpression, resolvedPath, CommonUtils.toRange(flowNodeLineRange));
                acceptImport(resolvedPath);
                return this;
            }
        }

        LinePosition linePosition;
        try {
            // If the file exists, get the end line of the file
            workspaceManager.loadProject(filePath);
            Document document = workspaceManager.document(resolvedPath).orElseThrow();
            linePosition = document.syntaxTree().rootNode().lineRange().endLine();
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException(e);
        } catch (ProjectException e) {
            // If the file does not exist, set the line range to (0,0)
            linePosition = LinePosition.from(0, 0);
        }

        // Add the current source to the end of the file
        textEdit(isExpression, resolvedPath, CommonUtils.toRange(linePosition));
        acceptImport(resolvedPath);
        return this;
    }

    // TODO: Need to reuse other textEdit methods
    public SourceBuilder textEdit(boolean isExpression, String fileName, LineRange lineRange, boolean allowEdits) {
        Path resolvedPath = workspaceManager.projectRoot(filePath).resolve(fileName);
        LineRange flowNodeLineRange = flowNode.codedata().lineRange();
        if (flowNodeLineRange != null && allowEdits) {
            LinePosition startLine = flowNodeLineRange.startLine();
            LinePosition endLine = flowNodeLineRange.endLine();

            if (startLine.line() != 0 || startLine.offset() != 0 || endLine.line() != 0 || endLine.offset() != 0) {
                textEdit(isExpression, resolvedPath, CommonUtils.toRange(flowNodeLineRange));
                acceptImport(resolvedPath);
                return this;
            }
        }

        textEdit(isExpression, resolvedPath, CommonUtils.toRange(lineRange));
        acceptImport(resolvedPath);
        return this;
    }

    public SourceBuilder acceptImport(Path resolvedPath) {
        Codedata codedata = flowNode.codedata();
        String org = codedata.org();
        String module = codedata.module();
        return acceptImport(resolvedPath, org, module);
    }

    public SourceBuilder acceptImportWithVariableType() {
        Optional<Property> optionalType = flowNode.getProperty(Property.TYPE_KEY);
        if (optionalType.isPresent()) {
            Property type = optionalType.get();

            // TODO: There can be cases where the return type and the value type both come from imported modules. We
            //  have
            //  to optimize how we handle the return type, as the current implementation does not allow the user to
            //  assign the error to a variable and handle it.
            // Add the import statements if exists in the return type
            if (type.codedata() != null && type.codedata().importStatements() != null &&
                    flowNode.getProperty(Property.CHECK_ERROR_KEY).map(property -> property.value().equals("false"))
                            .orElse(true)) {
                // TODO: Improve this logic to process all the imports at once
                for (String importStatement : type.codedata().importStatements().split(",")) {
                    String[] importParts = importStatement.split("/");
                    acceptImport(importParts[0], importParts[1]);
                }
            }
        }
        acceptImport();
        return this;
    }

    public SourceBuilder acceptImport() {
        return acceptImport(filePath);
    }

    public SourceBuilder acceptImport(String org, String module) {
        return acceptImport(filePath, org, module);
    }

    public SourceBuilder acceptImport(Path resolvedPath, String org, String module) {
        if (org == null || module == null || org.equals(CommonUtil.BALLERINA_ORG_NAME) &&
                CommonUtil.PRE_DECLARED_LANG_LIBS.contains(module)) {
            return this;
        }
        try {
            this.workspaceManager.loadProject(filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            return this;
        }
        // TODO: Check how we can only use this logic once compared to the textEdit(fileName) method
        Document document = workspaceManager.document(resolvedPath).orElseThrow();
        SyntaxTree syntaxTree = document.syntaxTree();
        LineRange lineRange = syntaxTree.rootNode().lineRange();

        Optional<Module> currentModule = this.workspaceManager.module(filePath);
        String currentModuleName = "";
        if (currentModule.isPresent()) {
            ModuleDescriptor descriptor = currentModule.get().descriptor();
            currentModuleName = descriptor.name().toString();
            if (descriptor.org().value().equals(org) && currentModuleName.equals(module)) {
                return this;
            }
        }

        boolean importExists = syntaxTree.rootNode().kind() == SyntaxKind.MODULE_PART &&
                ((ModulePartNode) syntaxTree.rootNode()).imports().stream().anyMatch(importDeclarationNode -> {
                    String moduleName = importDeclarationNode.moduleName().stream()
                            .map(IdentifierToken::text)
                            .collect(Collectors.joining("."));
                    return importDeclarationNode.orgName().isPresent() &&
                            org.equals(importDeclarationNode.orgName().get().orgName().text()) &&
                            module.equals(moduleName);
                });

        // Add the import statement
        if (!importExists) {
            String importSignature;
            Boolean generated = flowNode.codedata().isGenerated();
            // TODO: Check this condition for other cases like persist module
            if (!currentModuleName.isEmpty() && generated != null && generated) {
                importSignature = currentModuleName + "." + module;
            } else {
                importSignature = CommonUtils.getImportStatement(org, module, module);
            }
            tokenBuilder
                    .keyword(SyntaxKind.IMPORT_KEYWORD)
                    .name(importSignature)
                    .endOfStatement();
            textEdit(false, resolvedPath, CommonUtils.toRange(lineRange.startLine()));
        }
        return this;
    }

    public Optional<TypeDefinitionSymbol> getTypeDefinitionSymbol(String typeName) {
        try {
            workspaceManager.loadProject(filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException(e);
        }
        SemanticModel semanticModel = workspaceManager.semanticModel(filePath).orElseThrow();
        return semanticModel.moduleSymbols().stream()
                .filter(symbol -> symbol.kind() == SymbolKind.TYPE_DEFINITION && symbol.getName().isPresent() &&
                        symbol.getName().get().equals(typeName))
                .map(symbol -> (TypeDefinitionSymbol) symbol)
                .findFirst();
    }

    public SourceBuilder typedBindingPattern() {
        return typedBindingPattern(Property.TYPE_KEY);
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

        Optional<Property> ignoreProperty = onFailureBranch.getProperty(Property.IGNORE_KEY);
        if (ignoreProperty.isPresent() && ignoreProperty.get().value().equals("true")) {
            return this;
        }

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
        Map<String, Property> properties = nodeTemplate.properties();
        Set<String> keys = new LinkedHashSet<>(properties != null ? properties.keySet() : Set.of());
        keys.removeAll(ignoredProperties);

        boolean firstParamAdded = false;
        boolean missedDefaultValue = false;
        for (String key : keys) {
            Optional<Property> property = flowNode.getProperty(key);
            if (property.isEmpty()) {
                continue;
            }

            Property prop = property.get();
            String kind = prop.codedata().kind();
            boolean optional = prop.optional();

            if (kind.equals(ParameterData.Kind.PARAM_FOR_TYPE_INFER.name())) {
                continue;
            }

            if (firstParamAdded) {
                if ((kind.equals(ParameterData.Kind.REST_PARAMETER.name()))) {
                    if (isPropValueEmpty(prop) || ((List<?>) prop.value()).isEmpty()) {
                        continue;
                    }
                    if (hasRestParamValues(prop)) {
                        addRestParamValues(prop);
                        continue;
                    }
                } else if (kind.equals(ParameterData.Kind.INCLUDED_RECORD_REST.name())) {
                    if (isPropValueEmpty(prop) || ((List<?>) prop.value()).isEmpty()) {
                        continue;
                    }
                    if (hasRestParamValues(prop)) {
                        tokenBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                        addIncludedRecordRestParamValues(prop);
                        continue;
                    }
                }
            }

            if (!optional && kind.equals(ParameterData.Kind.REQUIRED.name())) {
                if (firstParamAdded) {
                    tokenBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                }
                tokenBuilder.expression(prop);
            } else if (kind.equals(ParameterData.Kind.INCLUDED_RECORD.name())) {
                if (isPropValueEmpty(prop)) {
                    continue;
                }
                if (firstParamAdded) {
                    tokenBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                }
                tokenBuilder.expression(prop);
            } else if (kind.equals(ParameterData.Kind.DEFAULTABLE.name())) {
                if (isPropValueEmpty(prop)) {
                    missedDefaultValue = true;
                    continue;
                }
                if (prop.placeholder().equals(prop.value())) {
                    continue;
                }
                if (firstParamAdded) {
                    tokenBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                }
                if (missedDefaultValue) {
                    tokenBuilder.name(prop.codedata().originalName()).whiteSpace()
                            .keyword(SyntaxKind.EQUAL_TOKEN).expression(prop);
                } else {
                    tokenBuilder.expression(prop);
                }
            } else if (kind.equals(ParameterData.Kind.INCLUDED_FIELD.name())) {
                if (isPropValueEmpty(prop)) {
                    continue;
                }
                if (firstParamAdded) {
                    tokenBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                }
                tokenBuilder.name(prop.codedata().originalName())
                        .whiteSpace().keyword(SyntaxKind.EQUAL_TOKEN).expression(prop);
            } else if (kind.equals(ParameterData.Kind.REST_PARAMETER.name())) {
                if (isPropValueEmpty(prop) || ((List<?>) prop.value()).isEmpty()) {
                    continue;
                }
                if (firstParamAdded) {
                    tokenBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                }
                addRestParamValues(prop);
            } else if (kind.equals(ParameterData.Kind.INCLUDED_RECORD_REST.name())) {
                if (isPropValueEmpty(prop) || ((List<?>) prop.value()).isEmpty()) {
                    continue;
                }
                if (firstParamAdded) {
                    tokenBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                }
                addIncludedRecordRestParamValues(prop);
            }

            firstParamAdded = true;
        }

        tokenBuilder
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();
        return this;
    }

    public SourceBuilder agentParameters(FlowNode nodeTemplate) {
        tokenBuilder.keyword(SyntaxKind.OPEN_PAREN_TOKEN);
        agentFirstParameter(nodeTemplate);
        agentSecondParameter(nodeTemplate);
        tokenBuilder
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();
        return this;
    }

    private SourceBuilder agentFirstParameter(FlowNode nodeTemplate) {
        Optional<Property> property = nodeTemplate.getProperty("model");
        if (property.isEmpty()) {
            return this;
        }
        tokenBuilder.expression(property.get());
        tokenBuilder.keyword(SyntaxKind.COMMA_TOKEN);
        return this;
    }

    private SourceBuilder agentSecondParameter(FlowNode nodeTemplate) {
        Optional<Property> property = nodeTemplate.getProperty("tools");
        if (property.isEmpty()) {
            return this;
        }

        tokenBuilder.keyword(SyntaxKind.OPEN_BRACKET_TOKEN);
        if (property.get().value() instanceof List<?> values) {
            if (!values.isEmpty()) {
                List<String> strValues = ((List<?>) property.get().value()).stream().map(Object::toString).toList();
                tokenBuilder.expression(String.join(", ", strValues));
            }
        }
        tokenBuilder.keyword(SyntaxKind.CLOSE_BRACKET_TOKEN);
        return this;
    }

    private boolean isPropValueEmpty(Property property) {
        return property.value() == null || (property.optional() && property.value().toString().isEmpty());
    }

    private boolean hasRestParamValues(Property prop) {
        if (prop.value() instanceof List<?> values) {
            return !values.isEmpty();
        }
        return false;
    }

    private void addRestParamValues(Property prop) {
        if (prop.value() instanceof List<?> values) {
            if (!values.isEmpty()) {
                List<String> strValues = ((List<?>) prop.value()).stream().map(Object::toString).toList();
                tokenBuilder.expression(String.join(", ", strValues));
            }
        }
    }

    private void addIncludedRecordRestParamValues(Property prop) {
        if (prop.value() instanceof List<?>) {
            List<Map> values = (List<Map>) prop.value();
            if (!values.isEmpty()) {
                List<String> result = new ArrayList<>();
                values.forEach(keyValuePair -> {
                    String key = (String) keyValuePair.keySet().iterator().next();
                    String value = keyValuePair.values().iterator().next().toString();
                    result.add(key + " = " + value);
                });
                tokenBuilder.expression(String.join(", ", result));
            }
        }
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

    public SourceBuilder comment() {
        String comment = token().skipFormatting().build(false);
        tokenBuilder = new TokenBuilder(this);

        List<TextEdit> textEdits = textEditsMap.get(filePath);
        if (textEdits == null) {
            textEdits = new ArrayList<>();
        }
        textEdits.add(0, new TextEdit(CommonUtils.toRange(flowNode.codedata().lineRange()), comment));
        textEditsMap.put(filePath, textEdits);

        return this;
    }

    public Map<Path, List<TextEdit>> build() {
        return textEditsMap;
    }

    public static class TokenBuilder extends FacetedBuilder<SourceBuilder> {

        private boolean skipFormatting;
        private static final String WHITE_SPACE = " ";

        private static final FormattingTreeModifier
                treeModifier = new FormattingTreeModifier(FormattingOptions.builder().build(), (LineRange) null);
        private final StringBuilder sb;

        public TokenBuilder(SourceBuilder parentBuilder) {
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

        public TokenBuilder resourcePath(String path) {
            sb.append(path);
            return this;
        }

        public TokenBuilder name(Property property) {
            sb.append(property.toSourceCode());
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

        public TokenBuilder expression(String exprAsStr) {
            sb.append(exprAsStr);
            return this;
        }

        public TokenBuilder expressionWithType(Property type, Property variable) {
            sb.append(type.toSourceCode()).append(WHITE_SPACE).append(variable.toSourceCode()).append(WHITE_SPACE);
            return this;
        }

        public TokenBuilder expressionWithType(String type, Property variable) {
            sb.append(type).append(WHITE_SPACE).append(variable.toSourceCode()).append(WHITE_SPACE);
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
                    .append(WHITE_SPACE);
            return this;
        }

        public TokenBuilder openParen() {
            sb.append(SyntaxKind.OPEN_PAREN_TOKEN.stringValue());
            return this;
        }

        public TokenBuilder closeParen() {
            sb.append(SyntaxKind.CLOSE_PAREN_TOKEN.stringValue());
            return this;
        }

        public TokenBuilder endOfStatement() {
            sb.append(SyntaxKind.SEMICOLON_TOKEN.stringValue()).append(System.lineSeparator());
            return this;
        }

        public TokenBuilder skipFormatting() {
            this.skipFormatting = true;
            return this;
        }

        public TokenBuilder descriptionDoc(String description) {
            sb.append(SyntaxKind.HASH_TOKEN.stringValue())
                    .append(WHITE_SPACE)
                    .append(description);
            if (!description.endsWith(System.lineSeparator())) {
                sb.append(System.lineSeparator());
            }
            return this;
        }

        public TokenBuilder parameterDoc(String paramName, String description) {
            sb.append(SyntaxKind.HASH_TOKEN.stringValue())
                    .append(WHITE_SPACE)
                    .append(SyntaxKind.PLUS_TOKEN.stringValue())
                    .append(WHITE_SPACE)
                    .append(paramName)
                    .append(WHITE_SPACE)
                    .append("-")
                    .append(WHITE_SPACE)
                    .append(description)
                    .append(System.lineSeparator());
            return this;
        }

        public String build(boolean isExpression) {
            String outputStr = sb.toString();
            if (skipFormatting) {
                return outputStr;
            }
            Node modifiedNode = isExpression ? NodeParser.parseExpression(outputStr).apply(treeModifier) :
                    NodeParser.parseStatement(outputStr).apply(treeModifier);
            return modifiedNode.toSourceCode().strip();
        }
    }
}
