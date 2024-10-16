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
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.SymbolKind;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is responsible for generating types from the semantic model.
 */
public class TypesGenerator {

    private final SemanticModel semanticModel;
    private static final List<String> DEFAULT_TYPES =
            List.of("int", "string", "float", "boolean", "decimal", "xml", "error", "function", "future", "typedesc",
                    "handle", "stream", "any", "anydata", "stream", "never", "readonly", "json", "byte");
    private final Gson gson;

    public TypesGenerator(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.gson = new Gson();
    }

    public JsonArray getTypes() {
        List<String> visibleTypes = semanticModel.moduleSymbols().stream()
                .filter(symbol -> symbol.kind() == SymbolKind.TYPE_DEFINITION)
                .flatMap(symbol -> symbol.getName().stream())
                .collect(Collectors.toCollection(ArrayList::new));
        visibleTypes.addAll(DEFAULT_TYPES);
        return gson.toJsonTree(visibleTypes).getAsJsonArray();
    }
}
