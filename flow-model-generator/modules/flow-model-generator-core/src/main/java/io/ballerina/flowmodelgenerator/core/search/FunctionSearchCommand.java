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

package io.ballerina.flowmodelgenerator.core.search;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.AnnotationAttachmentSymbol;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.SearchResult;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.PositionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a command to search for functions within a module. This class extends SearchCommand and provides
 * functionality to search for both project-specific and library functions.
 *
 * <p>
 * The search includes:
 * <li>Functions within the current project/module </li>
 * <li>Imported functions from dependencies</li>
 * <li>Available functions from the standard library (if enabled)</li>
 *
 * <p>The search results are organized into different categories:</p>
 * <li>CURRENT_INTEGRATION: Functions from the current project</li>
 * <li>IMPORTED_FUNCTIONS: Functions from imported modules</li>
 * <li>AVAILABLE_FUNCTIONS: Functions available but not imported (optional)</li>
 * </p>
 *
 * @see SearchCommand
 * @since 2.0.0
 */
class FunctionSearchCommand extends SearchCommand {

    public static final String TOOL_ANNOTATION = "Tool";
    private static final String BALLERINAX = "ballerinax";
    private static final String AI_AGENT = "ai.agent";
    private static final Map<String, List<String>> POPULAR_BALLERINA_FUNCTIONS = Map.of(
            "log", List.of("printInfo", "printDebug", "printError", "printWarn"),
            "time", List.of("utcNow", "utcFromString"),
            "io", List.of("print", "println", "fileWriteString", "fileWriteJson", "fileReadString", "fileReadJson")
    );
    private static final String FETCH_KEY = "functions";
    private final List<String> moduleNames;

    public FunctionSearchCommand(Project project, LineRange position, Map<String, String> queryMap) {
        super(project, position, queryMap);

        // Obtain the imported module names
        Package currentPackage = project.currentPackage();
        currentPackage.getCompilation();
        moduleNames = currentPackage.getDefaultModule().moduleDependencies().stream()
                .map(moduleDependency -> moduleDependency.descriptor().name().packageName().value())
                .toList();
        // TODO: Use this method when https://github.com/ballerina-platform/ballerina-lang/issues/43695 is fixed
        // List<String> moduleNames = semanticModel.moduleSymbols().stream()
        // .filter(symbol -> symbol.kind().equals(SymbolKind.MODULE))
        // .flatMap(symbol -> symbol.getName().stream())
        // .toList();
    }

    @Override
    protected List<Item> defaultView() {
        buildProjectNodes();
        List<SearchResult> searchResults = new ArrayList<>();
        if (!moduleNames.isEmpty()) {
            searchResults.addAll(dbManager.searchFunctionsByPackages(moduleNames, List.of(), limit, offset));
        }
        searchResults.addAll(defaultViewHolder.get(this).getOrDefault(FETCH_KEY, List.of()));
        buildLibraryNodes(searchResults);
        return rootBuilder.build().items();
    }

    @Override
    protected List<Item> search() {
        buildProjectNodes();
        List<SearchResult> functionSearchList = dbManager.searchFunctions(query, limit, offset);
        buildLibraryNodes(functionSearchList);
        return rootBuilder.build().items();
    }

    @Override
    protected Map<String, List<SearchResult>> fetchPopularItems() {
        List<String> packageNames = new ArrayList<>(POPULAR_BALLERINA_FUNCTIONS.keySet());
        List<String> functionNames = POPULAR_BALLERINA_FUNCTIONS.values().stream()
                .flatMap(List::stream)
                .toList();
        return Map.of(FETCH_KEY, dbManager.searchFunctionsByPackages(packageNames, functionNames, limit, offset));
    }

