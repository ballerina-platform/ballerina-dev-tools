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
import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a command to search for natural programming functions within a module.
 * This class provides the functionality to search for user defined and `ballerinax/np` library functions.
 *
 * @since 2.0.0
 */
class NPFunctionSearchCommand extends SearchCommand {

    private static final String BALLERINAX_ORG_NAME = "ballerinax";
    private static final String NP_PACKAGE_NAME = "np";
    private static final String CALL_LLM_FUNCTION_NAME = "callLlm";

    private static final String BUILT_IN_FUNCTIONS = "Built-in Functions";
    private final List<String> moduleNames;

    public NPFunctionSearchCommand(Project project, LineRange position, Map<String, String> queryMap) {
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
        getCallLlmNode();
        buildProjectNodes();
        return rootBuilder.build().items();
    }

    private void getCallLlmNode() {
        Metadata metadata = new Metadata.Builder<>(null)
                .label("Call LLM")
                .description("AI Call\n```ballerina\nnp:callLlm(\"prompt message\", id = 845315)\n```\n\n")
                .icon("https://gist.github.com/user-attachments/assets/903c5c16-7d67-4af8-8113-ce7c59ccdaab")
                .build();

        Codedata codedata = new Codedata.Builder<>(null)
                .node(NodeKind.NP_FUNCTION_CALL)
                .org(BALLERINAX_ORG_NAME)
                .module(NP_PACKAGE_NAME)
                .symbol(CALL_LLM_FUNCTION_NAME)
                .version("0.1.0")
                .build();

        Category.Builder builder = rootBuilder.stepIn(BUILT_IN_FUNCTIONS, null, null);
        builder.node(new AvailableNode(metadata, codedata, true));
    }

    @Override
    protected List<Item> search() {
        buildProjectNodes();
        return rootBuilder.build().items();
    }

    @Override
    protected Map<String, List<SearchResult>> fetchPopularItems() {
        return Map.of();
    }

    private void buildProjectNodes() {
        List<Symbol> functionSymbols = project.currentPackage().getDefaultModule().getCompilation().
                getSemanticModel().moduleSymbols().stream()
                .filter(symbol -> symbol.kind().equals(SymbolKind.FUNCTION)).toList();
        Category.Builder projectBuilder = rootBuilder.stepIn(Category.Name.CURRENT_INTEGRATION);

        List<Item> availableNodes = new ArrayList<>();
        for (Symbol symbol : functionSymbols) {
            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
            if (!CommonUtils.isNpFunction(functionSymbol)) {
                continue;
            }

            Metadata metadata = new Metadata.Builder<>(null)
                    .label(symbol.getName().get())
                    .description(functionSymbol.documentation()
                            .flatMap(Documentation::description)
                            .orElse(null))
                    .build();

            Codedata.Builder<Object> codedata = new Codedata.Builder<>(null)
                    .node(NodeKind.NP_FUNCTION_CALL)
                    .symbol(symbol.getName().get());
            Optional<ModuleSymbol> moduleSymbol = functionSymbol.getModule();
            if (moduleSymbol.isPresent()) {
                ModuleID id = moduleSymbol.get().id();
                id.packageName();
                id.moduleName();
            }

            availableNodes.add(new AvailableNode(metadata, codedata.build(), true));
        }
        projectBuilder.items(availableNodes);
    }
}
