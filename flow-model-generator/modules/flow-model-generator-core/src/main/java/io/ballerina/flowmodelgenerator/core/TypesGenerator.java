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
import io.ballerina.compiler.api.Types;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import org.ballerinalang.model.types.TypeKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class is responsible for generating types from the semantic model.
 *
 * @since 2.0.0
 */
public class TypesGenerator {

    private final Gson gson;
    private final Map<String, TypeSymbol> builtinTypeSymbols;

    private TypesGenerator() {
        this.gson = new Gson();
        this.builtinTypeSymbols = new LinkedHashMap<>();
    }

    public JsonArray getTypes(SemanticModel semanticModel) {
        List<String> visibleTypes = semanticModel.moduleSymbols().parallelStream()
                .filter(symbol -> symbol.kind() == SymbolKind.TYPE_DEFINITION)
                .flatMap(symbol -> symbol.getName().stream())
                .collect(Collectors.toCollection(ArrayList::new));
        initializeTypeSymbolMap(semanticModel);
        visibleTypes.addAll(builtinTypeSymbols.keySet());
        return gson.toJsonTree(visibleTypes).getAsJsonArray();
    }

    public Optional<TypeSymbol> getTypeSymbol(SemanticModel semanticModel, String typeName) {
        initializeTypeSymbolMap(semanticModel);
        return Optional.ofNullable(builtinTypeSymbols.get(typeName));
    }

    private void initializeTypeSymbolMap(SemanticModel semanticModel) {
        if (!builtinTypeSymbols.isEmpty()) {
            return;
        }

        Types types = semanticModel.types();
        builtinTypeSymbols.put(TypeKind.STRING.typeName(), types.STRING);
        builtinTypeSymbols.put(TypeKind.BOOLEAN.typeName(), types.BOOLEAN);
        builtinTypeSymbols.put(TypeKind.INT.typeName(), types.INT);
        builtinTypeSymbols.put(TypeKind.FLOAT.typeName(), types.FLOAT);
        builtinTypeSymbols.put(TypeKind.DECIMAL.typeName(), types.DECIMAL);
        builtinTypeSymbols.put(TypeKind.XML.typeName(), types.XML);
        builtinTypeSymbols.put(TypeKind.BYTE.typeName(), types.BYTE);
        builtinTypeSymbols.put(TypeKind.ERROR.typeName(), types.ERROR);
        builtinTypeSymbols.put(TypeKind.JSON.typeName(), types.JSON);
        builtinTypeSymbols.put(TypeKind.ANY.typeName(), types.ANY);
        builtinTypeSymbols.put(TypeKind.ANYDATA.typeName(), types.ANYDATA);
        builtinTypeSymbols.put(TypeKind.FUNCTION.typeName(), types.FUNCTION);
        builtinTypeSymbols.put(TypeKind.FUTURE.typeName(), types.FUTURE);
        builtinTypeSymbols.put(TypeKind.TYPEDESC.typeName(), types.TYPEDESC);
        builtinTypeSymbols.put(TypeKind.HANDLE.typeName(), types.HANDLE);
        builtinTypeSymbols.put(TypeKind.STREAM.typeName(), types.STREAM);
        builtinTypeSymbols.put(TypeKind.NEVER.typeName(), types.NEVER);
        builtinTypeSymbols.put(TypeKind.READONLY.typeName(), types.READONLY);
    }

    public static TypesGenerator getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {

        private static final TypesGenerator INSTANCE = new TypesGenerator();
    }
}
