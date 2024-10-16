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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import org.ballerinalang.diagramutil.connector.models.connector.Type;

import java.util.List;
import java.util.Optional;

/**
 * Generates types of the data mapper model.
 *
 * @since 1.4.0
 */
public class DataMapper {

    private final SemanticModel semanticModel;
    private final Gson gson;

    public DataMapper(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.gson = new Gson();
    }

    public JsonElement getTypes(JsonElement node, String propertyKey) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        Codedata codedata = flowNode.codedata();
        NodeKind nodeKind = codedata.node();
        if (nodeKind == NodeKind.VARIABLE) {
            String dataType = flowNode.properties().get(Property.DATA_TYPE_KEY).toSourceCode();
            Symbol varSymbol = getSymbol(semanticModel.moduleSymbols(), dataType);
            if (varSymbol == null) {
                throw new IllegalStateException("Symbol cannot be found for : " + dataType);
            }
            Type t = Type.fromSemanticSymbol(varSymbol);
            if (t != null) {
                return gson.toJsonTree(t);
            }
            throw new IllegalStateException("Type cannot be found for : " + propertyKey);
        } else if (nodeKind == NodeKind.FUNCTION_CALL) {
            Symbol varSymbol = getSymbol(semanticModel.moduleSymbols(), codedata.symbol());
            if (varSymbol == null || varSymbol.kind() != SymbolKind.FUNCTION) {
                throw new IllegalStateException("Symbol cannot be found for : " + codedata.symbol());
            }
            Optional<List<ParameterSymbol>> optParams = ((FunctionSymbol) varSymbol).typeDescriptor().params();
            if (optParams.isEmpty()) {
                return new JsonObject();
            }
            for (ParameterSymbol paramSymbol : optParams.get()) {
                Optional<String> optParamName = paramSymbol.getName();
                if (optParamName.isPresent()) {
                    if (optParamName.get().equals(propertyKey)) {
                        Type t = Type.fromSemanticSymbol(paramSymbol);
                        if (t != null) {
                            return gson.toJsonTree(t);
                        }
                    }
                }
            }
            throw new IllegalStateException("Type cannot be found for : " + propertyKey);
        } else {
            throw new IllegalStateException("Unhandled node kind : " + nodeKind.name());
        }
    }

    private Symbol getSymbol(List<Symbol> symbols, String name) {
        for (Symbol symbol : symbols) {
            Optional<String> optSymbolName = symbol.getName();
            if (optSymbolName.isPresent() && optSymbolName.get().equals(name)) {
                return symbol;
            }
        }
        return null;
    }
}
