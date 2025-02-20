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
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.Types;
import io.ballerina.compiler.api.symbols.SymbolKind;
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

    private final Map<String, TypeSymbol> builtinTypeSymbols;
    private final Map<String, CompletionItem> builtinTypeCompletionItems;

    private static final String PRIMITIVE_TYPE = "Primitive";
    private static final String USER_DEFINED_TYPE = "User-Defined";
    private static final List<SymbolKind> TYPE_SYMBOL_KINDS =
            List.of(SymbolKind.TYPE_DEFINITION, SymbolKind.CLASS, SymbolKind.ENUM);

    // Builtin type names
    public static final String TYPE_STRING = TypeKind.STRING.typeName();
    public static final String TYPE_BOOLEAN = TypeKind.BOOLEAN.typeName();
    public static final String TYPE_INT = TypeKind.INT.typeName();
    public static final String TYPE_FLOAT = TypeKind.FLOAT.typeName();
    public static final String TYPE_DECIMAL = TypeKind.DECIMAL.typeName();
    public static final String TYPE_XML = TypeKind.XML.typeName();
    public static final String TYPE_BYTE = TypeKind.BYTE.typeName();
    public static final String TYPE_ERROR = TypeKind.ERROR.typeName();
    public static final String TYPE_JSON = TypeKind.JSON.typeName();
    public static final String TYPE_ANY = TypeKind.ANY.typeName();
    public static final String TYPE_ANYDATA = TypeKind.ANYDATA.typeName();
    public static final String TYPE_FUNCTION = TypeKind.FUNCTION.typeName();
    public static final String TYPE_FUTURE = TypeKind.FUTURE.typeName();
    public static final String TYPE_TYPEDESC = TypeKind.TYPEDESC.typeName();
    public static final String TYPE_HANDLE = TypeKind.HANDLE.typeName();
    public static final String TYPE_STREAM = TypeKind.STREAM.typeName();
    public static final String TYPE_NEVER = TypeKind.NEVER.typeName();
    public static final String TYPE_READONLY = TypeKind.READONLY.typeName();

    private TypesGenerator() {
        this.builtinTypeSymbols = new LinkedHashMap<>();
        this.builtinTypeCompletionItems = new LinkedHashMap<>();
    }

    public Either<List<CompletionItem>, CompletionList> getTypes(SemanticModel semanticModel) {
        List<CompletionItem> completionItems = semanticModel.moduleSymbols().parallelStream()
                .filter(symbol -> TYPE_SYMBOL_KINDS.contains(symbol.kind()))
                .map(symbol -> TypeCompletionItemBuilder.build(symbol, symbol.getName().orElse(""), USER_DEFINED_TYPE))
                .collect(Collectors.toCollection(ArrayList::new));
        initializeBuiltinTypes(semanticModel);
        completionItems.addAll(builtinTypeCompletionItems.values());
        return Either.forLeft(completionItems);
    }

    public Optional<TypeSymbol> getTypeSymbol(SemanticModel semanticModel, String typeName) {
        initializeBuiltinTypes(semanticModel);
        return Optional.ofNullable(builtinTypeSymbols.get(typeName));
    }

    private void initializeBuiltinTypes(SemanticModel semanticModel) {
        if (!builtinTypeSymbols.isEmpty()) {
            return;
        }

        // Obtain the type symbols for the builtin types
        Types types = semanticModel.types();
        builtinTypeSymbols.put(TYPE_STRING, types.STRING);
        builtinTypeSymbols.put(TYPE_BOOLEAN, types.BOOLEAN);
        builtinTypeSymbols.put(TYPE_INT, types.INT);
        builtinTypeSymbols.put(TYPE_FLOAT, types.FLOAT);
        builtinTypeSymbols.put(TYPE_DECIMAL, types.DECIMAL);
        builtinTypeSymbols.put(TYPE_XML, types.XML);
        builtinTypeSymbols.put(TYPE_BYTE, types.BYTE);
        builtinTypeSymbols.put(TYPE_ERROR, types.ERROR);
        builtinTypeSymbols.put(TYPE_JSON, types.JSON);
        builtinTypeSymbols.put(TYPE_ANY, types.ANY);
        builtinTypeSymbols.put(TYPE_ANYDATA, types.ANYDATA);
        builtinTypeSymbols.put(TYPE_FUNCTION, types.FUNCTION);
        builtinTypeSymbols.put(TYPE_FUTURE, types.FUTURE);
        builtinTypeSymbols.put(TYPE_TYPEDESC, types.TYPEDESC);
        builtinTypeSymbols.put(TYPE_HANDLE, types.HANDLE);
        builtinTypeSymbols.put(TYPE_STREAM, types.STREAM);
        builtinTypeSymbols.put(TYPE_NEVER, types.NEVER);
        builtinTypeSymbols.put(TYPE_READONLY, types.READONLY);

        // Build the completion items for the builtin types
        builtinTypeSymbols.forEach((name, symbol) -> builtinTypeCompletionItems.put(name,
                TypeCompletionItemBuilder.build(symbol, name, PRIMITIVE_TYPE)));
    }

    public static TypesGenerator getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {

        private static final TypesGenerator INSTANCE = new TypesGenerator();
    }
}
