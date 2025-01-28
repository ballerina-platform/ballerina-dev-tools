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
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.langserver.common.utils.PositionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Generates types of the visible variable types for the given cursor position.
 *
 * @since 2.0.0
 */
public class VisibleVariableTypesGenerator {

    private final SemanticModel semanticModel;
    private final Document document;
    private final LinePosition position;
    private final Gson gson;

    public VisibleVariableTypesGenerator(SemanticModel semanticModel, Document document, LinePosition position) {
        this.semanticModel = semanticModel;
        this.document = document;
        this.position = position;
        this.gson = new Gson();
    }

    public JsonArray getVisibleVariableTypes() {
        Optional<LineRange> functionLineRange = findFunctionLineRange();
        List<Category.Variable> moduleVariables = new ArrayList<>();
        List<Category.Variable> configurableVariables = new ArrayList<>();
        List<Category.Variable> localVariables = new ArrayList<>();
        List<Category.Variable> parameters = new ArrayList<>();
        List<Category> categories = Arrays.asList(
                new Category(Category.MODULE_CATEGORY, moduleVariables),
                new Category(Category.CONFIGURABLE_CATEGORY, configurableVariables),
                new Category(Category.LOCAL_CATEGORY, localVariables),
                new Category(Category.PARAMETER_CATEGORY, parameters)
        );

        List<Symbol> symbols = semanticModel.visibleSymbols(document, position);
        for (Symbol symbol : symbols) {
            if (symbol.kind() == SymbolKind.VARIABLE) {
                VariableSymbol variableSymbol = (VariableSymbol) symbol;
                String name = variableSymbol.getName().orElse("");
                Type type = Type.fromSemanticSymbol(variableSymbol);

                if (variableSymbol.qualifiers().contains(Qualifier.CONFIGURABLE)) {
                    configurableVariables.add(new Category.Variable(name, type));
                } else if (functionLineRange.isPresent() &&
                        isInFunctionRange(variableSymbol, functionLineRange.get())) {
                    localVariables.add(new Category.Variable(name, type));
                } else {
                    moduleVariables.add(new Category.Variable(name, type));
                }
            } else if (symbol.kind() == SymbolKind.PARAMETER) {
                String name = symbol.getName().orElse("");
                Type type = Type.fromSemanticSymbol(symbol);
                parameters.add(new Category.Variable(name, type));
            }
        }

        categories.forEach(category -> Collections.sort(category.types()));
        return gson.toJsonTree(categories).getAsJsonArray();
    }

    private boolean isInFunctionRange(VariableSymbol variableSymbol, LineRange functionLineRange) {
        return variableSymbol.getLocation().isPresent() &&
                PositionUtil.isWithinLineRange(variableSymbol.getLocation().get().lineRange(), functionLineRange);
    }

    private Optional<LineRange> findFunctionLineRange() {
        ModulePartNode rootNode = document.syntaxTree().rootNode();
        NonTerminalNode parent =
                rootNode.findNode(TextRange.from(document.textDocument().textPositionFrom(position), 0));
        while (parent != null && notDefinitionKind(parent.kind())) {
            parent = parent.parent();
        }
        return parent == null ? Optional.empty() : Optional.of(parent.lineRange());
    }

    private static boolean notDefinitionKind(SyntaxKind kind) {
        return kind != SyntaxKind.FUNCTION_DEFINITION && kind != SyntaxKind.RESOURCE_ACCESSOR_DEFINITION;
    }

    /**
     * Represents a category of variables.
     *
     * @param name  the name of the category
     * @param types the list of variables in the category
     * @since 2.0.0
     */
    private record Category(String name, List<Variable> types) {

        public static final String MODULE_CATEGORY = "Module Variables";
        public static final String CONFIGURABLE_CATEGORY = "Configurable Variables";
        public static final String LOCAL_CATEGORY = "Local Variables";
        public static final String PARAMETER_CATEGORY = "Parameters";

        public record Variable(String name, Type type) implements Comparable<Variable> {

            @Override
            public int compareTo(Variable o) {
                return this.name.compareTo(o.name);
            }
        }

    }

}
