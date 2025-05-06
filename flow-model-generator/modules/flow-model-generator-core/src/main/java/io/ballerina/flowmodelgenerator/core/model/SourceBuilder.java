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
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.flowmodelgenerator.core.utils.FileSystemUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.DefaultValueGeneratorUtil;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleDescriptor;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.formatter.core.FormattingTreeModifier;
import org.ballerinalang.formatter.core.options.FormattingOptions;
import org.ballerinalang.langserver.LSClientLogger;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.common.utils.RecordUtil;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SourceBuilder {

    private TokenBuilder tokenBuilder;
    public final Path filePath;
    public final FlowNode flowNode;
    public final WorkspaceManager workspaceManager;
    private final Map<Path, List<TextEdit>> textEditsMap;
    private final Set<String> imports;
    private final LSClientLogger lsClientLogger;
    private Range defaultRange;

    // Project file names
    private static final String CONNECTIONS_BAL = "connections.bal";
    private static final String AUTOMATION_BAL = "automation.bal";
    private static final String AGENTS_BAL = "agents.bal";
    private static final String DATA_MAPPINGS_BAL = "data_mappings.bal";
    private static final String FUNCTIONS_BAL = "functions.bal";
    private static final String BALLERINA_FILE_SUFFIX = ".bal";

    public SourceBuilder(FlowNode flowNode, WorkspaceManager workspaceManager, Path filePath,
                         LSClientLogger lsClientLogger) {
        this.tokenBuilder = new TokenBuilder(this);
        this.textEditsMap = new HashMap<>();
        this.flowNode = flowNode;
        this.workspaceManager = workspaceManager;
        this.imports = new HashSet<>();
        this.lsClientLogger = lsClientLogger;

        Codedata codedata = flowNode.codedata();
        if (codedata == null) {
            this.filePath = filePath;
        } else {
            NodeKind nodeKind = codedata.node();
            if (filePath.endsWith(AGENTS_BAL) && (nodeKind == NodeKind.FUNCTION_DEFINITION
                    || nodeKind == NodeKind.CLASS_INIT
                    || nodeKind == NodeKind.RESOURCE_ACTION_CALL
                    || nodeKind == NodeKind.REMOTE_ACTION_CALL)) {
                nodeKind = NodeKind.AGENT;
            }
            this.filePath = resolvePath(filePath, nodeKind, codedata.lineRange(), codedata.isNew());
        }
    }

    public SourceBuilder(FlowNode flowNode, WorkspaceManager workspaceManager, Path filePath) {
        this(flowNode, workspaceManager, filePath, null);
    }

    private Path resolvePath(Path inputPath, NodeKind node, LineRange lineRange, Boolean isNew) {
        if (Boolean.TRUE.equals(isNew) || lineRange == null) {
            String defaultFile = switch (node) {
                case NEW_CONNECTION -> CONNECTIONS_BAL;
                case DATA_MAPPER_DEFINITION -> DATA_MAPPINGS_BAL;
                case FUNCTION_DEFINITION, NP_FUNCTION, NP_FUNCTION_DEFINITION -> FUNCTIONS_BAL;
                case AUTOMATION -> AUTOMATION_BAL;
                case AGENT -> AGENTS_BAL;
                default -> null;
            };
            if (defaultFile == null) {
                if (lineRange == null) {
                    throw new IllegalArgumentException("Cannot determine the line range");
                }
                defaultRange = CommonUtils.toRange(lineRange);
                return inputPath;
            }

            // Create the document if not exists
            Path resolvedPath = workspaceManager.projectRoot(inputPath).resolve(defaultFile);
            try {
                workspaceManager.loadProject(inputPath);
                Document document = FileSystemUtils.getDocument(workspaceManager, resolvedPath);
                // If the file exists, get the end of the file
                defaultRange = CommonUtils.toRange(document.syntaxTree().rootNode().lineRange().endLine());
            } catch (WorkspaceDocumentException | EventSyncException e) {
                throw new RuntimeException(e);
            }
            return resolvedPath;
        }

        // Set the default range using the codedata
        defaultRange = CommonUtils.toRange(lineRange);

        // If the file name is not a bal file, then defaults to the filename in the line range.
        if (!inputPath.toString().endsWith(BALLERINA_FILE_SUFFIX)) {
            return inputPath.resolve(lineRange.fileName());
        }
        return inputPath;
    }

    public TokenBuilder token() {
        return tokenBuilder;
    }

    public SourceBuilder newVariable() {
        return newVariable(Property.TYPE_KEY);
    }

    public SourceBuilder newVariableWithInferredType() {
        Optional<Property> optionalType = getProperty(Property.TYPE_KEY);
        Optional<Property> variable = getProperty(Property.VARIABLE_KEY);

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
        Optional<Property> type = getProperty(typeKey);
        Optional<Property> variable = getProperty(Property.VARIABLE_KEY);

        if (type.isPresent() && variable.isPresent()) {
            tokenBuilder.expressionWithType(type.get(), variable.get())
                    .keyword(SyntaxKind.EQUAL_TOKEN);
        }
        return this;
    }

    public SourceBuilder acceptImportWithVariableType() {
        Optional<Property> optionalType = getProperty(Property.TYPE_KEY);
        if (optionalType.isPresent()) {
            Property type = optionalType.get();

            // TODO: There can be cases where the return type and the value type both come from imported modules. We
            //  have to optimize how we handle the return type, as the current implementation does not allow the user
            //  to assign the error to a variable and handle it.
            // Add the import statements if exists in the return type
            if (type.imports() != null && getProperty(Property.CHECK_ERROR_KEY)
                    .map(property -> property.value().equals("false")).orElse(true)) {
                // TODO: Improve this logic to process all the imports at once
                type.imports().values().forEach(moduleId -> {
                    String[] importParts = moduleId.split("/");
                    acceptImport(importParts[0], importParts[1].split(":")[0]);
                });
            }
        }
        acceptImport();
        return this;
    }

    public Optional<Property> getProperty(String key) {
        Optional<Property> property = flowNode.getProperty(key);
        property.ifPresent(prop -> {
            Map<String, String> propImports = prop.imports();
            if (propImports != null) {
                propImports.values().forEach(propImport -> imports.add(propImport.split(":")[0]));
            }
        });
        return property;
    }

    public SourceBuilder acceptImport() {
        Codedata codedata = flowNode.codedata();
        String org = codedata.org();
        String module = codedata.module();
        return acceptImport(org, module);
    }

    // TODO: This should be removed once the codedata is refactored to capture the module name
    public SourceBuilder addImport(String text) {
        imports.add(text);
        return this;
    }

    public SourceBuilder acceptImport(String org, String module) {
        return acceptImport(org, module, false);
    }

    public SourceBuilder acceptImport(String org, String module, boolean defaultNamespace) {
        if (org == null || module == null || org.equals(CommonUtil.BALLERINA_ORG_NAME) &&
                CommonUtil.PRE_DECLARED_LANG_LIBS.contains(module)) {
            return this;
        }
        try {
            this.workspaceManager.loadProject(filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            return this;
        }

        // Generate the import signature
        String importSignature = CommonUtils.getImportStatement(org, module, module);
        if (defaultNamespace) {
            importSignature += " as _";
        }
        imports.add(importSignature);
        return this;
    }

    public Optional<String> getExpressionBodyText(String typeName, Map<String, String> imports) {
        PackageUtil.loadProject(workspaceManager, filePath);
        Document document = FileSystemUtils.getDocument(workspaceManager, filePath);

        // Obtain the symbols of the imports
        Map<String, BLangPackage> packageMap = new HashMap<>();
        if (imports != null) {
            imports.values().forEach(moduleId -> {
                ModuleInfo moduleInfo = ModuleInfo.from(moduleId);
                PackageUtil.pullModuleAndNotify(lsClientLogger, moduleInfo).ifPresent(pkg ->
                        packageMap.put(CommonUtils.getDefaultModulePrefix(pkg.packageName().value()),
                                PackageUtil.getCompilation(pkg).defaultModuleBLangPackage())
                );
            });
        }

        SemanticModel semanticModel = FileSystemUtils.getSemanticModel(workspaceManager, filePath);
        Optional<TypeSymbol> optionalType = semanticModel.types().getType(document, typeName, packageMap);
        if (optionalType.isEmpty()) {
            return Optional.empty();
        }
        TypeSymbol typeSymbol = optionalType.get();

        if (typeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            TypeReferenceTypeSymbol typeDefinitionSymbol = (TypeReferenceTypeSymbol) typeSymbol;
            typeSymbol = typeDefinitionSymbol.typeDescriptor();
        }

        String bodyText;
        if (typeSymbol.typeKind() == TypeDescKind.RECORD) {
            String recordFields =
                    RecordUtil.getFillAllRecordFieldInsertText(((RecordTypeSymbol) typeSymbol).fieldDescriptors());
            bodyText = String.format("{%n%s%n}", recordFields);
        } else {
            bodyText = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
        }
        return Optional.of(bodyText);
    }

    public SourceBuilder typedBindingPattern() {
        return typedBindingPattern(Property.TYPE_KEY);
    }

    public SourceBuilder typedBindingPattern(String typeKey) {
        Optional<Property> type = getProperty(typeKey);
        Optional<Property> variable = getProperty(Property.VARIABLE_KEY);

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
            SourceBuilder sourceBuilder = new SourceBuilder(node, workspaceManager, filePath, lsClientLogger);
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
        return functionParameters(nodeTemplate, ignoredProperties, false);
    }

    public SourceBuilder functionParameters(FlowNode nodeTemplate, Set<String> ignoredProperties, boolean setFormat) {
        tokenBuilder.keyword(SyntaxKind.OPEN_PAREN_TOKEN);
        if (setFormat) {
            tokenBuilder.name(System.lineSeparator());
        }
        Map<String, Property> properties = nodeTemplate.properties();
        Set<String> keys = new LinkedHashSet<>(properties != null ? properties.keySet() : Set.of());
        keys.removeAll(ignoredProperties);

        boolean firstParamAdded = false;
        boolean missedDefaultValue = false;
        for (String key : keys) {
            Optional<Property> property = getProperty(key);
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
                        tokenBuilder.keyword(SyntaxKind.COMMA_TOKEN);
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
                tokenBuilder.param(prop);
            } else if (kind.equals(ParameterData.Kind.INCLUDED_RECORD.name())) {
                if (isPropValueEmpty(prop)) {
                    continue;
                }
                if (firstParamAdded) {
                    tokenBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                }
                tokenBuilder.param(prop);
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
                    tokenBuilder.param(prop);
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

    public SourceBuilder textEdit() {
        return textEdit(SourceKind.STATEMENT, filePath, defaultRange);
    }

    public SourceBuilder textEdit(SourceKind sourceKind) {
        return textEdit(sourceKind, filePath, defaultRange);
    }

    public SourceBuilder textEdit(SourceKind sourceKind, Path filePath, Range range) {
        String text = token().build(sourceKind);
        tokenBuilder = new TokenBuilder(this);

        List<TextEdit> textEdits = textEditsMap.get(filePath);
        if (textEdits == null) {
            textEdits = new ArrayList<>();
        }
        textEdits.addFirst(new TextEdit(range, text));
        textEditsMap.put(filePath, textEdits);

        return this;
    }

    public SourceBuilder comment() {
        String comment = token().skipFormatting().build(SourceKind.STATEMENT);
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
        // Add the imports if exists
        addImports();
        return textEditsMap;
    }

    private void addImports() {
        try {
            this.workspaceManager.loadProject(filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            return;
        }
        // Obtain the start line of the document
        Document document = FileSystemUtils.getDocument(workspaceManager, filePath);
        SyntaxTree syntaxTree = document.syntaxTree();
        LinePosition startLine = syntaxTree.rootNode().lineRange().startLine();
        Range startLineRange = CommonUtils.toRange(startLine);

        // Obtain the module descriptor of the current module
        String currentModuleOrg;
        String currentModuleName;
        Optional<Module> currentModule = this.workspaceManager.module(filePath);
        boolean isGenerated;
        if (currentModule.isPresent()) {
            ModuleDescriptor descriptor = currentModule.get().descriptor();
            currentModuleName = descriptor.name().toString();
            currentModuleOrg = descriptor.org().value();
            imports.remove(currentModuleOrg + "/" + currentModuleName);
            isGenerated = Boolean.TRUE.equals(flowNode.codedata().isGenerated());
        } else {
            currentModuleName = "";
            isGenerated = false;
        }

        // Remove the existing imports
        ModulePartNode rootNode = syntaxTree.rootNode();
        for (ImportDeclarationNode existingImport : rootNode.imports()) {
            String moduleName = existingImport.moduleName().stream()
                    .map(IdentifierToken::text)
                    .collect(Collectors.joining("."));
            String prefix = existingImport.orgName().map(org -> org.orgName().text() + "/").orElse("");
            imports.remove(prefix + moduleName);
        }

        // Generate the text edits for the imports
        for (String moduleImport : imports) {
            // TODO: Check this condition for other cases like persist module
            String importPrefix = isGenerated ? currentModuleName + "." : "";
            tokenBuilder
                    .keyword(SyntaxKind.IMPORT_KEYWORD)
                    .name(importPrefix + moduleImport)
                    .endOfStatement();
            textEdit(SourceKind.IMPORT, filePath, startLineRange);
        }
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

        public TokenBuilder param(Property property) {
            String source = property.toSourceCode();
            if (source.startsWith("$")) {
                source = "'" + source.substring(1);
            }
            sb.append(source);
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

        public TokenBuilder rightDoubleArrowToken() {
            sb.append(WHITE_SPACE).append(SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN.stringValue()).append(WHITE_SPACE);
            return this;
        }

        public TokenBuilder equal() {
            sb.append(WHITE_SPACE).append(SyntaxKind.EQUAL_TOKEN.stringValue()).append(WHITE_SPACE);
            return this;
        }

        public TokenBuilder semicolon() {
            sb.append(SyntaxKind.SEMICOLON_TOKEN.stringValue()).append(System.lineSeparator());
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
            if (description != null && !description.isEmpty()) {
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
            }
            return this;
        }

        public TokenBuilder returnDoc(String returnDescription) {
            if (returnDescription != null && !returnDescription.isEmpty()) {
                sb.append(SyntaxKind.HASH_TOKEN.stringValue())
                        .append(WHITE_SPACE)
                        .append(SyntaxKind.PLUS_TOKEN.stringValue())
                        .append(WHITE_SPACE)
                        .append(SyntaxKind.RETURN_KEYWORD.stringValue())
                        .append(WHITE_SPACE)
                        .append("-")
                        .append(WHITE_SPACE)
                        .append(returnDescription)
                        .append(System.lineSeparator());
            }
            return this;
        }

        public String build(SourceKind kind) {
            String outputStr = sb.toString();
            if (skipFormatting) {
                return outputStr;
            }

            Node parsedNode = switch (kind) {
                case DECLARATION -> NodeParser.parseModuleMemberDeclaration(outputStr);
                case STATEMENT -> NodeParser.parseStatement(outputStr);
                case EXPRESSION -> NodeParser.parseExpression(outputStr);
                case IMPORT -> NodeParser.parseImportDeclaration(outputStr);
            };
            return parsedNode.apply(treeModifier).toSourceCode().strip();
        }
    }

    public enum SourceKind {
        DECLARATION,
        STATEMENT,
        EXPRESSION,
        IMPORT
    }
}
