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
import io.ballerina.compiler.api.Types;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.langserver.common.utils.PositionUtil;

import java.util.*;

/**
 * Generates types of the visible variable types for the given cursor position.
 *
 * @since 1.4.0
 */
public class DataMapper {

    private final SemanticModel semanticModel;
    private final Document document;
    private final Gson gson;

    public DataMapper(SemanticModel semanticModel, Document document) {
        this.semanticModel = semanticModel;
        this.document = document;
        this.gson = new Gson();
    }

    public JsonArray getTypes(JsonElement node) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        NodeKind nodeKind = flowNode.codedata().node();
        List<Type> types = new ArrayList<>();
        if (nodeKind == NodeKind.VARIABLE) {
            Types typesSet = semanticModel.types();
            Type t = Type.fromSemanticSymbol(semanticModel.moduleSymbols().get(1));
            Property property = flowNode.properties().get(Property.DATA_TYPE_KEY);
            Optional<Symbol> typeByName = typesSet.getTypeByName("", "", "", property.toSourceCode());
            if (typeByName.isPresent()) {

            }
        } else if (nodeKind == NodeKind.FUNCTION_CALL) {

        }
//        Optional<LineRange> functionLineRange = findFunctionLineRange();
//        List<Category.Variable> moduleVariables = new ArrayList<>();
//        List<Category.Variable> configurableVariables = new ArrayList<>();
//        List<Category.Variable> localVariables = new ArrayList<>();
//        List<Category> categories = Arrays.asList(
//                new Category(Category.MODULE_CATEGORY, moduleVariables),
//                new Category(Category.CONFIGURABLE_CATEGORY, configurableVariables),
//                new Category(Category.LOCAL_CATEGORY, localVariables)
//        );
//
//        semanticModel.visibleSymbols(document, position).stream()
//                .filter(symbol -> symbol.kind() == SymbolKind.VARIABLE)
//                .map(symbol -> (VariableSymbol) symbol)
//                .forEach(variableSymbol -> {
//                    String name = variableSymbol.getName().orElse("");
//                    Type type = Type.fromSemanticSymbol(variableSymbol);
//
//                    if (variableSymbol.qualifiers().contains(Qualifier.CONFIGURABLE)) {
//                        configurableVariables.add(new Category.Variable(name, type));
//                    } else if (functionLineRange.isPresent() &&
//                            isInFunctionRange(variableSymbol, functionLineRange.get())) {
//                        localVariables.add(new Category.Variable(name, type));
//                    } else {
//                        moduleVariables.add(new Category.Variable(name, type));
//                    }
//                });
//
//        categories.forEach(category -> Collections.sort(category.types()));
        return gson.toJsonTree(types).getAsJsonArray();
    }

    /**
     * Represents a category of variables.
     *
     * @param name  the name of the category
     * @param types the list of variables in the category
     * @since 1.4.0
     */
    private record Category(String name, List<Variable> types) {

        public static final String MODULE_CATEGORY = "Module Variables";
        public static final String CONFIGURABLE_CATEGORY = "Configurable Variables";
        public static final String LOCAL_CATEGORY = "Local Variables";

        public record Variable(String name, Type type) implements Comparable<Variable> {

            @Override
            public int compareTo(Variable o) {
                return this.name.compareTo(o.name);
            }
        }

    }

}
