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

package io.ballerina.flowmodelgenerator.core.utils;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.StreamTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.flowmodelgenerator.core.model.ModuleInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class for types.
 *
 * @since 2.0.0
 */
public class TypeUtils {
    private static final Set<TypeDescKind> BUILT_IN_TYPE_KINDS = Set.of(
            TypeDescKind.INT, TypeDescKind.BYTE, TypeDescKind.FLOAT,
            TypeDescKind.DECIMAL, TypeDescKind.BOOLEAN, TypeDescKind.STRING,
            TypeDescKind.INT_SIGNED8, TypeDescKind.INT_SIGNED16, TypeDescKind.INT_SIGNED32,
            TypeDescKind.INT_UNSIGNED8, TypeDescKind.INT_UNSIGNED16, TypeDescKind.INT_UNSIGNED32,
            TypeDescKind.STRING_CHAR, TypeDescKind.ANYDATA, TypeDescKind.ANY, TypeDescKind.NIL, TypeDescKind.NEVER,
            TypeDescKind.XML, TypeDescKind.XML_COMMENT, TypeDescKind.XML_ELEMENT, TypeDescKind.XML_TEXT,
            TypeDescKind.XML_PROCESSING_INSTRUCTION
    );

    /**
     * Checks if the given type symbol is a built-in type.
     *
     * @param typeSymbol the type symbol to check
     * @return true if the type symbol is a built-in type, false otherwise
     */
    public static boolean isBuiltInType(TypeSymbol typeSymbol) {
        return BUILT_IN_TYPE_KINDS.contains(typeSymbol.typeKind());
    }

    /**
     * Get the type reference ids of the given type symbol.
     *
     * @param typeSymbol the type symbol
     * @param moduleInfo the module information
     * @return the type reference ids
     */
    public static List<String> getTypeRefIds(TypeSymbol typeSymbol, ModuleInfo moduleInfo) {
        List<String> typeRefs = new ArrayList<>();
        addTypeRefIds(typeSymbol, moduleInfo, typeRefs);
        return typeRefs;
    }

    /**
     * Generate the type reference id of the given type-reference symbol.
     *
     * @param typeSymbol the type symbol
     * @param moduleInfo the module information
     * @return the type id
     */
    public static String generateReferencedTypeId(TypeSymbol typeSymbol, ModuleInfo moduleInfo) {
        if (typeSymbol.getName().isEmpty()) {
            return typeSymbol.signature();  // anonymous type
        }

        if (CommonUtils.isWithinPackage(typeSymbol, moduleInfo)) {
            return typeSymbol.getName().get();
        }

        // referred type is not from the given package
        ModuleID moduleId = typeSymbol.getModule().get().id();
        return String.format("%s/%s:%s",
                moduleId.orgName(), moduleId.packageName(), typeSymbol.getName().get());
    }

    private static void addTypeRefIds(TypeSymbol ts, ModuleInfo moduleInfo, List<String> typeRefs) {
        if (isBuiltInType(ts)) {
            return;
        }
        switch (ts.typeKind()) {
            case TYPE_REFERENCE -> {
                typeRefs.add(generateReferencedTypeId(ts, moduleInfo));
            }
            case ARRAY -> {
                addTypeRefIds(((ArrayTypeSymbol) ts).memberTypeDescriptor(), moduleInfo, typeRefs);
            }
            case UNION -> {
                ((UnionTypeSymbol) ts).userSpecifiedMemberTypes().forEach(t -> {
                    addTypeRefIds(t, moduleInfo, typeRefs);
                });
            }
            case MAP -> {
                addTypeRefIds(((MapTypeSymbol) ts).typeParam(), moduleInfo, typeRefs);
            }
            case STREAM -> {
                addTypeRefIds(((StreamTypeSymbol) ts).typeParameter(), moduleInfo, typeRefs);
            }
            case INTERSECTION -> {
                ((IntersectionTypeSymbol) ts).memberTypeDescriptors().forEach(
                        t -> addTypeRefIds(t, moduleInfo, typeRefs));
            }
            default -> {
                typeRefs.add(ts.signature());
            }
        }
    }
}
