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
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generates text edits for the nodes that are requested to delete.
 *
 * @since 1.4.0
 */
public class TypesManager {
    private static final Gson gson = new Gson();

    private static Predicate<Symbol> supportedTypesPredicate = symbol -> {
        if (symbol.kind().equals(SymbolKind.ENUM)) {
            return true;
        }

        if (!symbol.kind().equals(SymbolKind.TYPE_DEFINITION)) {
            return false;
        }

        switch (((TypeDefinitionSymbol) symbol).typeDescriptor().typeKind()) {
            case RECORD, ARRAY, TUPLE, UNION, ERROR -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    };

    public static JsonElement getAllTypes(SemanticModel semanticModel) {
        Map<String, Symbol> symbolMap = semanticModel.moduleSymbols().stream()
                .filter(supportedTypesPredicate)
                .collect(Collectors.toMap(symbol -> symbol.getName().orElse(""), symbol -> symbol));

        // Now we have all the defined types in the module scope
        // Now we need to get foreign types that we have defined members of the types
        // e.g: ballerina\time:UTC in Person record as a type of field `dateOfBirth`
        symbolMap.forEach((key, element) -> {
            if (!element.kind().equals(SymbolKind.TYPE_DEFINITION)) {
                return;
            }
            TypeSymbol typeSymbol = ((TypeDefinitionSymbol) element).typeDescriptor();
            addForeignTypes(typeSymbol, symbolMap);
        });

        return gson.toJsonTree(symbolMap.keySet());
    }

    private static void addForeignTypes(TypeSymbol typeSymbol, Map<String, Symbol> symbolMap) {
        // Record
        if (typeSymbol.typeKind().equals(TypeDescKind.RECORD)) {
            RecordTypeSymbol recordTypeSymbol = (RecordTypeSymbol) typeSymbol;

            // Type inclusions
            List<TypeSymbol> inclusions = recordTypeSymbol.typeInclusions();
            inclusions.forEach(inc -> {
                addToForeignSymbolsIfNotAdded(symbolMap, inc);
            });

            // Rest field
            Optional<TypeSymbol> restTypeDescriptor = recordTypeSymbol.restTypeDescriptor();
            if (restTypeDescriptor.isPresent()) {
                TypeSymbol restType = restTypeDescriptor.get();
                addToForeignSymbolsIfNotAdded(symbolMap, restType);
            }
            
            // Field members
            Map<String, RecordFieldSymbol> fieldSymbolMap = recordTypeSymbol.fieldDescriptors();
            fieldSymbolMap.forEach((key, field) -> {
                TypeSymbol ts = field.typeDescriptor();
                addToForeignSymbolsIfNotAdded(symbolMap, ts);
            });
        }

        // Union
        if (typeSymbol.typeKind().equals(TypeDescKind.UNION)) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
            List<TypeSymbol> unionMembers = unionTypeSymbol.userSpecifiedMemberTypes();
            unionMembers.forEach(member -> {
                addToForeignSymbolsIfNotAdded(symbolMap, member);
                // TODO: Handle union within the union type (e.g. type UnionType Color|(Person|User);
                // TODO: Handle different kinds within a union type. (e.g. type UnionType Color|time:UTC[])
            });
        }

        // Array
        if (typeSymbol.typeKind().equals(TypeDescKind.ARRAY)) {
            ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) typeSymbol;
            TypeSymbol arrMemberTypeDesc = arrayTypeSymbol.memberTypeDescriptor();
            if (arrMemberTypeDesc.typeKind().equals(TypeDescKind.ARRAY)
                    || arrMemberTypeDesc.typeKind().equals(TypeDescKind.UNION)) {
                addForeignTypes(arrayTypeSymbol, symbolMap);
            } else {
                addToForeignSymbolsIfNotAdded(symbolMap, arrMemberTypeDesc);
            }
        }
    }

    private static void addToForeignSymbolsIfNotAdded(Map<String, Symbol> foreignSymbols, TypeSymbol inc) {
        if (isForeignType(inc) && !foreignSymbols.containsKey(inc.getName().get())) {
            foreignSymbols.put(inc.getName().get(), inc);
        }
    }

    private static boolean isForeignType(TypeSymbol type) {
        return true;
    }
}
