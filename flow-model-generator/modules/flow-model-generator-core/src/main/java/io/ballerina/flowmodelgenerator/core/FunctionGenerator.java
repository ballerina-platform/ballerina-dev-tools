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
import io.ballerina.flowmodelgenerator.core.central.ApiResponse;
import io.ballerina.flowmodelgenerator.core.central.LocalIndexCentral;
import io.ballerina.flowmodelgenerator.core.central.PackageResponse;
import io.ballerina.flowmodelgenerator.core.central.RemoteCentral;
import io.ballerina.flowmodelgenerator.core.central.SymbolResponse;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.Metadata;

import java.util.List;
import java.util.Map;

/**
 * Generates functions based on a given keyword.
 *
 * @since 1.4.0
 */
public class FunctionGenerator {

    private final Gson gson;

    public FunctionGenerator() {
        gson = new Gson();
    }

    public JsonArray getFunctions(Map<String, String> queryMap) {
        if (CommonUtils.hasNoKeyword(queryMap)) {
            return gson.toJsonTree(LocalIndexCentral.getInstance().getFunctions()).getAsJsonArray();
        }

        PackageResponse packages = RemoteCentral.getInstance().searchPackages(queryMap);
        Category.Builder rootBuilder = new Category.Builder(null);

        // Find the packages for the given query.
        for (PackageResponse.Package pkg : packages.packages()) {
            if (isUserOrganization(pkg.organization())) {
                continue;
            }
            ApiResponse functionResponses =
                    RemoteCentral.getInstance().functions(pkg.organization(), pkg.name(), pkg.version());

            List<ApiResponse.Function> functions = functionResponses.data().apiDocs().docsData().modules().stream()
                    .flatMap(module -> module.functions().stream())
                    .toList();
            if (functions.isEmpty()) {
                continue;
            }

            // Add every function in the package.
            Category.Builder builder = rootBuilder.stepIn(pkg.name())
                    .metadata()
                    .label(pkg.name())
                    .description(pkg.summary())
                    .icon(pkg.icon())
                    .stepOut();

            for (ApiResponse.Function function : functions) {
                Metadata metadata = new Metadata.Builder<>(null)
                        .label(function.name())
                        .description(function.description())
                        .build();
                Codedata codedata = new Codedata.Builder<>(null)
                        .node(FlowNode.Kind.FUNCTION_CALL)
                        .org(pkg.organization())
                        .module(pkg.name())
                        .object(function.name())
                        .build();
                builder.node(new AvailableNode(metadata, codedata, true));
            }
        }

        // Find the symbols for the given query.
        SymbolResponse symbolResponse = RemoteCentral.getInstance().searchSymbols(queryMap);
        for (SymbolResponse.Symbol symbol : symbolResponse.symbols()) {
            if (!symbol.symbolType().equals("function") || (isUserOrganization(symbol.organization()))) {
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
                    .object(symbol.symbolName())
                    .build();
            rootBuilder.stepIn(symbol.name()).node(new AvailableNode(metadata, codedata, true));
        }

        return gson.toJsonTree(rootBuilder.build().items()).getAsJsonArray();

    }

    private boolean isUserOrganization(String organization) {
        return !organization.equals("ballerina") && !organization.equals("ballerinax");
    }
}
