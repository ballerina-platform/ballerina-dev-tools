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

package io.ballerina.flowmodelgenerator.core.model.properties;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.flowmodelgenerator.core.model.Expression;

import java.util.Optional;

/**
 * Represents builder for the node properties.
 *
 * @since 2201.9.0
 */
public abstract class NodePropertiesBuilder {

    private static final String VARIABLE_KEY = "Variable";
    private static final String VARIABLE_DOC = "Result Variable";

    protected final SemanticModel semanticModel;
    protected Expression.Builder expressionBuilder;

    protected Expression variable;
    protected Expression expression;

    public NodePropertiesBuilder(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.expressionBuilder = new Expression.Builder();
    }

    public void setVariable(TypedBindingPatternNode typedBindingPatternNode) {
        if (typedBindingPatternNode == null) {
            return;
        }
        BindingPatternNode bindingPatternNode = typedBindingPatternNode.bindingPattern();

        expressionBuilder.key(VARIABLE_KEY);
        expressionBuilder.value(bindingPatternNode.toString());
        expressionBuilder.setEditable();
        expressionBuilder.typeKind(Expression.ExpressionTypeKind.BTYPE);
        expressionBuilder.setDocumentation(VARIABLE_DOC);

        Optional<Symbol> typeDescriptorSymbol = semanticModel.symbol(typedBindingPatternNode.typeDescriptor());
        if (typeDescriptorSymbol.isPresent() && typeDescriptorSymbol.get().kind() == SymbolKind.TYPE) {
            TypeSymbol typeSymbol = (TypeSymbol) typeDescriptorSymbol.get();
            expressionBuilder.type(typeSymbol);
        } else {
            Optional<Symbol> bindingPatternSymbol = semanticModel.symbol(bindingPatternNode);
            if (bindingPatternSymbol.isPresent() && bindingPatternSymbol.get().kind() == SymbolKind.VARIABLE) {
                expressionBuilder.type(((VariableSymbol) bindingPatternSymbol.get()).typeDescriptor());
            }
        }

        this.variable = expressionBuilder.build();
    }

    public void setExpression(ExpressionNode expression) {
        expressionBuilder.key(DefaultExpression.EXPRESSION_RHS_LABEL);
        expressionBuilder.typeKind(Expression.ExpressionTypeKind.BTYPE);
        expressionBuilder.setDocumentation(DefaultExpression.EXPRESSION_RHS_DOC);
        expressionBuilder.setEditable();
        expressionBuilder.value(expression.toSourceCode());
        semanticModel.typeOf(expression).ifPresent(expressionBuilder::type);
        this.expression = expressionBuilder.build();
    }

    /**
     * Builds the node properties.
     *
     * @return node properties
     */
    public abstract NodeProperties build();
}