    private void buildProjectNodes() {
        List<Symbol> functionSymbols = project.currentPackage().getDefaultModule().getCompilation().
                getSemanticModel().moduleSymbols().stream()
                .filter(symbol -> symbol.kind().equals(SymbolKind.FUNCTION) && !symbol.nameEquals("main")).toList();
        Category.Builder projectBuilder = rootBuilder.stepIn(Category.Name.CURRENT_INTEGRATION);
        Category.Builder agentToolsBuilder = rootBuilder.stepIn(Category.Name.AGENT_TOOLS);

        List<Item> availableNodes = new ArrayList<>();
        List<Item> availableTools = new ArrayList<>();
        for (Symbol symbol : functionSymbols) {
            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
            boolean isDataMappedFunction = false;
            Optional<Location> location = symbol.getLocation();
            if (location.isPresent()) {
                isDataMappedFunction = location.get().lineRange().fileName().equals(DATA_MAPPER_FILE_NAME);
                LineRange fnLineRange = location.get().lineRange();
                if (fnLineRange.fileName().equals(position.fileName()) &&
                        PositionUtil.isWithinLineRange(fnLineRange, position)) {
                    continue;
                }
            }
            symbol.getName();

            if (symbol.getName().isEmpty() ||
                    (!query.isEmpty() && !symbol.getName().get().toLowerCase(Locale.ROOT)
                            .contains(query.toLowerCase(Locale.ROOT)))) {
                continue;
            }

            boolean isAgentTool = isAgentTool(functionSymbol);
            Metadata metadata = new Metadata.Builder<>(null)
                    .label(symbol.getName().get())
                    .description(functionSymbol.documentation()
                            .flatMap(Documentation::description)
                            .orElse(null))
                    .addData("isDataMappedFunction", isDataMappedFunction)
                    .addData("isAgentTool", isAgentTool)
                    .build();

            Codedata.Builder<Object> codedata = new Codedata.Builder<>(null)
                    .node(NodeKind.FUNCTION_CALL)
                    .symbol(symbol.getName().get());
            Optional<ModuleSymbol> moduleSymbol = functionSymbol.getModule();
            if (moduleSymbol.isPresent()) {
                ModuleID id = moduleSymbol.get().id();
                id.packageName();
                id.moduleName();
            }

            if (isAgentTool) {
                availableTools.add(new AvailableNode(metadata, codedata.build(), true));
            } else {
                availableNodes.add(new AvailableNode(metadata, codedata.build(), true));
            }
        }
        projectBuilder.items(availableNodes);
        agentToolsBuilder.items(availableTools);
    }

    private void buildLibraryNodes(List<SearchResult> functionSearchList) {
        // Set the categories based on the available flags
        Category.Builder importedFnBuilder = rootBuilder.stepIn(Category.Name.IMPORTED_FUNCTIONS);
        Category.Builder availableFnBuilder = rootBuilder.stepIn(Category.Name.AVAILABLE_FUNCTIONS);

        // Add the library functions
        for (SearchResult searchResult : functionSearchList) {
            SearchResult.Package packageInfo = searchResult.packageInfo();

            // Add the function to the respective category
            String icon = CommonUtils.generateIcon(packageInfo.org(), packageInfo.name(), packageInfo.version());
            Metadata metadata = new Metadata.Builder<>(null)
                    .label(searchResult.name())
                    .description(searchResult.description())
                    .icon(icon)
                    .build();
            Codedata codedata = new Codedata.Builder<>(null)
                    .node(NodeKind.FUNCTION_CALL)
                    .org(packageInfo.org())
                    .module(packageInfo.name())
                    .symbol(searchResult.name())
                    .version(packageInfo.version())
                    .build();
            Category.Builder builder =
                    moduleNames.contains(packageInfo.name()) ? importedFnBuilder : availableFnBuilder;
            if (builder != null) {
                builder.stepIn(packageInfo.name(), "", List.of())
                        .node(new AvailableNode(metadata, codedata, true));
            }
        }
    }

    private boolean isAgentTool(FunctionSymbol functionSymbol) {
        for (AnnotationAttachmentSymbol annotAttachment : functionSymbol.annotAttachments()) {
            AnnotationSymbol annotationSymbol = annotAttachment.typeDescriptor();
            Optional<ModuleSymbol> optModule = annotationSymbol.getModule();
            if (optModule.isEmpty()) {
                continue;
            }
            ModuleID id = optModule.get().id();
            if (!(id.orgName().equals(BALLERINAX) && id.packageName().equals(AI_AGENT))) {
                continue;
            }
            Optional<String> optName = annotationSymbol.getName();
            if (optName.isEmpty()) {
                continue;
            }
            if (optName.get().equals(TOOL_ANNOTATION)) {
                return true;
            }
        }
        return false;
    }
}
