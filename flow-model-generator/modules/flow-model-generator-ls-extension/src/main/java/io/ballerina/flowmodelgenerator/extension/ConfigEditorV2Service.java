/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariableGetRequest;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariableNodeTemplateRequest;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariableUpdateRequest;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariableDeleteResponse;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariableNodeTemplateResponse;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariableUpdateResponse;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariablesResponse;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleDependency;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PlatformLibraryScope;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.toml.api.Toml;
import io.ballerina.toml.semantic.TomlType;
import io.ballerina.toml.semantic.ast.TomlArrayValueNode;
import io.ballerina.toml.semantic.ast.TomlKeyValueNode;
import io.ballerina.toml.semantic.ast.TomlNode;
import io.ballerina.toml.semantic.ast.TomlTableNode;
import io.ballerina.toml.semantic.ast.TomlValueNode;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.ballerina.flowmodelgenerator.core.model.Property.CONFIG_VALUE_KEY;
import static io.ballerina.flowmodelgenerator.core.model.Property.CONFIG_VAR_DOC_KEY;
import static io.ballerina.flowmodelgenerator.core.model.Property.DEFAULT_VALUE_KEY;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("configEditorV2")
public class ConfigEditorV2Service implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;
    private Gson gson;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    /**
     * Retrieves configuration variables from the Ballerina project.
     *
     * @param req The req containing project path
     * @return A future with configuration variables response
     */
    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariablesResponse> getConfigVariables(ConfigVariableGetRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariablesResponse response = new ConfigVariablesResponse();
            // Need to preserve the insertion order (default package first)
            Map<String, Map<String, List<FlowNode>>> configVarMap = new LinkedHashMap<>();
            try {
                Project project = workspaceManager.loadProject(Path.of(req.projectPath()));
                Package rootPackage = project.currentPackage();

                // Parse Config.toml if it exists
                Toml configTomlValues = parseConfigToml(project);

                configVarMap.putAll(extractVariablesFromProject(rootPackage, configTomlValues));
                if (req.includeLibraries()) {
                    configVarMap.putAll(extractConfigsFromDependencies(rootPackage, configTomlValues));
                }

                response.setConfigVariables(gson.toJsonTree(configVarMap));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    /**
     * Update a given config variable with the provided value.
     *
     * @param req The req containing config variable and file path
     * @return A future with update response containing text edits
     */
    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariableUpdateResponse> updateConfigVariable(ConfigVariableUpdateRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariableUpdateResponse response = new ConfigVariableUpdateResponse();
            try {
                FlowNode configVariable = gson.fromJson(req.configVariable(), FlowNode.class);
                Path configFilePath = Path.of(req.configFilePath());
                Project rootProject = workspaceManager.loadProject(configFilePath);

                Map<Path, List<TextEdit>> allTextEdits = new HashMap<>();
                // text edits to Ballerina source files
                if (isPackageInRootProject(req.packageName(), rootProject)) {
                    allTextEdits.putAll(constructSourceTextEdits(rootProject, configFilePath, configVariable, false));
                }
                // text edits to Config.toml
                Path configTomlPath = rootProject.sourceRoot().resolve("Config.toml");
                allTextEdits.putAll(constructConfigTomlTextEdits(rootProject, req.packageName(), req.moduleName(),
                        configVariable, configTomlPath, false));

                response.setTextEdits(gson.toJsonTree(allTextEdits));
            } catch (Throwable e) {
                response.setError(e);
            }

            return response;
        });
    }

    /**
     * Update a given config variable with the provided value.
     *
     * @param req The req containing config variable and file path
     * @return A future with update response containing text edits
     */
    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariableDeleteResponse> deleteConfigVariable(ConfigVariableUpdateRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariableDeleteResponse response = new ConfigVariableDeleteResponse();
            try {
                FlowNode configVariable = gson.fromJson(req.configVariable(), FlowNode.class);
                Path configFilePath = Path.of(req.configFilePath());
                Project rootProject = workspaceManager.loadProject(configFilePath);

                Map<Path, List<TextEdit>> allTextEdits = new HashMap<>();
                allTextEdits.putAll(constructSourceTextEdits(rootProject, configFilePath, configVariable, true));

                // text edits to Config.toml
                Path configTomlPath = rootProject.sourceRoot().resolve("Config.toml");
                allTextEdits.putAll(constructConfigTomlTextEdits(rootProject, req.packageName(), req.moduleName(),
                        configVariable, configTomlPath, true));

                response.setTextEdits(gson.toJsonTree(allTextEdits));
            } catch (Throwable e) {
                response.setError(e);
            }

            return response;
        });
    }

    /**
     * Retrieves the node template for configurable variables.
     *
     * @return A future with the node template response
     */
    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariableNodeTemplateResponse> getNodeTemplate(ConfigVariableNodeTemplateRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariableNodeTemplateResponse response = new ConfigVariableNodeTemplateResponse();
            try {
                FlowNode flowNode = getConfigVariableFlowNodeTemplate(request.isNew());
                JsonElement nodeTemplate = gson.toJsonTree(flowNode);
                response.setFlowNode(nodeTemplate);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    private Map<Path, List<TextEdit>> constructSourceTextEdits(Project rootProject, Path configTomlPath,
                                                               FlowNode variable, boolean isDelete) {
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        try {
            Path variableFilePath = findVariableFilePath(variable, configTomlPath, rootProject);
            if (variableFilePath == null) {
                return textEditsMap;
            }

            Optional<Document> document = workspaceManager.document(variableFilePath);
            if (document.isEmpty()) {
                return textEditsMap;
            }
            LineRange lineRange = variable.codedata().lineRange();
            String configStatement = constructConfigStatement(variable);

            List<TextEdit> textEdits = new ArrayList<>();
            if (isNew(variable) || lineRange == null) {
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                LinePosition startPos = LinePosition.from(modulePartNode.lineRange().endLine().line() + 1, 0);
                textEdits.add(new TextEdit(CommonUtils.toRange(startPos), configStatement));
            } else if (isDelete) {
                textEdits.add(new TextEdit(CommonUtils.toRange(lineRange), ""));
            } else {
                textEdits.add(new TextEdit(CommonUtils.toRange(lineRange), configStatement));
            }

            textEditsMap.put(configTomlPath, textEdits);
            return textEditsMap;
        } catch (Exception e) {
            return textEditsMap;
        }
    }

    private static String constructConfigStatement(FlowNode node) {
        String defaultValue = node.properties().get(DEFAULT_VALUE_KEY).toSourceCode();
        String variableDoc = node.properties().get(CONFIG_VAR_DOC_KEY).toSourceCode();
        List<String> docLines = Arrays.stream(variableDoc.split(System.lineSeparator())).toList();

        StringBuilder configStatementBuilder = new StringBuilder();
        docLines.forEach(docLine -> {
                    if (!docLine.isBlank()) {
                        configStatementBuilder.append("# ").append(docLine).append(System.lineSeparator());
                    }
                }
        );

        configStatementBuilder.append(String.format("configurable %s %s = %s;",
                node.properties().get(Property.TYPE_KEY).toSourceCode(),
                node.properties().get(Property.VARIABLE_KEY).toSourceCode(),
                defaultValue.isEmpty() ? "?" : defaultValue)
        );

        return configStatementBuilder.toString();
    }

    /**
     * Parses the Config.toml file and returns the configuration values.
     *
     * @param project The current project
     * @return A map containing configuration values from Config.toml
     */
    private Toml parseConfigToml(Project project) {
        try {
            Path configTomlPath = project.sourceRoot().resolve("Config.toml");
            if (!Files.exists(configTomlPath)) {
                return null;
            }

            return Toml.read(configTomlPath);
        } catch (Exception e) {
            // TODO: add diagnostic handling
        }

        return null;
    }

    /**
     * Gets the configuration value for a variable from Config.toml.
     */
    private Optional<TomlNode> getConfigValue(Toml configValues, String packageName,
                                              String moduleName, String variableName, boolean isRootProject) {
        if (configValues == null) {
            return Optional.empty();
        }

        String pkgName = packageName.replace("/", ".");
        String tomlPkgEntryKey = moduleName.isEmpty() ? pkgName : String.format("%s.%s", pkgName, moduleName);

        // 1. try to access config values stored with the package name
        Optional<Toml> moduleConfigValues = configValues.getTable(tomlPkgEntryKey);
        if (moduleConfigValues.isPresent()) {
            Optional<TomlValueNode> variableValueNode = moduleConfigValues.get().get(variableName);
            if (variableValueNode.isPresent()) {
                return Optional.of(variableValueNode.get());
            }

            Optional<Toml> variableValueTable = moduleConfigValues.get().getTable(variableName);
            if (variableValueTable.isPresent()) {
                return Optional.ofNullable(variableValueTable.get().rootNode());
            }
        }

        // 2. if the module belongs to the root package, try to access directly as config values can be stored
        // without the package name
        if (isRootProject) {
            if (moduleName.isEmpty()) {
                Optional<TomlValueNode> variableValueNode = configValues.get(variableName);
                if (variableValueNode.isPresent()) {
                    return Optional.of(variableValueNode.get());
                }

                Optional<Toml> variableValueTable = configValues.getTable(variableName);
                if (variableValueTable.isPresent()) {
                    return Optional.ofNullable(variableValueTable.get().rootNode());
                }
            } else {
                moduleConfigValues = configValues.getTable(moduleName);
                if (moduleConfigValues.isPresent()) {
                    Optional<TomlValueNode> variableValueNode = moduleConfigValues.get().get(variableName);
                    if (variableValueNode.isPresent()) {
                        return Optional.of(variableValueNode.get());
                    }

                    Optional<Toml> variableValueTable = moduleConfigValues.get().getTable(variableName);
                    if (variableValueTable.isPresent()) {
                        return Optional.ofNullable(variableValueTable.get().rootNode());
                    }
                }
            }
        }

        // 3. if the variable is not found, try to access it with a dotted notation
        String dottedVariableName = String.format("%s.%s", tomlPkgEntryKey, variableName);
        Optional<TomlValueNode> variableValueNode = configValues.get(dottedVariableName);
        if (variableValueNode.isPresent()) {
            return Optional.of(variableValueNode.get());
        }

        return Optional.empty();
    }

    private String getAsString(TomlNode tomlValueNode) {
        switch (tomlValueNode.kind()) {
            case TABLE -> {
                List<String> keyValuePairs = new LinkedList<>();
                ((TomlTableNode) tomlValueNode).entries().forEach((key, topLevelNode) -> {
                    if (topLevelNode.kind() == TomlType.KEY_VALUE) {
                        TomlKeyValueNode keyValueNode = (TomlKeyValueNode) topLevelNode;
                        keyValuePairs.add(key + ": " + getAsString(keyValueNode.value()));
                    }
                });
                return "{" + String.join(", ", keyValuePairs) + "}";
            }
            case INTEGER, DOUBLE, BOOLEAN -> {
                return tomlValueNode.toString();
            }
            case STRING -> {
                return "\"" + tomlValueNode + "\"";
            }
            case ARRAY -> {
                List<TomlValueNode> elements = ((TomlArrayValueNode) tomlValueNode).elements();
                List<String> elementValues = elements.stream().map(this::getAsString).toList();
                return "[" + String.join(", ", elementValues) + "]";
            }
            case TABLE_ARRAY, INLINE_TABLE, UNQUOTED_KEY, KEY_VALUE, NONE -> {
                // TODO: Handle these cases if needed
            }
            default -> {
                return null;
            }
        }

        return null;
    }

    /**
     * Extracts configuration variables from the current package and its submodules.
     */
    private Map<String, Map<String, List<FlowNode>>> extractVariablesFromProject(
            Package rootPackage, Toml configTomlValues) {
        Map<String, List<FlowNode>> moduleConfigVarMap = new HashMap<>();
        String pkgName = rootPackage.packageOrg().value() + "/" + rootPackage.packageName().value();

        for (Module module : rootPackage.modules()) {
            String modName = module.moduleName().moduleNamePart() != null ?
                    module.moduleName().moduleNamePart() : "";
            List<FlowNode> variables = extractModuleConfigVariables(module, configTomlValues, pkgName, modName, true);
            moduleConfigVarMap.put(modName, variables);
        }

        Map<String, Map<String, List<FlowNode>>> configVarMap = new LinkedHashMap<>();
        configVarMap.put(pkgName, moduleConfigVarMap);
        return configVarMap;
    }

    private Path findVariableFilePath(FlowNode configVariable, Path configFilePath, Project rootProject) {
        if (isNew(configVariable)) {
            return configFilePath;
        }

        if (rootProject.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            return rootProject.sourceRoot();
        }
        String variableFileName = configVariable.codedata().lineRange().fileName();

        for (Module module : rootProject.currentPackage().modules()) {
            for (DocumentId documentId : module.documentIds()) {
                Document document = module.document(documentId);
                if (document.name().equals(variableFileName)) {
                    return rootProject.sourceRoot().resolve(document.syntaxTree().filePath());
                }
            }
        }

        return null;
    }

    private boolean isNew(FlowNode configVariable) {
        return configVariable.codedata().isNew() != null && configVariable.codedata().isNew();
    }

    /**
     * Extracts configuration variables from the given module.
     */
    private List<FlowNode> extractModuleConfigVariables(Module module, Toml configTomlValues,
                                                        String packageName, String moduleName, boolean isRootProject) {
        List<FlowNode> configVariables = new LinkedList<>();
        Optional<SemanticModel> semanticModel = getSemanticModel(module);

        for (DocumentId documentId : module.documentIds()) {
            Document document = module.document(documentId);
            ModulePartNode modulePartNode = document.syntaxTree().rootNode();
            for (Node node : modulePartNode.children()) {
                if (node.kind() == SyntaxKind.MODULE_VAR_DECL) {
                    ModuleVariableDeclarationNode varDeclarationNode = (ModuleVariableDeclarationNode) node;
                    if (hasConfigurableQualifier(varDeclarationNode)) {
                        FlowNode configVarNode = constructConfigVarNode(varDeclarationNode, semanticModel.orElse(null),
                                configTomlValues, packageName, moduleName, isRootProject);
                        configVariables.add(configVarNode);
                    }
                }
            }
        }

        return configVariables;
    }

    private static Optional<SemanticModel> getSemanticModel(Module module) {
        try {
            SemanticModel semanticModel = module.packageInstance().getCompilation().getSemanticModel(module.moduleId());
            return Optional.ofNullable(semanticModel);
        } catch (Exception e) {
            // getSemanticModel() can throw an Error if the module is an imported module without a semantic model.
            return Optional.empty();
        }
    }

    /**
     * Extracts configuration variables from the dependencies of the current package.
     */
    private Map<String, Map<String, List<FlowNode>>> extractConfigsFromDependencies(
            Package currentPackage, Toml configTomlValues) {
        Map<String, Map<String, List<FlowNode>>> dependencyConfigVarMap = new HashMap<>();
        for (Module module : currentPackage.modules()) {
            List<ModuleDependency> validDependencies = new LinkedList<>();
            populateValidDependencies(currentPackage, module, validDependencies);
            dependencyConfigVarMap.putAll(getImportedConfigVars(currentPackage, validDependencies, configTomlValues));
        }

        return dependencyConfigVarMap;
    }

    private static boolean hasConfigurableQualifier(ModuleVariableDeclarationNode node) {
        return node.qualifiers()
                .stream()
                .anyMatch(q -> q.text().equals(Qualifier.CONFIGURABLE.getValue()));
    }

    /**
     * Retrieve configurable variables for all the direct imports for a package.
     */
    private Map<String, Map<String, List<FlowNode>>> getImportedConfigVars(
            Package currentPkg, Collection<ModuleDependency> moduleDependencies,
            Toml configTomlValues) {
        Map<String, Map<String, List<FlowNode>>> pkgConfigs = new HashMap<>();
        Collection<ResolvedPackageDependency> dependencies = currentPkg.getResolution().dependencyGraph().getNodes();
        for (ResolvedPackageDependency dependency : dependencies) {
            if (!isDirectDependency(dependency, moduleDependencies)) {
                continue;
            }

            Map<String, List<FlowNode>> moduleConfigs = processDependency(dependency, configTomlValues);
            if (!moduleConfigs.isEmpty()) {
                String pkgKey = dependency.packageInstance().packageOrg().value() + "/" +
                        dependency.packageInstance().packageName().value();
                pkgConfigs.put(pkgKey, moduleConfigs);
            }
        }

        return pkgConfigs;
    }

    private Map<String, List<FlowNode>> processDependency(ResolvedPackageDependency dependency,
                                                          Toml configTomlValues) {
        Map<String, List<FlowNode>> moduleConfigs = new HashMap<>();
        String packageName = dependency.packageInstance().packageOrg().value() + "/" +
                dependency.packageInstance().packageName().value();

        for (Module module : dependency.packageInstance().modules()) {
            String moduleName = module.moduleName().moduleNamePart() != null ?
                    module.moduleName().moduleNamePart() : "";
            List<FlowNode> variables = extractModuleConfigVariables(module, configTomlValues, packageName, moduleName,
                    false);
            if (!variables.isEmpty()) {
                moduleConfigs.put(moduleName, variables);
            }
        }

        return moduleConfigs;
    }

    /**
     * Get all the valid dependencies for the package.
     */
    private static void populateValidDependencies(Package packageInstance, Module module,
                                                  Collection<ModuleDependency> dependencies) {
        for (ModuleDependency moduleDependency : module.moduleDependencies()) {
            if (!isDefaultScope(moduleDependency) || isSamePackage(packageInstance, moduleDependency)) {
                continue;
            }
            dependencies.add(moduleDependency);
        }
    }

    /**
     * Check if the dependency has the default scope.
     */
    private static boolean isDefaultScope(ModuleDependency moduleDependency) {
        return moduleDependency.packageDependency().scope().getValue().equals(
                PlatformLibraryScope.DEFAULT.getStringValue());
    }

    /**
     * Check if the dependency is From the same package.
     */
    private static boolean isSamePackage(Package packageInstance, ModuleDependency moduleDependency) {
        String orgValue = moduleDependency.descriptor().org().value();
        String packageVal = moduleDependency.descriptor().packageName().value();
        return orgValue.equals(packageInstance.packageOrg().value())
                && packageVal.equals(packageInstance.packageName().value());
    }

    /**
     * Check if a given resolved package dependency is a direct dependency of the package.
     */
    private static boolean isDirectDependency(ResolvedPackageDependency dep,
                                              Collection<ModuleDependency> moduleDependencies) {
        PackageDescriptor descriptor = dep.packageInstance().descriptor();
        String orgName = descriptor.org().value();
        String packageName = descriptor.name().value();

        for (ModuleDependency dependency : moduleDependencies) {
            if (dependency.descriptor().org().value().equals(orgName) &&
                    dependency.descriptor().packageName().value().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Constructs a FlowNode for a configuration variable with Config.toml value.
     */
    private FlowNode constructConfigVarNode(ModuleVariableDeclarationNode variableNode,
                                            SemanticModel semanticModel, Toml configTomlValues,
                                            String packageName, String moduleName, boolean isRootProject) {

        NodeBuilder nodeBuilder = NodeBuilder.getNodeFromKind(NodeKind.CONFIG_VARIABLE)
                .semanticModel(semanticModel)
                .defaultModuleName(null);

        if (semanticModel != null) {
            nodeBuilder.semanticModel(semanticModel);
            DiagnosticHandler diagnosticHandler = new DiagnosticHandler(semanticModel);
            diagnosticHandler.handle(nodeBuilder, variableNode.lineRange(), false);
            nodeBuilder.diagnosticHandler(diagnosticHandler);
        }

        TypedBindingPatternNode typedBindingPattern = variableNode.typedBindingPattern();
        String variableName = typedBindingPattern.bindingPattern().toSourceCode().trim();
        Optional<Node> markdownDocs = extractVariableDocs(variableNode);

        // Get the configuration value from Config.toml
        Optional<TomlNode> configTomlValue = getConfigValue(configTomlValues, packageName, moduleName, variableName,
                isRootProject);
        ExpressionNode configValueExpr = null;
        if (configTomlValue.isPresent()) {
            configValueExpr = NodeParser.parseExpression(getAsString(configTomlValue.get()));
        }

        return nodeBuilder
                .metadata()
                .stepOut()
                .codedata()
                .node(NodeKind.CONFIG_VARIABLE)
                .lineRange(variableNode.lineRange())
                .stepOut()
                .properties()
                .variableName(variableName, isRootProject)
                .type(typedBindingPattern.typeDescriptor(), isRootProject)
                .defaultValue(variableNode.initializer().orElse(null), isRootProject)
                .configValue(configValueExpr)
                .documentation(markdownDocs.orElse(null), isRootProject)
                .stepOut()
                .build();
    }

    /**
     * Extracts the markdown documentation from the variable node.
     */
    private static Optional<Node> extractVariableDocs(ModuleVariableDeclarationNode variableNode) {
        Optional<MetadataNode> metadata = variableNode.metadata();
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        return metadata.get().documentationString();
    }

    private static FlowNode getConfigVariableFlowNodeTemplate(boolean isNew) {
        NodeBuilder nodeBuilder = NodeBuilder.getNodeFromKind(NodeKind.CONFIG_VARIABLE)
                .defaultModuleName(null);
        return nodeBuilder
                .metadata()
                .stepOut()
                .codedata()
                .node(NodeKind.CONFIG_VARIABLE)
                .lineRange(null)
                .isNew(isNew)
                .stepOut()
                .properties()
                .type(null, true, false)
                .variableName(null, true, false)
                .defaultValue(null)
                .configValue(null)
                .documentation(null)
                .stepOut()
                .build();
    }

    /**
     * Constructs text edits for updating Config.toml file with the new config value.
     */
    private Map<Path, List<TextEdit>> constructConfigTomlTextEdits(Project project, String packageName,
                                                                   String moduleName, FlowNode configVariable,
                                                                   Path configTomlPath, boolean isDelete) {
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        try {
            if (!configVariable.properties().containsKey(CONFIG_VALUE_KEY)) {
                return textEditsMap;
            }
            Map<String, Property> properties = configVariable.properties();
            String variableName = properties.get(Property.VARIABLE_KEY).value().toString();
            String configValue = properties.get(CONFIG_VALUE_KEY).toSourceCode();

            Toml existingConfigToml = parseConfigToml(project);
            Optional<TomlNode> oldConfigValue = getConfigValue(existingConfigToml, packageName, moduleName,
                    variableName, isPackageInRootProject(packageName, project));
            if (isDelete && oldConfigValue.isEmpty()) {
                return textEditsMap;
            } else if (oldConfigValue.isEmpty() && configValue.isEmpty()) {
                return textEditsMap;
            }

            String orgName = packageName.split("/")[0];
            String pkgName = packageName.split("/")[1];
            String newContent = isDelete || configValue.isEmpty() ? "" : constructConfigTomlStatement(
                    orgName, pkgName, moduleName, variableName, configValue, oldConfigValue.isPresent());

            List<TextEdit> textEdits = new ArrayList<>();
            if (oldConfigValue.isPresent()) {
                String fileName = oldConfigValue.get().location().lineRange().fileName();
                LinePosition startPos = LinePosition.from(oldConfigValue.get().location().lineRange().startLine().line(), 0);
                LinePosition endPos = LinePosition.from(oldConfigValue.get().location().lineRange().endLine().line(),
                        oldConfigValue.get().location().lineRange().endLine().offset());
                LineRange lineRange = LineRange.from(fileName, startPos, endPos);
                textEdits.add(new TextEdit(CommonUtils.toRange(lineRange), newContent));
            } else {
                // if the variable is new, we need to find the relevant section in Config.toml file and add the new
                // entry after the last entry of the section.
                if (existingConfigToml != null) {
                    // Try to find existing section for the module
                    String sectionKey = moduleName.isEmpty() ? String.format("%s.%s", orgName, pkgName)
                            : String.format("%s.%s.%s", orgName, pkgName, moduleName);

                    Optional<Toml> moduleSection = existingConfigToml.getTable(sectionKey);
                    if (moduleSection.isPresent()) {
                        // Section exists - find the last entry and add after it
                        TomlTableNode moduleTableNode = moduleSection.get().rootNode();
                        if (!moduleTableNode.entries().isEmpty()) {
                            // Get the last entry in the section
                            TomlNode lastEntry = moduleTableNode.entries().values().stream()
                                    .reduce((first, second) -> second)
                                    .orElse(null);

                            if (lastEntry != null) {
                                LinePosition insertPos = LinePosition.from(
                                        lastEntry.location().lineRange().endLine().line() + 1, 0);
                                textEdits.add(new TextEdit(CommonUtils.toRange(insertPos),
                                        String.format("%s = %s%n", variableName, configValue)));
                            }
                        } else {
                            // Section exists but is empty - add right after section header
                            LinePosition insertPos = LinePosition.from(
                                    moduleTableNode.location().lineRange().endLine().line() + 1, 0);
                            textEdits.add(new TextEdit(CommonUtils.toRange(insertPos),
                                    String.format("%s = %s%n", variableName, configValue)));
                        }
                    } else {
                        // Section doesn't exist - append to end of file
                        TomlTableNode rootNode = existingConfigToml.rootNode();
                        LinePosition insertPos;

                        if (!rootNode.entries().isEmpty()) {
                            // Find the last entry in the root table
                            TomlNode lastEntry = rootNode.entries().values().stream()
                                    .reduce((first, second) -> second)
                                    .orElse(null);
                            insertPos = LinePosition.from(lastEntry.location().lineRange().endLine().line() + 1, 0);
                        } else {
                            // Empty config file
                            insertPos = LinePosition.from(0, 0);
                        }

                        textEdits.add(new TextEdit(CommonUtils.toRange(insertPos),
                                String.format("%n%s%n", newContent)));
                    }
                } else {
                    // Config.toml doesn't exist - create new file with the content
                    if (!Files.exists(configTomlPath)) {
                        try {
                            Files.createFile(configTomlPath);
                        } catch (Exception createEx) {
                            // Handle file creation error
                            return textEditsMap;
                        }
                    }
                    LinePosition startPos = LinePosition.from(0, 0);
                    textEdits.add(new TextEdit(CommonUtils.toRange(startPos), newContent + System.lineSeparator()));
                }
            }

            textEditsMap.put(configTomlPath, textEdits);
            return textEditsMap;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Check if the package is the root project package.
     */
    private static boolean isPackageInRootProject(String packageName, Project rootProject) {
        String rootPackageName = rootProject.currentPackage().packageOrg().value() + "/" +
                rootProject.currentPackage().packageName().value();
        return packageName.equals(rootPackageName);
    }

    private static String constructConfigTomlStatement(String orgName, String packageName, String moduleName,
                                                       String variableName, String value, boolean moduleEntryExists) {
        ExpressionNode configValueExpr = NodeParser.parseExpression(value);
        String tomlValue = getInTomlSyntax(configValueExpr);
        if (moduleEntryExists) {
            return String.format("%s = %s", variableName, tomlValue);
        } else {
            if (moduleName.isEmpty()) {
                return String.format("[%s.%s]\n%s = %s", orgName, packageName, variableName, tomlValue);
            } else {
                return String.format("[%s.%s.%s]\n%s = %s", orgName, packageName, moduleName, variableName, tomlValue);
            }
        }
    }

    private static String getInTomlSyntax(ExpressionNode configValueExpr) {
        switch (configValueExpr.kind()) {
            case MAPPING_CONSTRUCTOR -> {
                MappingConstructorExpressionNode recordTypeDesc = (MappingConstructorExpressionNode) configValueExpr;
                StringBuilder sb = new StringBuilder("{");
                for (MappingFieldNode field : recordTypeDesc.fields()) {
                    if (field.kind() == SyntaxKind.SPECIFIC_FIELD) {
                        SpecificFieldNode mappingField = (SpecificFieldNode) field;
                        String key = mappingField.fieldName().toSourceCode();
                        String value = mappingField.valueExpr().isPresent() ?
                                getInTomlSyntax(mappingField.valueExpr().get()) : null;
                        if (value != null) {
                            sb.append(key).append(" = ").append(value).append(", ");
                        }
                    }
                }
                if (sb.length() > 1) {
                    sb.setLength(sb.length() - 2);
                }
                sb.append("}");
                return sb.toString();
            }
            case LIST_CONSTRUCTOR -> {
                ListConstructorExpressionNode arrayTypeDesc = (ListConstructorExpressionNode) configValueExpr;
                StringBuilder sb = new StringBuilder("[");
                List<String> memberValues = new LinkedList<>();
                for (Node element : arrayTypeDesc.expressions()) {
                    if (element instanceof ExpressionNode expressionNode) {
                        memberValues.add(getInTomlSyntax(expressionNode));
                    }
                }
                sb.append(String.join(", ", memberValues));
                sb.append("]");
                return sb.toString();
            }
            default -> {
                // TODO: Add support for other types if needed
                return configValueExpr.toSourceCode();
            }
        }
    }
}
