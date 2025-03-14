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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.Types;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.EnumSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import org.ballerinalang.model.types.TypeKind;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

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

    private final Map<String, TypeSymbol> typeSymbolMap;
    private final Map<TypeSymbol, CompletionItem> completionItemMap;
    private final Map<TypeSymbol, List<CompletionItem>> subtypeItemsMap;
    private volatile boolean initialized = false;

    private static final String USER_DEFINED_TYPE = "User-Defined";
    private static final List<SymbolKind> TYPE_SYMBOL_KINDS = List.of(SymbolKind.TYPE_DEFINITION, SymbolKind.CLASS,
            SymbolKind.ENUM);

    // Basic simple types
    public static final String TYPE_BOOLEAN = TypeKind.BOOLEAN.typeName();
    public static final String TYPE_DECIMAL = TypeKind.DECIMAL.typeName();
    public static final String TYPE_FLOAT = TypeKind.FLOAT.typeName();
    public static final String TYPE_INT = TypeKind.INT.typeName();
    public static final String TYPE_NIL = "()";

    // Basic sequence types
    public static final String TYPE_STRING = TypeKind.STRING.typeName();
    public static final String TYPE_XML = TypeKind.XML.typeName();

    // Basic behavioral
    public static final String TYPE_ERROR = TypeKind.ERROR.typeName();
    public static final String TYPE_FUNCTION = TypeKind.FUNCTION.typeName();
    public static final String TYPE_FUTURE = TypeKind.FUTURE.typeName();
    public static final String TYPE_HANDLE = TypeKind.HANDLE.typeName();
    public static final String TYPE_STREAM = TypeKind.STREAM.typeName();
    public static final String TYPE_TYPEDESC = TypeKind.TYPEDESC.typeName();

    // Predefined structural types
    public static final String TYPE_BYTE_ARRAY = "byte[]";
    public static final String TYPE_MAP_JSON = "map<json>";
    public static final String TYPE_MAP_STRING = "map<string>";
    public static final String TYPE_JSON_ARRAY = "json[]";

    // Other
    public static final String TYPE_ANY = TypeKind.ANY.typeName();
    public static final String TYPE_ANYDATA = TypeKind.ANYDATA.typeName();
    public static final String TYPE_BYTE = TypeKind.BYTE.typeName();
    public static final String TYPE_JSON = TypeKind.JSON.typeName();
    public static final String TYPE_READONLY = TypeKind.READONLY.typeName();
    public static final String TYPE_RECORD = TypeKind.RECORD.typeName();

    // Categories
    private static final Map<String, List<String>> categoryMap = Map.of(
            "Primitive Types",
            List.of(TYPE_STRING, TYPE_INT, TYPE_FLOAT, TYPE_DECIMAL, TYPE_BOOLEAN, TYPE_NIL, TYPE_BYTE),
            "Data Types", List.of(TYPE_JSON, TYPE_XML, TYPE_ANYDATA),
            "Structural Types", List.of(TYPE_BYTE_ARRAY, TYPE_MAP_JSON, TYPE_MAP_STRING, TYPE_JSON_ARRAY, TYPE_RECORD),
            "Error Types", List.of(TYPE_ERROR),
            "Behaviour Types", List.of(TYPE_FUNCTION, TYPE_FUTURE, TYPE_TYPEDESC, TYPE_HANDLE, TYPE_STREAM),
            "Other Types", List.of(TYPE_ANY, TYPE_READONLY));

    private TypesGenerator() {
        this.typeSymbolMap = new LinkedHashMap<>();
        this.completionItemMap = new LinkedHashMap<>();
        this.subtypeItemsMap = new LinkedHashMap<>();
    }

    public Either<List<CompletionItem>, CompletionList> getTypes(SemanticModel semanticModel, String typeConstraint) {
        // Get the symbol of the type constraint
        initializeBuiltinTypes(semanticModel);
        TypeSymbol typeSymbol = typeSymbolMap.get(typeConstraint);

        // If the symbol not found, return all the completions
        if (typeSymbol == null) {
            List<CompletionItem> completionItems = semanticModel.moduleSymbols().parallelStream()
                    .filter(symbol -> TYPE_SYMBOL_KINDS.contains(symbol.kind()))
                    .map(symbol -> TypeCompletionItemBuilder.build(symbol, symbol.getName().orElse(""),
                            USER_DEFINED_TYPE))
                    .collect(Collectors.toCollection(ArrayList::new));
            completionItems.addAll(completionItemMap.values());
            return Either.forLeft(completionItems);
        }

        // Get the filtered type completions
        List<CompletionItem> completionItems = semanticModel.moduleSymbols().parallelStream()
                .filter(symbol -> isSubtype(typeSymbol, symbol))
                .map(symbol -> TypeCompletionItemBuilder.build(symbol, symbol.getName().orElse(""),
                        USER_DEFINED_TYPE))
                .collect(Collectors.toCollection(ArrayList::new));
        completionItems.addAll(subtypeItemsMap.get(typeSymbol));
        return Either.forLeft(completionItems);
    }

    private static boolean isSubtype(TypeSymbol parentSymbol, Symbol childSymbol) {
        TypeSymbol childTypeSymbol = switch (childSymbol.kind()) {
            case TYPE_DEFINITION -> ((TypeDefinitionSymbol) childSymbol).typeDescriptor();
            case CLASS -> ((ClassSymbol) childSymbol);
            case ENUM -> ((EnumSymbol) childSymbol).typeDescriptor();
            default -> null;
        };
        if (childTypeSymbol == null) {
            return false;
        }
        return childTypeSymbol.subtypeOf(parentSymbol);
    }

    public Optional<TypeSymbol> getTypeSymbol(SemanticModel semanticModel, String typeName) {
        initializeBuiltinTypes(semanticModel);
        return Optional.ofNullable(typeSymbolMap.get(typeName));
    }

    private void initializeBuiltinTypes(SemanticModel semanticModel) {
        if (initialized) {
            return;
        }

        synchronized (this) {
            if (initialized) {
                return;
            }

            // Obtain the type symbols for the builtin types
            Types types = semanticModel.types();
            typeSymbolMap.put(TYPE_STRING, types.STRING);
            typeSymbolMap.put(TYPE_BOOLEAN, types.BOOLEAN);
            typeSymbolMap.put(TYPE_INT, types.INT);
            typeSymbolMap.put(TYPE_NIL, types.NIL);
            typeSymbolMap.put(TYPE_FLOAT, types.FLOAT);
            typeSymbolMap.put(TYPE_DECIMAL, types.DECIMAL);
            typeSymbolMap.put(TYPE_XML, types.XML);
            typeSymbolMap.put(TYPE_BYTE, types.BYTE);
            typeSymbolMap.put(TYPE_ERROR, types.ERROR);
            typeSymbolMap.put(TYPE_JSON, types.JSON);
            typeSymbolMap.put(TYPE_ANY, types.ANY);
            typeSymbolMap.put(TYPE_ANYDATA, types.ANYDATA);
            typeSymbolMap.put(TYPE_FUNCTION, types.FUNCTION);
            typeSymbolMap.put(TYPE_FUTURE, types.FUTURE);
            typeSymbolMap.put(TYPE_TYPEDESC, types.TYPEDESC);
            typeSymbolMap.put(TYPE_HANDLE, types.HANDLE);
            typeSymbolMap.put(TYPE_STREAM, types.STREAM);
            typeSymbolMap.put(TYPE_READONLY, types.READONLY);
            typeSymbolMap.put(TYPE_RECORD, types.builder().RECORD_TYPE.withRestField(types.ANYDATA).build());
            typeSymbolMap.put(TYPE_MAP_JSON, types.builder().MAP_TYPE.withTypeParam(types.JSON).build());
            typeSymbolMap.put(TYPE_MAP_STRING, types.builder().MAP_TYPE.withTypeParam(types.STRING).build());
            typeSymbolMap.put(TYPE_JSON_ARRAY, types.builder().ARRAY_TYPE.withType(types.JSON).build());
            typeSymbolMap.put(TYPE_BYTE_ARRAY, types.builder().ARRAY_TYPE.withType(types.BYTE).build());

            // Build the completion items for the builtin types
            categoryMap.forEach((category, typeNames) -> {
                typeNames.forEach(typeName -> {
                    TypeSymbol symbol = typeSymbolMap.get(typeName);
                    completionItemMap.put(symbol, TypeCompletionItemBuilder.build(symbol, typeName, category));
                });
            });

            // Build the subtype items for the builtin types
            typeSymbolMap.forEach((name, symbol) -> {
                List<CompletionItem> completionsList = typeSymbolMap.values().parallelStream()
                        .filter(typeSymbol -> typeSymbol.subtypeOf(symbol))
                        .map(completionItemMap::get)
                        .toList();
                subtypeItemsMap.put(symbol, completionsList);
            });

            initialized = true;
        }
    }

    public static TypesGenerator getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {

        private static final TypesGenerator INSTANCE = new TypesGenerator();
    }
}
