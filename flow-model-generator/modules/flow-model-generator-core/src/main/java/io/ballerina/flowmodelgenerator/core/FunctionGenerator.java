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
import com.google.gson.JsonArray;
import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.flowmodelgenerator.core.db.DatabaseManager;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.projects.Module;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.PositionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Generates functions based on a given keyword.
 *
 * @since 2.0.0
 */
public class FunctionGenerator {

    private final Gson gson;
    private final Module module;
    private final Category.Builder rootBuilder;

    private static final String INCLUDE_AVAILABLE_FUNCTIONS_FLAG = "includeAvailableFunctions";

    public FunctionGenerator(Module module) {
        gson = new Gson();
        this.module = module;
        this.rootBuilder = new Category.Builder(null);
    }

    public JsonArray getFunctions(Map<String, String> queryMap, LineRange position) {
        buildProjectNodes(queryMap, position);

        Map<String, String> modifiedQueryMap = new HashMap<>(queryMap);
        if (CommonUtils.hasNoKeyword(queryMap, "limit")) {
            modifiedQueryMap.put("limit", "20");
        }
        if (CommonUtils.hasNoKeyword(queryMap, "offset")) {
            modifiedQueryMap.put("offset", "0");
        }
        if (CommonUtils.hasNoKeyword(queryMap, INCLUDE_AVAILABLE_FUNCTIONS_FLAG)) {
            modifiedQueryMap.put(INCLUDE_AVAILABLE_FUNCTIONS_FLAG, "false");
        }
        buildLibraryFunctions(modifiedQueryMap);

        return gson.toJsonTree(rootBuilder.build().items()).getAsJsonArray();
    }

    private void buildProjectNodes(Map<String, String> queryMap, LineRange position) {
        List<Symbol> functionSymbols = module.getCompilation().getSemanticModel().moduleSymbols().stream()
                .filter(symbol -> symbol.kind().equals(SymbolKind.FUNCTION)).toList();
        Category.Builder projectBuilder = rootBuilder.stepIn(Category.Name.CURRENT_INTEGRATION);

        String keyword = queryMap.get("q");
        List<Item> availableNodes = new ArrayList<>();
        for (Symbol symbol : functionSymbols) {
            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
            if (symbol.getLocation().isPresent()) {
                LineRange fnLineRange = symbol.getLocation().get().lineRange();
                if (PositionUtil.isWithinLineRange(fnLineRange, position)) {
                    continue;
                }
            }
            symbol.getName();

            if (symbol.getName().isEmpty() ||
                    (keyword != null && !symbol.getName().get().toLowerCase(Locale.ROOT)
                            .contains(keyword.toLowerCase(Locale.ROOT)))) {
                continue;
            }

            Metadata metadata = new Metadata.Builder<>(null)
                    .label(symbol.getName().get())
                    .description(functionSymbol.documentation()
                            .flatMap(Documentation::description)
                            .orElse(null))
                    .build();

            Codedata.Builder<Object> codedata = new Codedata.Builder<>(null)
                    .node(NodeKind.FUNCTION_CALL)
                    .symbol(symbol.getName().get());
            Optional<ModuleSymbol> module = functionSymbol.getModule();
            if (module.isPresent()) {
                ModuleID id = module.get().id();
                id.packageName();
                id.moduleName();
            }

            availableNodes.add(new AvailableNode(metadata, codedata.build(), true));
        }
        projectBuilder.items(availableNodes);

    }

    private void buildLibraryFunctions(Map<String, String> queryMap) {
        // Obtain the imported module names
        List<String> moduleNames = module.moduleDependencies().stream()
                .map(moduleDependency -> moduleDependency.descriptor().name().packageName().value())
                .toList();
        // TODO: Use this method when https://github.com/ballerina-platform/ballerina-lang/issues/43695 is fixed
//        List<String> moduleNames = semanticModel.moduleSymbols().stream()
//                .filter(symbol -> symbol.kind().equals(SymbolKind.MODULE))
//                .flatMap(symbol -> symbol.getName().stream())
//                .toList();

        // Build the imported library functions if exists
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!moduleNames.isEmpty()) {
            List<FunctionResult> functionsByPackages =
                    dbManager.searchFunctionsInPackages(moduleNames, queryMap, DatabaseManager.FunctionKind.FUNCTION);
            Category.Builder libraryBuilder = rootBuilder.stepIn(Category.Name.IMPORTED_FUNCTIONS);
            addLibraryFunction(functionsByPackages, libraryBuilder);
        }

        // Build the available library functions if flag is set
        if (queryMap.get(INCLUDE_AVAILABLE_FUNCTIONS_FLAG).equals("false")) {
            return;
        }
        Category.Builder utilityBuilder = rootBuilder.stepIn(Category.Name.AVAILABLE_FUNCTIONS);
        List<FunctionResult> functionResults = CommonUtils.hasNoKeyword(queryMap, "q")
                ? dbManager.getFunctionsByOrg("ballerina", DatabaseManager.FunctionKind.FUNCTION)
                : dbManager.searchFunctions(queryMap, DatabaseManager.FunctionKind.FUNCTION);
        functionResults.removeIf(functionResult -> moduleNames.contains(functionResult.packageName()));
        addLibraryFunction(functionResults, utilityBuilder);
    }

    private static void addLibraryFunction(List<FunctionResult> functionResults, Category.Builder utilityBuilder) {
        for (FunctionResult functionResult : functionResults) {
            String icon = CommonUtils.generateIcon(functionResult.org(), functionResult.packageName(),
                    functionResult.version());
            Metadata metadata = new Metadata.Builder<>(null)
                    .label(functionResult.name())
                    .description(functionResult.description())
                    .icon(icon)
                    .build();
            Codedata codedata = new Codedata.Builder<>(null)
                    .node(NodeKind.FUNCTION_CALL)
                    .org(functionResult.org())
                    .module(functionResult.packageName())
                    .symbol(functionResult.name())
                    .version(functionResult.version())
                    .build();
            utilityBuilder.stepIn(functionResult.packageName(), "", icon)
                    .node(new AvailableNode(metadata, codedata, true));
        }
    }
}
