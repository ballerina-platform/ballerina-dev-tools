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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
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
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("configEditorV2")
public class ConfigEditorServiceV2 implements ExtendedLanguageServerService {

    private static final String BAL_FILE_EXTENSION = ".bal";

    private WorkspaceManager workspaceManager;
    private Gson gson;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
        this.gson = new Gson();
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    /**
     * Retrieves configuration variables from the Ballerina project.
     *
     * @param request The request containing project path
     * @return A future with configuration variables response
     */
    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariablesResponse> getConfigVariables(ConfigVariablesGetRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariablesResponse response = new ConfigVariablesResponse();
            Map<String, Map<String, List<FlowNode>>> configVarMap = new HashMap<>();
            try {
                Path projectPath = Path.of(request.projectPath());
                Project project = this.workspaceManager.loadProject(projectPath);
                Package currentPackage = project.currentPackage();
                Module defaultModule = currentPackage.getDefaultModule();

                // Add config variables from main project
                Map<String, List<FlowNode>> moduleConfigVarMap = new HashMap<>();
                for (Module module : currentPackage.modules()) {
                    moduleConfigVarMap.put(module.moduleName().moduleNamePart(), extractModuleConfigVariables(module));
                }
                String orgName = currentPackage.packageOrg().value();
                String pkgName = currentPackage.packageName().value();
                configVarMap.put(orgName + "/" + pkgName, moduleConfigVarMap);

                // Add config variables from dependencies
                configVarMap.putAll(extractConfigsFromDependencies(currentPackage));

                JsonElement jsonTree = new Gson().toJsonTree(configVarMap);
                response.setConfigVariables(jsonTree);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    /**
     * Update a given config variable with the provided value.
     *
     * @param request The request containing config variable and file path
     * @return A future with update response containing text edits
     */
    @JsonRequest
    @SuppressWarnings("unused")
    public CompletableFuture<ConfigVariablesUpdateResponse> updateConfigVariables(ConfigVariablesUpdateRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ConfigVariablesUpdateResponse response = new ConfigVariablesUpdateResponse();
            try {
                FlowNode configVariable = gson.fromJson(request.configVariable(), FlowNode.class);
                Path configFilePath = Path.of(request.configFilePath());
                Path variableFilePath = null;
                if (isNew(configVariable)) {
                    variableFilePath = configFilePath;
                    this.workspaceManager.loadProject(configFilePath);
                } else {
                    String variableFileName = configVariable.codedata().lineRange().fileName();
                    Project project = this.workspaceManager.loadProject(configFilePath);
                    if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
                        variableFilePath = project.sourceRoot();
                    } else {
                        for (Module module : project.currentPackage().modules()) {
                            for (DocumentId documentId : module.documentIds()) {
                                Document document = module.document(documentId);
                                if (document.name().equals(variableFileName)) {
                                    variableFilePath = project.sourceRoot().resolve(document.syntaxTree().filePath());
                                }
                            }
                        }
                    }
                }
                if (Objects.isNull(variableFilePath)) {
                    return response;
                }

                Optional<Document> document = this.workspaceManager.document(variableFilePath);
                if (document.isEmpty()) {
                    return response;
                }

                ConfigVariablesManager configVariablesManager = new ConfigVariablesManager();
                JsonElement textEdits = configVariablesManager.update(document.get(), variableFilePath, configVariable);
                response.setTextEdits(textEdits);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    private boolean isNew(FlowNode configVariable) {
        return configVariable.codedata().isNew() != null && configVariable.codedata().isNew();
    }

    /**
     * Extracts configuration variables from the given module.
     *
     * @param module The module to extract configuration variables from
     * @return A list of configuration variables
     */
    private static List<FlowNode> extractModuleConfigVariables(Module module) {
        LinkedList<FlowNode> configVariables = new LinkedList<>();
        SemanticModel semanticModel = module.getCompilation().getSemanticModel();
        for (DocumentId documentId : module.documentIds()) {
            Document document = module.document(documentId);
            if (document.name().endsWith(BAL_FILE_EXTENSION)) {
                ModulePartNode modulePartNode = document.syntaxTree().rootNode();
                for (Node node : modulePartNode.children()) {
                    if (node.kind() == SyntaxKind.MODULE_VAR_DECL) {
                        ModuleVariableDeclarationNode modVarDeclarationNode = (ModuleVariableDeclarationNode) node;
                        if (hasConfigurableQualifier(modVarDeclarationNode)) {
                            configVariables.add(constructConfigVarNode(modVarDeclarationNode, semanticModel));
                        }
                    }
                }
            }
        }

        return configVariables;
    }

    /**
     * Extracts configuration variables from the dependencies of the current package.
     */
    private static Map<String, Map<String, List<FlowNode>>> extractConfigsFromDependencies(Package currentPackage) {
        Map<String, Map<String, List<FlowNode>>> dependencyConfigVarMap = new HashMap<>();
        for (Module module : currentPackage.modules()) {
            LinkedList<ModuleDependency> validDependencies = new LinkedList<>();
            populateValidDependencies(currentPackage, module, validDependencies);
            Map<String, Map<String, List<FlowNode>>> importedConfigVars = getImportedConfigVars(currentPackage, validDependencies);
            dependencyConfigVarMap.putAll(importedConfigVars);
        }

        return dependencyConfigVarMap;
    }

    private static boolean hasConfigurableQualifier(ModuleVariableDeclarationNode modVarDeclarationNode) {
        return modVarDeclarationNode.qualifiers()
                .stream().anyMatch(q -> q.text().equals(Qualifier.CONFIGURABLE.getValue()));
    }

    /**
     * Retrieve configurable variables for all the direct imports for a package.
     *
     * @param currentPkg         Current package instance
     * @param moduleDependencies Used dependencies of the package
     * @return Map of configurable variables organized by module details
     */
    private static Map<String, Map<String, List<FlowNode>>> getImportedConfigVars(
            Package currentPkg, Collection<ModuleDependency> moduleDependencies) {
        Map<String, Map<String, List<FlowNode>>> pkgConfigs = new HashMap<>();
        Collection<ResolvedPackageDependency> dependencies = currentPkg.getResolution().dependencyGraph().getNodes();
        for (ResolvedPackageDependency dependency : dependencies) {
            if (isDirectDependency(dependency, moduleDependencies)) {
                Map<String, List<FlowNode>> moduleConfigs = processDependency(dependency);
                String org = dependency.packageInstance().packageOrg().value();
                String name = dependency.packageInstance().packageName().value();
                pkgConfigs.put(org + "/" + name, moduleConfigs);
            }
        }

        return pkgConfigs;
    }

    private static Map<String, List<FlowNode>> processDependency(ResolvedPackageDependency dependency) {
        Map<String, List<FlowNode>> moduleConfigs = new HashMap<>();
        for (Module module : dependency.packageInstance().modules()) {
            List<FlowNode> variables = extractModuleConfigVariables(module);
            String modName = module.moduleName().moduleNamePart() != null ? module.moduleName().moduleNamePart() : "";
            moduleConfigs.put(modName, variables);
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
        Collection<ModuleDependency> directDependencies = module.moduleDependencies();
        for (ModuleDependency moduleDependency : directDependencies) {
            if (!isDefaultScope(moduleDependency)) {
                continue;
            }
            dependencies.add(moduleDependency);

            // TODO: Verify logic
            if (isSamePackage(packageInstance, moduleDependency)) {
                for (Module mod : packageInstance.modules()) {
                    String modName = mod.descriptor().name().moduleNamePart();
                    if (modName != null && modName.equals(moduleDependency.descriptor().name().moduleNamePart())) {
                        populateValidDependencies(packageInstance, mod, dependencies);
                    }
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
            if (dependency.descriptor().org().value().equals(orgName)
                    && dependency.descriptor().packageName().value().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private static FlowNode constructConfigVarNode(ModuleVariableDeclarationNode variableNode,
                                                   SemanticModel semanticModel) {
        DiagnosticHandler diagnosticHandler = new DiagnosticHandler(semanticModel);
        NodeBuilder nodeBuilder = NodeBuilder.getNodeFromKind(NodeKind.CONFIG_VARIABLE)
                .semanticModel(semanticModel)
                .diagnosticHandler(diagnosticHandler)
                .defaultModuleName(null);
        diagnosticHandler.handle(nodeBuilder, variableNode.lineRange(), false);

        TypedBindingPatternNode typedBindingPattern = variableNode.typedBindingPattern();
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
                .variableName(typedBindingPattern.bindingPattern().toSourceCode().trim())
                .defaultValue(variableNode.initializer().orElse(null))
                .stepOut()
                .build();
    }
}
