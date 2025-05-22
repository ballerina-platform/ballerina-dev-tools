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
import io.ballerina.projects.PlatformLibraryScope;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.ballerinalang.model.elements.PackageID;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;
import org.wso2.ballerinalang.compiler.PackageCache;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangVariable;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.util.Flags;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            try {
                Map<String, Map<String, List<FlowNode>>> configVarMap = getAllConfigVariables(req.projectPath());
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
                Path variableFilePath = resolveVariableFilePath(configVariable, configFilePath);
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
     * Collects all config variables from a project and its dependencies.
     */
    private Map<String, Map<String, List<FlowNode>>> getAllConfigVariables(String projectPath) throws Exception {
        // Preserves insertion order (default package first)
        Map<String, Map<String, List<FlowNode>>> configVarMap = new LinkedHashMap<>();
        Project project = workspaceManager.loadProject(Path.of(projectPath));
        Package currentPackage = project.currentPackage();

        // Extract config variables from the main project
        Map<String, List<FlowNode>> moduleConfigVarMap = extractConfigVarsFromCurrentPackage(currentPackage);
        if (!moduleConfigVarMap.isEmpty()) {
            String pkgName = currentPackage.packageOrg().value() + "/" + currentPackage.packageName().value();
            configVarMap.put(pkgName, moduleConfigVarMap);
        }

        // Extract config variables from dependencies
        configVarMap.putAll(extractConfigsFromDependencies(currentPackage));

        return configVarMap;
    }

    /**
     * Extracts config variables from the current package's modules.
     */
    private Map<String, List<FlowNode>> extractConfigVarsFromCurrentPackage(Package currentPackage) {
        Map<String, List<FlowNode>> moduleConfigVarMap = new HashMap<>();
        for (Module module : currentPackage.modules()) {
            List<FlowNode> variables = extractModuleConfigVariables(module);
            if (!variables.isEmpty()) {
                String moduleName = module.moduleName().moduleNamePart() != null ?
                        module.moduleName().moduleNamePart() : "";
                moduleConfigVarMap.put(moduleName, variables);
            }
        }
        return moduleConfigVarMap;
    }

    /**
     * Resolves the path to the file containing the variable to be updated.
     */
    private Path resolveVariableFilePath(FlowNode configVariable, Path configFilePath)
            throws WorkspaceDocumentException, EventSyncException {
        if (isNewConfigVariable(configVariable)) {
            workspaceManager.loadProject(configFilePath);
            return configFilePath;
        }

        String variableFileName = configVariable.codedata().lineRange().fileName();
        Project project = workspaceManager.loadProject(configFilePath);

        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            return project.sourceRoot();
        }

        return findFilePathInModules(project, variableFileName);
    }

    private Path findFilePathInModules(Project project, String fileName) {
        for (Module module : project.currentPackage().modules()) {
            for (DocumentId documentId : module.documentIds()) {
                Document document = module.document(documentId);
                if (document.name().equals(fileName)) {
                    return project.sourceRoot().resolve(document.syntaxTree().filePath());
                }
            }
        }

        return null;
    }

    private boolean isNewConfigVariable(FlowNode configVariable) {
        return configVariable.codedata().isNew() != null && configVariable.codedata().isNew();
    }

    /**
     * Extracts configuration variables from the given module.
     */
    private static List<FlowNode> extractModuleConfigVariables(Module module) {
        List<FlowNode> configVariables = new LinkedList<>();
        // Note: Cannot use "module.getCompilation().semanticModel()" as it causes intermittent errors
        SemanticModel semanticModel = module.packageInstance().getCompilation().getSemanticModel(module.moduleId());

        for (DocumentId documentId : module.documentIds()) {
            Document document = module.document(documentId);
            ModulePartNode modulePartNode = document.syntaxTree().rootNode();
            for (Node node : modulePartNode.children()) {
                if (node.kind() == SyntaxKind.MODULE_VAR_DECL) {
                    ModuleVariableDeclarationNode varDeclarationNode = (ModuleVariableDeclarationNode) node;
                    if (hasConfigurableQualifier(varDeclarationNode)) {
                        configVariables.add(constructConfigVarNode(varDeclarationNode, semanticModel));
                    }
                }
            }
        }

        return configVariables;
    }

    private static boolean hasConfigurableQualifier(ModuleVariableDeclarationNode node) {
        return node.qualifiers()
                .stream()
                .anyMatch(q -> q.text().equals(Qualifier.CONFIGURABLE.getValue()));
    }

    /**
     * Extracts configuration variables from the dependencies of the current package.
     */
    private static Map<String, Map<String, List<FlowNode>>> extractConfigsFromDependencies(Package currentPackage) {
        Map<String, Map<String, List<FlowNode>>> dependencyConfigVarMap = new HashMap<>();
        for (Module module : currentPackage.modules()) {
            List<ModuleDependency> validDependencies = new LinkedList<>();
            populateValidDependencies(currentPackage, module, validDependencies);
            dependencyConfigVarMap.putAll(getImportedConfigVars(currentPackage, validDependencies));
        }

        return dependencyConfigVarMap;
    }

    /**
     * Collects all valid dependencies for the package.
     */
    private static void populateValidDependencies(Package packageInstance, Module module,
                                                  Collection<ModuleDependency> dependencies) {
        for (ModuleDependency moduleDependency : module.moduleDependencies()) {
            // Check if we should include this dependency
            if (!isDefaultScope(moduleDependency) || isWithinSamePackage(moduleDependency, packageInstance)) {
                continue;
            }
            dependencies.add(moduleDependency);

            for (Module mod : packageInstance.modules()) {
                String moduleName = mod.descriptor().name().moduleNamePart();
                if (moduleName != null && moduleName.equals(moduleDependency.descriptor().name().moduleNamePart())) {
                    populateValidDependencies(packageInstance, mod, dependencies);
                }
            }
        }
    }

    private static boolean isDefaultScope(ModuleDependency moduleDependency) {
        return moduleDependency.packageDependency().scope().getValue().equals(
                PlatformLibraryScope.DEFAULT.getStringValue());
    }

    private static boolean isWithinSamePackage(ModuleDependency moduleDependency, Package packageInstance) {
        String orgValue = moduleDependency.descriptor().org().value();
        String packageVal = moduleDependency.descriptor().packageName().value();
        return orgValue.equals(packageInstance.packageOrg().value())
                && packageVal.equals(packageInstance.packageName().value());
    }

    /**
     * Extracts config variables from imported modules.
     */
    private static Map<String, Map<String, List<FlowNode>>> getImportedConfigVars(
            Package currentPackage, Collection<ModuleDependency> validDependencies) {

        Map<String, Map<String, List<FlowNode>>> dependencyConfigMap = new HashMap<>();
        for (Module module : currentPackage.modules()) {
            BLangPackage bLangPackage = getBLangPackageForModule(module);
            if (bLangPackage == null) {
                continue;
            }

            bLangPackage.symbol.imports.forEach(
                    importSymbol -> {
                        if (isDirectDependency(validDependencies, importSymbol)) {
                            processImportSymbol(module, importSymbol, dependencyConfigMap, validDependencies);
                        }
                    }
            );
        }

        return dependencyConfigMap;
    }

    private static BLangPackage getBLangPackageForModule(Module module) {
        CompilerContext compilerContext = module.project()
                .projectEnvironmentContext()
                .getService(CompilerContext.class);

        PackageCache packageCache = PackageCache.getInstance(compilerContext);
        PackageID packageID = new PackageID(
                Names.fromString(module.descriptor().org().value()),
                Names.fromString(module.descriptor().packageName().value()),
                Names.fromString(module.descriptor().version().value().toString())
        );

        return packageCache.get(packageID);
    }

    private static boolean isDirectDependency(Collection<ModuleDependency> moduleDependencies,
                                              BPackageSymbol importSymbol) {
        String orgName = importSymbol.descriptor.org().value();
        String packageName = importSymbol.descriptor.packageName().value();
        String moduleName = importSymbol.descriptor.name().moduleNamePart();

        return moduleDependencies.stream().anyMatch(dependency ->
                dependency.descriptor().org().value().equals(orgName)
                        && dependency.descriptor().packageName().value().equals(packageName)
                        && (moduleName == null ? dependency.descriptor().name().moduleNamePart() == null
                        : moduleName.equals(dependency.descriptor().name().moduleNamePart()))
        );
    }

    /**
     * Process an import symbol to extract configuration variables.
     */
    private static void processImportSymbol(Module module, BPackageSymbol importSymbol,
                                            Map<String, Map<String, List<FlowNode>>> configDetails,
                                            Collection<ModuleDependency> moduleDependencies) {
        String orgName = importSymbol.descriptor.org().value();
        String packageName = importSymbol.descriptor.packageName().value();
        String moduleNamePart = importSymbol.descriptor.name().moduleNamePart();
        String moduleName = moduleNamePart == null ? "" : moduleNamePart;

        // Find matching dependency
        Optional<ModuleDependency> matchingDependency = findMatchingDependency(
                moduleDependencies, orgName, packageName, moduleNamePart);

        if (matchingDependency.isEmpty()) {
            return;
        }

        // Get BLang package and extract configurable variables
        BLangPackage bLangPackage = getBLangPackageForDependency(module, matchingDependency.get());
        if (bLangPackage == null) {
            return;
        }

        // Extract configurable variables from the package
        List<FlowNode> configVariables = extractConfigVariablesFromBLangPackage(bLangPackage);

        if (!configVariables.isEmpty()) {
            Map<String, List<FlowNode>> moduleConfigMap = new HashMap<>();
            moduleConfigMap.put(moduleName, configVariables);
            configDetails.put(orgName + "/" + packageName, moduleConfigMap);
        }
    }

    private static Optional<ModuleDependency> findMatchingDependency(
            Collection<ModuleDependency> dependencies, String orgName, String packageName, String moduleNamePart) {

        return dependencies.stream()
                .filter(dependency ->
                        dependency.descriptor().org().value().equals(orgName)
                                && dependency.descriptor().packageName().value().equals(packageName)
                                && Objects.equals(moduleNamePart, dependency.descriptor().name().moduleNamePart()))
                .findFirst();
    }

    private static BLangPackage getBLangPackageForDependency(Module module, ModuleDependency dependency) {
        CompilerContext compilerContext = module.project().projectEnvironmentContext()
                .getService(CompilerContext.class);
        PackageCache packageCache = PackageCache.getInstance(compilerContext);
        PackageID packageID = new PackageID(
                Names.fromString(dependency.descriptor().org().value()),
                Names.fromString(dependency.descriptor().packageName().value()),
                Names.fromString(dependency.descriptor().version().value().toString())
        );
        return packageCache.get(packageID);
    }

    private static List<FlowNode> extractConfigVariablesFromBLangPackage(BLangPackage bLangPackage) {
        List<FlowNode> configVariables = new ArrayList<>();
        bLangPackage.getGlobalVariables().forEach(globalVar -> {
            getConfigFromBVar(globalVar).ifPresent(configVariables::add);
        });
        return configVariables;
    }

    private static Optional<FlowNode> getConfigFromBVar(BLangVariable globalVar) {
        BVarSymbol symbol = globalVar.symbol;
        if (symbol == null || symbol.tag != SymTag.VARIABLE || !Symbols.isFlagOn(symbol.flags, Flags.CONFIGURABLE)) {
            return Optional.empty();
        }

        return Optional.ofNullable(constructConfigVarNode(globalVar));
    }

    /**
     * Construct a configuration variable node from the given variable node.
     */
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

    /**
     * Construct a configuration variable node from the given BLang variable.
     */
    private static FlowNode constructConfigVarNode(BLangVariable variable) {
        // TODO: Can we add diagnostics handler without semantic model?
        // DiagnosticHandler diagnosticHandler = new DiagnosticHandler(semanticModel);
        NodeBuilder nodeBuilder = NodeBuilder.getNodeFromKind(NodeKind.CONFIG_VARIABLE)
                // .semanticModel(semanticModel)
                // .diagnosticHandler(diagnosticHandler)
                .defaultModuleName(null);

        BVarSymbol symbol = variable.symbol;
        // diagnosticHandler.handle(nodeBuilder, symbol.getPosition().lineRange(), false);

        return nodeBuilder
                .metadata()
                .label("Config variables")
                .stepOut()
                .codedata()
                .node(NodeKind.CONFIG_VARIABLE)
                .lineRange(symbol.getPosition().lineRange())
                .stepOut()
                .properties()
                .type(symbol.getType().getQualifiedTypeName(), false, null)
                .variableName(symbol.getName().getValue())
                .defaultValue(getDefaultValueExpression(variable))
                .stepOut()
                .build();
    }

    private static ExpressionNode getDefaultValueExpression(BLangVariable variable) {
        String defaultVal = variable.getInitialExpression() != null ? variable.getInitialExpression().toString() : null;
        try {
            return NodeParser.parseExpression(defaultVal);
        } catch (Exception e) {
            return null;
        }
    }
}
