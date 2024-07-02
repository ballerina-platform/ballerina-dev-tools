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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.ExpressionAttributes;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeAttributes;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

/**
 * Represents the generalized action invocation node in the flow model.
 *
 * @since 1.4.0
 */
public class CallNode extends FlowNode {

    protected CallNode(String label, Kind kind, Map<String, Expression> nodeProperties) {
        super(label, kind, false, nodeProperties);
    }

    @Override
    public String toSource() {
        SourceBuilder sourceBuilder = new SourceBuilder();

        Expression variable = getProperty(NodePropertiesBuilder.VARIABLE_KEY);
        if (variable != null) {
            sourceBuilder
                    .expressionWithType(variable)
                    .keyword(SyntaxKind.EQUAL_TOKEN);
        }

        if (returning()) {
            sourceBuilder.keyword(SyntaxKind.RETURN_KEYWORD);
        }

        if (hasFlag(NODE_FLAG_CHECKED)) {
            sourceBuilder.keyword(SyntaxKind.CHECK_KEYWORD);
        }

        NodeAttributes.Info info = NodeAttributes.get(kind());
        Expression client = getProperty(info.callExpression().key());

        sourceBuilder.expression(client)
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(info.key())
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN);

        List<ExpressionAttributes.Info> parameterExpressions = info.parameterExpressions();

        if (!parameterExpressions.isEmpty()) {
            Expression firstParameter = getProperty(parameterExpressions.get(0).key());
            if (firstParameter != null) {
                sourceBuilder.expression(firstParameter);
            }

            boolean hasEmptyParam = false;
            for (int i = 1; i < parameterExpressions.size(); i++) {
                String parameterKey = parameterExpressions.get(i).key();
                Expression parameter = getProperty(parameterKey);

                if (parameter == null || parameter.value() == null) {
                    hasEmptyParam = true;
                    continue;
                }

                sourceBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                if (hasEmptyParam) {
                    sourceBuilder
                            .name(parameterKey)
                            .keyword(SyntaxKind.EQUAL_TOKEN);
                    hasEmptyParam = false;
                }
                sourceBuilder.expression(parameter);
            }
        }

        sourceBuilder
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();

        return sourceBuilder.build(false);
    }

    public static class Builder extends FlowNode.NodePropertiesBuilder {

        private String label;
        private Kind kind;

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        public Builder nodeInfo(NodeAttributes.Info info) {
            this.label = info.label();
            this.kind = info.kind();
            return this;
        }

        public Builder callExpression(ExpressionNode expressionNode, ExpressionAttributes.Info info) {
            Expression client = new Expression.Builder()
                    .label(info.label())
                    .type(info.type())
                    .value(expressionNode.toString())
                    .typeKind(Expression.ExpressionTypeKind.BTYPE)
                    .editable()
                    .documentation(info.documentation())
                    .build();
            addProperty(info.key(), client);
            return this;
        }

        public Builder functionArguments(SeparatedNodeList<FunctionArgumentNode> arguments,
                                         List<ParameterSymbol> parameterSymbols) {
            final Map<String, Node> namedArgValueMap = new HashMap<>();
            final Queue<Node> positionalArgs = new LinkedList<>();

            for (FunctionArgumentNode argument : arguments) {
                switch (argument.kind()) {
                    case NAMED_ARG -> {
                        NamedArgumentNode namedArgument = (NamedArgumentNode) argument;
                        namedArgValueMap.put(namedArgument.argumentName().name().text(),
                                namedArgument.expression());
                    }
                    case POSITIONAL_ARG -> positionalArgs.add(((PositionalArgumentNode) argument).expression());
                    default -> {
                        // Ignore the default case
                    }
                }
            }

            expressionBuilder = new Expression.Builder();
            int numParams = parameterSymbols.size();
            int numPositionalArgs = positionalArgs.size();

            for (int i = 0; i < numParams; i++) {
                ParameterSymbol parameterSymbol = parameterSymbols.get(i);
                Optional<String> name = parameterSymbol.getName();
                if (name.isEmpty()) {
                    continue;
                }
                String parameterName = name.get();
                Node paramValue = i < numPositionalArgs ? positionalArgs.poll() : namedArgValueMap.get(parameterName);

                ExpressionAttributes.Info info = ExpressionAttributes.get(parameterName);
                if (info != null) {
                    expressionBuilder
                            .label(info.label())
                            .documentation(info.documentation())
                            .typeKind(Expression.ExpressionTypeKind.BTYPE)
                            .editable()
                            .optional(parameterSymbol.paramKind() == ParameterKind.DEFAULTABLE);

                    if (paramValue != null) {
                        expressionBuilder.value(paramValue.toSourceCode());
                    }

                    String staticType = info.type();
                    Optional<TypeSymbol> valueType =
                            paramValue != null ? semanticModel.typeOf(paramValue) : Optional.empty();

                    if (info.dynamicType() && valueType.isPresent()) {
                        // Obtain the type from the value if the dynamic type is set
                        expressionBuilder.type(valueType.get());
                    } else if (staticType != null) {
                        // Set the static type
                        expressionBuilder.type(staticType);
                    } else {
                        // Set the type of the symbol if none of types were found
                        expressionBuilder.type(parameterSymbol.typeDescriptor());
                    }

                    addProperty(parameterName, expressionBuilder.build());
                }
            }
            return this;
        }

        @Override
        public FlowNode build() {
            return new CallNode(label, kind, nodeProperties);
        }
    }
}
