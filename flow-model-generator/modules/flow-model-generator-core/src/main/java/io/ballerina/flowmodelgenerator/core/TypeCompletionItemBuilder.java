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

package io.ballerina.flowmodelgenerator.core;

import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.common.utils.SymbolUtil;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionItemLabelDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * This class is being used to build Type completion item.
 * TODO: This is a copy of the implementation used in the the language-server core. We need to refactor the
 *  visibility of that class to reuse the implementation.
 *
 * @since 2.0.0
 */
public final class TypeCompletionItemBuilder {

    private TypeCompletionItemBuilder() {
    }

    /**
     * Creates and returns a completion item.
     *
     * @param bSymbol Symbol or null
     * @param label   label
     * @param detail  detail
     * @return {@link CompletionItem}
     */
    public static CompletionItem build(Symbol bSymbol, String label, String detail) {
        CompletionItemLabelDetails completionItemLabelDetails = new CompletionItemLabelDetails();
        completionItemLabelDetails.setDetail(detail);
        CompletionItem item = new CompletionItem();
        item.setLabel(label);
        String insertText = CommonUtil.escapeSpecialCharsInInsertText(label);
        item.setInsertText(insertText);
        setMeta(item, bSymbol, completionItemLabelDetails);
        item.setLabelDetails(completionItemLabelDetails);
        return item;
    }

    private static void setMeta(CompletionItem item, Symbol bSymbol,
                                CompletionItemLabelDetails completionItemLabelDetails) {
        if (bSymbol == null) {
            item.setKind(CompletionItemKind.Unit);
            completionItemLabelDetails.setDescription("type");
            return;
        }
        if (bSymbol.kind() == SymbolKind.MODULE) {
            // package
            item.setKind(CompletionItemKind.Module);
            return;
        }
        if (bSymbol.kind() == SymbolKind.ENUM) {
            item.setKind(CompletionItemKind.Enum);
            completionItemLabelDetails.setDescription("enum");
            return;
        }
        Optional<? extends TypeSymbol> typeDescriptor = SymbolUtil.getTypeDescriptor(bSymbol);
        typeDescriptor = (typeDescriptor.isPresent() && typeDescriptor.get().typeKind() == TypeDescKind.TYPE_REFERENCE)
                ? Optional.of(((TypeReferenceTypeSymbol) typeDescriptor.get()).typeDescriptor()) : typeDescriptor;

        if (typeDescriptor.isEmpty() || typeDescriptor.get().typeKind() == null || typeDescriptor.get().typeKind() ==
                TypeDescKind.COMPILATION_ERROR) {
            item.setKind(CompletionItemKind.Unit);
            completionItemLabelDetails.setDescription("type");
            return;
        }

        //Or, else
         /*else if (typeDescKind.isPresent() && typeDescKind.get() == TypeDescKind.FINITE) {
            // Finite types
            item.setKind(CompletionItemKind.TypeParameter);
        }*/
        switch (typeDescriptor.get().typeKind()) {
            case UNION:
                // Union types
                List<TypeSymbol> memberTypes = new ArrayList<>(((UnionTypeSymbol) typeDescriptor.get())
                        .memberTypeDescriptors());

                // To handle cases where the source is incomplete and hence results in an empty union
                if (memberTypes.isEmpty()) {
                    item.setKind(CompletionItemKind.Unit);
                    completionItemLabelDetails.setDescription("type");
                    return;
                }

                boolean allMatch = memberTypes.stream()
                        .allMatch(typeDesc -> typeDesc.typeKind() == memberTypes.get(0).typeKind());
                if (allMatch) {
                    switch (memberTypes.get(0).typeKind()) {
                        case ERROR:
                            item.setKind(CompletionItemKind.Event);
                            break;
                        case RECORD:
                            item.setKind(CompletionItemKind.Struct);
                            break;
                        case OBJECT:
                            item.setKind(CompletionItemKind.Interface);
                            break;
                        default:
                            item.setKind(CompletionItemKind.TypeParameter);
                            break;
                    }
                } else {
                    item.setKind(CompletionItemKind.Enum);
                }
                break;
            case RECORD:
                item.setKind(CompletionItemKind.Struct);
                break;
            case OBJECT:
                item.setKind(CompletionItemKind.Interface);
                break;
            case ERROR:
                item.setKind(CompletionItemKind.Event);
                break;
            default:
                item.setKind(CompletionItemKind.TypeParameter);
        }

        Documentable documentableSymbol = bSymbol instanceof Documentable ? (Documentable) bSymbol : null;
        if (documentableSymbol != null && documentableSymbol.documentation().isPresent()
                && documentableSymbol.documentation().get().description().isPresent()) {
            item.setDocumentation(documentableSymbol.documentation().get().description().get());
        }

        // set sub bType
        String name = typeDescriptor.get().kind() == SymbolKind.CLASS
                ? typeDescriptor.get().kind().name()
                : typeDescriptor.get().typeKind().getName();
        String detail = name.substring(0, 1).toUpperCase(Locale.ENGLISH)
                + name.substring(1).toLowerCase(Locale.ENGLISH);
        completionItemLabelDetails.setDescription(detail);
    }
}
