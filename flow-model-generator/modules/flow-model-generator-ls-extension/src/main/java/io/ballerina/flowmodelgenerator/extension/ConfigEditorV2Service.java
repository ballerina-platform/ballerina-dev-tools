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
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.flowmodelgenerator.core.ConfigVariablesManager;
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariablesGetRequest;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariablesUpdateRequest;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariablesResponse;
import io.ballerina.flowmodelgenerator.extension.response.ConfigVariablesUpdateResponse;
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
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("configEditorV2")
public class ConfigEditorV2Service implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;
    private Gson gson;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
        this.gson = new GsonBuilder().serializeNulls().create();
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
    public CompletableFuture<ConfigVariablesResponse> getConfigVariables(ConfigVariablesGetRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariablesResponse response = new ConfigVariablesResponse();
            // Need to preserve the insertion order (default package first)
            Map<String, Map<String, List<FlowNode>>> configVarMap = new LinkedHashMap<>();
            try {
                Project project = workspaceManager.loadProject(Path.of(req.projectPath()));
                Package currentPackage = project.currentPackage();

                // Parse Config.toml if it exists
                Map<String, Object> configTomlValues = parseConfigToml(project);

                configVarMap.putAll(extractVariablesFromProject(currentPackage, configTomlValues));
                configVarMap.putAll(extractConfigsFromDependencies(currentPackage, configTomlValues));

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
    public CompletableFuture<ConfigVariablesUpdateResponse> updateConfigVariables(ConfigVariablesUpdateRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariablesUpdateResponse response = new ConfigVariablesUpdateResponse();
            try {
                FlowNode configVariable = gson.fromJson(req.configVariable(), FlowNode.class);
                Path configFilePath = Path.of(req.configFilePath());
                Path variableFilePath = findVariableFilePath(configVariable, configFilePath);

                if (variableFilePath == null) {
                    return response;
                }

                Optional<Document> document = workspaceManager.document(variableFilePath);
                if (document.isEmpty()) {
                    return response;
                }

                ConfigVariablesManager configManager = new ConfigVariablesManager();
                JsonElement textEdits = configManager.update(document.get(), variableFilePath, configVariable);
                response.setTextEdits(textEdits);
            } catch (Throwable e) {
                response.setError(e);
            }

            return response;
        });
    }

    /**
     * Parses the Config.toml file and returns the configuration values.
     *
     * @param project The current project
     * @return A map containing configuration values from Config.toml
     */
    private Map<String, Object> parseConfigToml(Project project) {
        Map<String, Object> configValues = new HashMap<>();

        try {
            Path configTomlPath = project.sourceRoot().resolve("Config.toml");
            if (!Files.exists(configTomlPath)) {
                return configValues;
            }

            Toml toml = Toml.read(configTomlPath);
            configValues = toml.toMap();
        } catch (Exception e) {
            // TODO: add diagnostic handling
        }

        return configValues;
    }

    /**
     * Gets the configuration value for a variable from Config.toml.
     *
     * @param configValues The parsed Config.toml values
     * @param packageName  The package name
     * @param moduleName   The module name
     * @param variableName The variable name
     * @return The configuration value if found, null otherwise
     */
    private Object getConfigValue(Map<String, Object> configValues, String packageName,
                                  String moduleName, String variableName) {
        if (configValues.isEmpty()) {
            return null;
        }

        // Try different key formats based on Ballerina configuration conventions
        // TODO: Remove redundant candidate keys
        String pkgName = packageName.replace("/", ".");
        String[] possibleKeys = {
                variableName,
                String.join(".", moduleName, variableName),
                String.join(".", pkgName, variableName),
                String.join(".", pkgName, moduleName, variableName),
        };

        for (String key : possibleKeys) {
            if (key != null && configValues.containsKey(key)) {
                return configValues.get(key);
            }
        }

        // Handle nested configurations
        return getNestedConfigValue(configValues, packageName, moduleName, variableName);
    }

    /**
     * Handles nested configuration values in Config.toml.
     *
     * @param configValues The parsed Config.toml values
     * @param packageName  The package name
     * @param moduleName   The module name
     * @param variableName The variable name
     * @return The nested configuration value if found, null otherwise
     */
    @SuppressWarnings("unchecked")
    private Object getNestedConfigValue(Map<String, Object> configValues, String packageName,
                                        String moduleName, String variableName) {
        try {
            // Check if there's a package-level section
            if (packageName != null && configValues.containsKey(packageName)) {
                Object packageSection = configValues.get(packageName);
                if (packageSection instanceof Map) {
                    Map<String, Object> packageMap = (Map<String, Object>) packageSection;

                    // Check module-level configuration
                    if (moduleName != null && !moduleName.isEmpty() && packageMap.containsKey(moduleName)) {
                        Object moduleSection = packageMap.get(moduleName);
                        if (moduleSection instanceof Map) {
                            Map<String, Object> moduleMap = (Map<String, Object>) moduleSection;
                            if (moduleMap.containsKey(variableName)) {
                                return moduleMap.get(variableName);
                            }
                        }
                    }

                    // Check package-level variable
                    if (packageMap.containsKey(variableName)) {
                        return packageMap.get(variableName);
                    }
                }
            }

            // Check if there's a module-level section at root
            if (moduleName != null && !moduleName.isEmpty() && configValues.containsKey(moduleName)) {
                Object moduleSection = configValues.get(moduleName);
                if (moduleSection instanceof Map) {
                    Map<String, Object> moduleMap = (Map<String, Object>) moduleSection;
                    if (moduleMap.containsKey(variableName)) {
                        return moduleMap.get(variableName);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore casting or other errors and return null
        }

        return null;
    }

    /**
     * Extracts configuration variables from the current package and its submodules.
     *
     * @param currentPackage   The current package instance
     * @param configTomlValues The parsed Config.toml values
     * @return A map containing configuration variables organized by package and module
     */
    private Map<String, Map<String, List<FlowNode>>> extractVariablesFromProject(
            Package currentPackage, Map<String, Object> configTomlValues) {
        Map<String, List<FlowNode>> moduleConfigVarMap = new HashMap<>();
        String pkgName = currentPackage.packageOrg().value() + "/" + currentPackage.packageName().value();

        for (Module module : currentPackage.modules()) {
            String modName = module.moduleName().moduleNamePart() != null ?
                    module.moduleName().moduleNamePart() : "";
            List<FlowNode> variables = extractModuleConfigVariables(module, configTomlValues, pkgName, modName);
            moduleConfigVarMap.put(modName, variables);
        }

        Map<String, Map<String, List<FlowNode>>> configVarMap = new LinkedHashMap<>();
        configVarMap.put(pkgName, moduleConfigVarMap);
        return configVarMap;
    }

    private Path findVariableFilePath(FlowNode configVariable, Path configFilePath)
            throws WorkspaceDocumentException, EventSyncException {
        if (isNew(configVariable)) {
            workspaceManager.loadProject(configFilePath);
            return configFilePath;
        }

        String variableFileName = configVariable.codedata().lineRange().fileName();
        Project project = workspaceManager.loadProject(configFilePath);

        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            return project.sourceRoot();
        }

        for (Module module : project.currentPackage().modules()) {
            for (DocumentId documentId : module.documentIds()) {
                Document document = module.document(documentId);
                if (document.name().equals(variableFileName)) {
                    return project.sourceRoot().resolve(document.syntaxTree().filePath());
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
     *
     * @param module           The module to extract configuration variables from
     * @param configTomlValues The parsed Config.toml values
     * @param packageName      The package name
     * @param moduleName       The module name
     * @return A list of configuration variables
     */
    private List<FlowNode> extractModuleConfigVariables(Module module, Map<String, Object> configTomlValues,
                                                        String packageName, String moduleName) {
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
                                configTomlValues, packageName, moduleName);
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
            Package currentPackage, Map<String, Object> configTomlValues) {
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
     *
     * @param currentPkg         Current package instance
     * @param moduleDependencies Used dependencies of the package
     * @param configTomlValues   The parsed Config.toml values
     * @return Map of configurable variables organized by module details
     */
    private Map<String, Map<String, List<FlowNode>>> getImportedConfigVars(
            Package currentPkg, Collection<ModuleDependency> moduleDependencies,
            Map<String, Object> configTomlValues) {
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
                                                          Map<String, Object> configTomlValues) {
        Map<String, List<FlowNode>> moduleConfigs = new HashMap<>();
        String packageName = dependency.packageInstance().packageOrg().value() + "/" +
                dependency.packageInstance().packageName().value();

        for (Module module : dependency.packageInstance().modules()) {
            String moduleName = module.moduleName().moduleNamePart() != null ?
                    module.moduleName().moduleNamePart() : "";
            List<FlowNode> variables = extractModuleConfigVariables(module, configTomlValues, packageName, moduleName);
            if (!variables.isEmpty()) {
                moduleConfigs.put(moduleName, variables);
            }
        }

        return moduleConfigs;
    }

    /**
     * Get all the valid dependencies for the package.
     *
     * @param packageInstance Package instance
     * @param module          module instance
     * @param dependencies    Collection of module dependencies
     */
    private static void populateValidDependencies(Package packageInstance, Module module,
                                                  Collection<ModuleDependency> dependencies) {
        for (ModuleDependency moduleDependency : module.moduleDependencies()) {
            if (!isDefaultScope(moduleDependency)) {
                continue;
            }
            dependencies.add(moduleDependency);

            if (!isSamePackage(packageInstance, moduleDependency)) {
                continue;
            }

            for (Module mod : packageInstance.modules()) {
                String moduleName = mod.descriptor().name().moduleNamePart();
                if (moduleName != null && moduleName.equals(moduleDependency.descriptor().name().moduleNamePart())) {
                    populateValidDependencies(packageInstance, mod, dependencies);
                }
            }
        }
    }

    /**
     * Check if the dependency has the default scope.
     *
     * @param moduleDependency Module dependency
     * @return boolean value indicating whether the dependency has default scope or not
     */
    private static boolean isDefaultScope(ModuleDependency moduleDependency) {
        return moduleDependency.packageDependency().scope().getValue().equals(
                PlatformLibraryScope.DEFAULT.getStringValue());
    }

    /**
     * Check if the dependency is From the same package.
     *
     * @param packageInstance  package instance
     * @param moduleDependency Module dependency
     * @return boolean value indicating whether the dependency is from the same package or not
     */
    private static boolean isSamePackage(Package packageInstance, ModuleDependency moduleDependency) {
        String orgValue = moduleDependency.descriptor().org().value();
        String packageVal = moduleDependency.descriptor().packageName().value();
        return orgValue.equals(packageInstance.packageOrg().value()) &&
                packageVal.equals(packageInstance.packageName().value());
    }

    /**
     * Check if a given resolved package dependency is a direct dependency of the package.
     *
     * @param dep                Resolved package dependency to check against
     * @param moduleDependencies Collection of module dependencies
     * @return boolean value indicating whether the module dependency is a direct dependency or not
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
     *
     * @param variableNode     The variable declaration node
     * @param semanticModel    The semantic model
     * @param configTomlValues The parsed Config.toml values
     * @param packageName      The package name
     * @param moduleName       The module name
     * @return A FlowNode representing the configuration variable
     */
    private FlowNode constructConfigVarNode(ModuleVariableDeclarationNode variableNode,
                                            SemanticModel semanticModel, Map<String, Object> configTomlValues,
                                            String packageName, String moduleName) {

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

        // Get the configuration value from Config.toml
        Object configTomlValue = getConfigValue(configTomlValues, packageName, moduleName, variableName);
        ExpressionNode configValueExpr = null;
        if (configTomlValue != null) {
            configValueExpr = NodeParser.parseExpression(configTomlValue.toString());
        }

        return nodeBuilder
                .metadata()
                .label("Config variables")
                .stepOut()
                .codedata()
                .node(NodeKind.CONFIG_VARIABLE)
                .lineRange(variableNode.lineRange())
                .stepOut()
                .properties()
                .type(typedBindingPattern.typeDescriptor(), true)
                .variableName(variableName)
                .defaultValue(variableNode.initializer().orElse(null))
                .configValue(configValueExpr)
                .stepOut()
                .build();
    }
}
