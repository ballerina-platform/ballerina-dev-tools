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
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeDescTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.flowmodelgenerator.core.central.ConnectorResponse;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.Map;
import java.util.Optional;

/**
 * Common utility functions used in the project.
 *
 * @since 1.4.0
 */
public class CommonUtils {

    /**
     * Removes the quotes from the given string.
     *
     * @param inputString the input string
     * @return the string without quotes
     */
    public static String removeQuotes(String inputString) {
        return inputString.replaceAll("^\"|\"$", "");
    }

    public static String getTypeSignature(SemanticModel semanticModel, TypeSymbol typeSymbol, boolean ignoreError) {
        return getTypeSignature(semanticModel, typeSymbol, ignoreError, ".");
    }

    public static String getProjectName(Document document) {
        return document.module().descriptor().packageName().value();
    }

    /**
     * Returns the type signature of the given type symbol.
     *
     * @param typeSymbol the type symbol
     * @return the type signature
     */
    public static String getTypeSignature(SemanticModel semanticModel, TypeSymbol typeSymbol, boolean ignoreError,
                                          String defaultModuleName) {
        return switch (typeSymbol.typeKind()) {
            case TYPE_REFERENCE -> {
                TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) typeSymbol;
                yield typeReferenceTypeSymbol.definition().getName()
                        .map(name -> typeReferenceTypeSymbol.getModule()
                                .flatMap(Symbol::getName)
                                .filter(prefix -> !defaultModuleName.equals(prefix))
                                .map(prefix -> prefix + ":" + name)
                                .orElse(name))
                        .orElseGet(() -> getTypeSignature(semanticModel, typeReferenceTypeSymbol.typeDescriptor(),
                                ignoreError, defaultModuleName));
            }
            case UNION -> {
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
                yield unionTypeSymbol.memberTypeDescriptors().stream()
                        .filter(memberType -> !ignoreError || !memberType.subtypeOf(semanticModel.types().ERROR))
                        .map(type -> getTypeSignature(semanticModel, type, ignoreError, defaultModuleName))
                        .reduce((s1, s2) -> s1 + "|" + s2)
                        .orElse(unionTypeSymbol.signature());
            }
            case INTERSECTION -> {
                IntersectionTypeSymbol intersectionTypeSymbol = (IntersectionTypeSymbol) typeSymbol;
                yield intersectionTypeSymbol.memberTypeDescriptors().stream()
                        .map(type -> getTypeSignature(semanticModel, type, ignoreError, defaultModuleName))
                        .reduce((s1, s2) -> s1 + " & " + s2)
                        .orElse(intersectionTypeSymbol.signature());
            }
            case TYPEDESC -> {
                TypeDescTypeSymbol typeDescTypeSymbol = (TypeDescTypeSymbol) typeSymbol;
                yield typeDescTypeSymbol.typeParameter()
                        .map(type -> getTypeSignature(semanticModel, type, ignoreError, defaultModuleName))
                        .orElse(typeDescTypeSymbol.signature());
            }
            case ERROR -> {
                Optional<String> moduleName = typeSymbol.getModule()
                        .map(module -> {
                            String prefix = module.id().modulePrefix();
                            return "annotations".equals(prefix) ? null : prefix;
                        });
                yield moduleName.map(s -> s + ":").orElse("") + typeSymbol.getName().orElse("error");
            }
            default -> {
                Optional<String> moduleName = typeSymbol.getModule().map(module -> module.id().modulePrefix());
                yield moduleName.map(s -> s + ":").orElse("") + typeSymbol.signature();
            }
        };
    }

    /**
     * Returns the module name of the given symbol.
     *
     * @param symbol the symbol to get the module name
     * @return the module name
     */
    public static String getModuleName(Symbol symbol) {
        return symbol.getModule().flatMap(Symbol::getName).orElse("");
    }

    /**
     * Returns the organization name of the given symbol.
     *
     * @param symbol the symbol to get the organization name
     * @return the organization name
     */
    public static String getOrgName(Symbol symbol) {
        return symbol.getModule()
                .map(module -> module.id().orgName())
                .orElse("");
    }

    /**
     * Returns the expression node with check expression if exists.
     *
     * @param expressionNode the expression node
     * @return the expression node with check expression if exists
     */
    public static NonTerminalNode getExpressionWithCheck(NonTerminalNode expressionNode) {
        NonTerminalNode parentNode = expressionNode.parent();
        return parentNode.kind() == SyntaxKind.CHECK_EXPRESSION ? parentNode : expressionNode;
    }

    /**
     * Returns the node in the syntax tree for the given text range.
     *
     * @param syntaxTree the syntax tree in which the node resides
     * @param textRange  the text range of the node
     * @return the node in the syntax tree
     */
    public static NonTerminalNode getNode(SyntaxTree syntaxTree, TextRange textRange) {
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        return modulePartNode.findNode(textRange, true);
    }

    /**
     * Convert the syntax-node line range into a lsp4j range.
     *
     * @param lineRange line range
     * @return {@link Range} converted range
     */
    public static Range toRange(LineRange lineRange) {
        return new Range(toPosition(lineRange.startLine()), toPosition(lineRange.endLine()));
    }

    /**
     * Converts syntax-node line position into a lsp4j position.
     *
     * @param position line position
     * @return {@link Range} converted range
     */
    public static Range toRange(LinePosition position) {
        return new Range(toPosition(position), toPosition(position));
    }

    /**
     * Converts syntax-node line position into a lsp4j position.
     *
     * @param linePosition - line position
     * @return {@link Position} converted position
     */
    public static Position toPosition(LinePosition linePosition) {
        return new Position(linePosition.line(), linePosition.offset());
    }

    /**
     * Get the type symbol of the given node.
     *
     * @param semanticModel the semantic model
     * @param node          the node to get the type symbol
     * @return the type symbol
     */
    public static Optional<TypeSymbol> getTypeSymbol(SemanticModel semanticModel, Node node) {
        if (node.kind() == SyntaxKind.TYPED_BINDING_PATTERN) {
            TypedBindingPatternNode typedBindingPatternNode = (TypedBindingPatternNode) node;
            BindingPatternNode bindingPatternNode = typedBindingPatternNode.bindingPattern();

            Optional<Symbol> typeDescriptorSymbol = semanticModel.symbol(typedBindingPatternNode.typeDescriptor());
            if (typeDescriptorSymbol.isPresent() && typeDescriptorSymbol.get().kind() == SymbolKind.TYPE) {
                return Optional.of((TypeSymbol) typeDescriptorSymbol.get());
            }

            Optional<Symbol> bindingPatternSymbol = semanticModel.symbol(bindingPatternNode);
            if (bindingPatternSymbol.isPresent() && bindingPatternSymbol.get().kind() == SymbolKind.VARIABLE) {
                return Optional.ofNullable(((VariableSymbol) bindingPatternSymbol.get()).typeDescriptor());
            }
        }
        return semanticModel.typeOf(node);
    }

    /**
     * Get the variable name from the given node.
     *
     * @param node the node to get the variable name
     * @return the variable name
     */
    public static String getVariableName(Node node) {
        if (node.kind() == SyntaxKind.TYPED_BINDING_PATTERN) {
            return ((TypedBindingPatternNode) node).bindingPattern().toString().strip();
        }
        if (node instanceof BuiltinSimpleNameReferenceNode builtinSimpleNameReferenceNode) {
            return builtinSimpleNameReferenceNode.name().text();
        }
        if (node instanceof SimpleNameReferenceNode simpleNameReferenceNode) {
            return simpleNameReferenceNode.name().text();
        }
        return node.toString().strip();
    }

    /**
     * Returns the default value for the given API doc type.
     *
     * @param type the type to get the default value for
     * @return the default value for the given type
     */
    public static String getDefaultValueForType(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case "inclusion", "record" -> "{}";
            case "string" -> "\"\"";
            default -> "";
        };
    }

    /**
     * Checks if the query map has no keyword.
     *
     * @param queryMap the query map to check
     * @return true if the query map has no keyword, false otherwise
     */
    public static boolean hasNoKeyword(Map<String, String> queryMap) {
        return queryMap == null || queryMap.isEmpty() || !queryMap.containsKey("q") || queryMap.get("q").isEmpty();
    }

    /**
     * Get the raw type of the type descriptor. If the type descriptor is a type reference then return the associated
     * type descriptor.
     *
     * @param typeDescriptor type descriptor to evaluate
     * @return {@link TypeSymbol} extracted type descriptor
     */
    public static TypeSymbol getRawType(TypeSymbol typeDescriptor) {
        if (typeDescriptor.typeKind() == TypeDescKind.INTERSECTION) {
            return getRawType(((IntersectionTypeSymbol) typeDescriptor).effectiveTypeDescriptor());
        }
        if (typeDescriptor.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            TypeReferenceTypeSymbol typeRef = (TypeReferenceTypeSymbol) typeDescriptor;
            if (typeRef.typeDescriptor().typeKind() == TypeDescKind.INTERSECTION) {
                return getRawType(((IntersectionTypeSymbol) typeRef.typeDescriptor()).effectiveTypeDescriptor());
            }
            TypeSymbol rawType = typeRef.typeDescriptor();
            if (rawType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                return getRawType(rawType);
            }
            return rawType;
        }
        return typeDescriptor;
    }

    public static Object getTypeConstraint(ConnectorResponse.Parameter param, String typeName) {
        return switch (typeName) {
            case "inclusion" -> param.inclusionType();
            default -> typeName;
        };
    }
}
