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
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.flowmodelgenerator.core.central.Function;
import io.ballerina.flowmodelgenerator.core.central.FunctionsResponse;
import io.ballerina.flowmodelgenerator.core.central.LocalIndexCentral;
import io.ballerina.flowmodelgenerator.core.central.PackageResponse;
import io.ballerina.flowmodelgenerator.core.central.RemoteCentral;
import io.ballerina.flowmodelgenerator.core.central.SymbolResponse;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.PositionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Generates functions based on a given keyword.
 *
 * @since 1.4.0
 */
public class FunctionGenerator {

    private final Gson gson;
    private final SemanticModel semanticModel;
    private final Category.Builder rootBuilder;

    public FunctionGenerator(SemanticModel semanticModel, Document document) {
        gson = new Gson();
        this.semanticModel = semanticModel;
        this.rootBuilder = new Category.Builder(null);
    }

    public JsonArray getFunctions(Map<String, String> queryMap, LineRange position) {
        buildProjectNodes(queryMap, position);
        buildUtilityNodes(queryMap);
        return gson.toJsonTree(rootBuilder.build().items()).getAsJsonArray();
    }

    private void buildProjectNodes(Map<String, String> queryMap, LineRange position) {
        List<Symbol> functionSymbols = semanticModel.moduleSymbols().stream()
                .filter(symbol -> symbol.kind().equals(SymbolKind.FUNCTION)).toList();
        Category.Builder projectBuilder = rootBuilder.stepIn(Category.Name.PROJECT_FUNCTIONS);

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
                    .node(FlowNode.Kind.FUNCTION_CALL)
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

    private void buildUtilityNodes(Map<String, String> queryMap) {
        Category.Builder utilityBuilder = rootBuilder.stepIn(Category.Name.UTILITIES);

        if (CommonUtils.hasNoKeyword(queryMap)) {
            utilityBuilder.items(LocalIndexCentral.getInstance().getFunctions());
            return;
        }

        PackageResponse packages = RemoteCentral.getInstance().searchPackages(queryMap);

        // Find the packages for the given query.
        for (PackageResponse.Package pkg : packages.packages()) {
            if (isNonBallerinaOrg(pkg.organization())) {
                continue;
            }
            FunctionsResponse functionResponses =
                    RemoteCentral.getInstance().functions(pkg.organization(), pkg.name(), pkg.version());

            List<Function> functions = functionResponses.data().apiDocs().docsData().modules().stream()
                    .flatMap(module -> module.functions().stream())
                    .toList();
            if (functions.isEmpty()) {
                continue;
            }

            // Add every function in the package.
            Category.Builder builder = utilityBuilder.stepIn(pkg.name())
                    .metadata()
                    .label(pkg.name())
                    .description(pkg.summary())
                    .icon(pkg.icon())
                    .stepOut();

            for (Function function : functions) {
                Metadata metadata = new Metadata.Builder<>(null)
                        .label(function.name())
                        .description(function.description())
                        .build();
                Codedata codedata = new Codedata.Builder<>(null)
                        .node(FlowNode.Kind.FUNCTION_CALL)
                        .org(pkg.organization())
                        .module(pkg.name())
                        .symbol(function.name())
                        .version(pkg.version())
                        .build();
                builder.node(new AvailableNode(metadata, codedata, true));
            }
        }

        // Find the symbols for the given query.
        SymbolResponse symbolResponse = RemoteCentral.getInstance().searchSymbols(queryMap);
        for (SymbolResponse.Symbol symbol : symbolResponse.symbols()) {
            if (!symbol.symbolType().equals("function") || (isNonBallerinaOrg(symbol.organization()))) {
                continue;
            }
            Metadata metadata = new Metadata.Builder<>(null)
                    .label(symbol.symbolName())
                    .description(symbol.description())
                    .icon(symbol.icon())
                    .build();
            Codedata codedata = new Codedata.Builder<>(null)
                    .node(FlowNode.Kind.FUNCTION_CALL)
                    .org(symbol.organization())
                    .module(symbol.name())
                    .symbol(symbol.symbolName())
                    .version(symbol.version())
                    .build();
            utilityBuilder.stepIn(symbol.name()).node(new AvailableNode(metadata, codedata, true));
        }
    }

    private boolean isNonBallerinaOrg(String organization) {
        return !organization.equals("ballerina") && !organization.equals("ballerinax");
    }
}
