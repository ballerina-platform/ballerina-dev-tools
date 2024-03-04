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
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.ExpressionList;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

/**
 * Represents the properties of a HTTP GET node.
 *
 * @since 2201.9.0
 */
public class HttpGet extends FlowNode {

    public static final String HTTP_GET_KEY = "get";
    public static final String HTTP_API_GET_LABEL = "HTTP GET";
    public static final String HTTP_API_GET_CLIENT = "Client";
    public static final String HTTP_API_GET_CLIENT_KEY = "client";
    public static final String HTTP_API_GET_CLIENT_TYPE = "http:Client";
    public static final String HTTP_API_GET_CLIENT_DOC = "HTTP Client Connection";
    public static final String HTTP_API_GET_PATH = "Path";
    public static final String HTTP_API_GET_PATH_KEY = "path";
    public static final String HTTP_API_GET_PATH_DOC = "HTTP Path";
    public static final String HTTP_API_GET_HEADERS = "Headers";
    public static final String HTTP_API_GET_HEADERS_KEY = "headers";
    public static final String HTTP_API_GET_HEADERS_DOC = "HTTP Headers";
    public static final String HTTP_API_GET_HEADERS_TYPE = "map<string|string[]>?";
    public static final String HTTP_API_GET_TARGET_TYPE = "Target Type";
    public static final String HTTP_API_GET_TARGET_TYPE_DOC = "HTTP Response Type";
    public static final String HTTP_API_GET_TARGET_TYPE_TYPE = "http:Response|anydata";

    protected HttpGet(Map<String, Expression> nodeProperties) {
        super(HTTP_API_GET_LABEL, Kind.LIBRARY_CALL_HTTP_GET, false, nodeProperties);
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

        Expression client = getProperty(HTTP_API_GET_CLIENT_KEY);
        Expression path = getProperty(HTTP_API_GET_PATH_KEY);

        sourceBuilder
                .expression(client)
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(HTTP_GET_KEY)
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN)
                .expression(path);

        Expression headers = getProperty(HTTP_API_GET_HEADERS_KEY);
        if (headers.value() != null) {
            sourceBuilder
                    .keyword(SyntaxKind.COMMA_TOKEN)
                    .expression(headers);
        }
        sourceBuilder
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();

        return sourceBuilder.build(false);
    }

    /**
     * Represents the builder for HTTP GET node properties.
     *
     * @since 2201.9.0
     */
    public static class Builder extends FlowNode.NodePropertiesBuilder {

        private Expression client;
        private Expression paths;
        private Expression headers;
        private Expression targetType;
        private ExpressionList params;
        private String targetTypeValue;
        private final Map<String, String> namedArgValueMap;
        private final Queue<String> positionalArgs;

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
            this.namedArgValueMap = new HashMap<>();
            this.positionalArgs = new LinkedList<>();
        }

        public void addFunctionArguments(SeparatedNodeList<FunctionArgumentNode> arguments) {
            for (FunctionArgumentNode argument : arguments) {
                switch (argument.kind()) {
                    case NAMED_ARG -> {
                        NamedArgumentNode namedArgument = (NamedArgumentNode) argument;
                        namedArgValueMap.put(namedArgument.argumentName().name().text(),
                                namedArgument.expression().toSourceCode());
                    }
                    case POSITIONAL_ARG ->
                            positionalArgs.add(((PositionalArgumentNode) argument).expression().toSourceCode());
                }
            }
        }

        public void addHttpParameters(List<ParameterSymbol> parameterSymbols) {
            expressionBuilder = new Expression.Builder();
            int numParams = parameterSymbols.size();
            int numPositionalArgs = this.positionalArgs.size();

            for (int i = 0; i < numParams; i++) {
                ParameterSymbol parameterSymbol = parameterSymbols.get(i);
                if (parameterSymbol.getName().isEmpty()) {
                    continue;
                }
                String paramValue = i < numPositionalArgs ? this.positionalArgs.poll() :
                        this.namedArgValueMap.get(parameterSymbol.getName().get());
                switch (parameterSymbol.getName().get()) {
                    case "path" -> {
                        expressionBuilder.type(parameterSymbol.typeDescriptor());
                        expressionBuilder.setEditable();
                        setParamValue(HTTP_API_GET_PATH, paramValue, HTTP_API_GET_PATH_DOC);
                        this.paths = expressionBuilder.build();
                    }
                    case "headers" -> {
                        this.expressionBuilder.type(HTTP_API_GET_HEADERS_TYPE);
                        expressionBuilder.optional(true);
                        expressionBuilder.setEditable();
                        setParamValue(HTTP_API_GET_HEADERS, paramValue, HTTP_API_GET_HEADERS_DOC);
                        this.headers = expressionBuilder.build();
                    }
                    case "targetType" -> {
                        expressionBuilder.value(targetTypeValue);
                        expressionBuilder.type(HTTP_API_GET_TARGET_TYPE_TYPE);
                        expressionBuilder.setEditable();
                        setParamValue(HTTP_API_GET_TARGET_TYPE, paramValue,
                                HTTP_API_GET_TARGET_TYPE_DOC);
                        this.targetType = expressionBuilder.build();
                    }
                }
            }
        }

        private void setParamValue(String path, String paramValue, String doc) {
            expressionBuilder.key(path);
            setParamValue(paramValue);
            expressionBuilder.typeKind(Expression.ExpressionTypeKind.BTYPE);
            expressionBuilder.setDocumentation(doc);
        }

        private void setParamValue(String paramValue) {
            if (paramValue != null) {
                expressionBuilder.value(paramValue);
            }
        }

        public void addClient(ExpressionNode expressionNode) {
            expressionBuilder = new Expression.Builder();
            expressionBuilder.key(HTTP_API_GET_CLIENT);
            expressionBuilder.type(HTTP_API_GET_CLIENT_TYPE);
            expressionBuilder.value(expressionNode.toString());
            expressionBuilder.typeKind(Expression.ExpressionTypeKind.BTYPE);
            expressionBuilder.setEditable();
            expressionBuilder.setDocumentation(HTTP_API_GET_CLIENT_DOC);
            this.client = expressionBuilder.build();
        }

        public void addTargetTypeValue(NonTerminalNode nonTerminalNode) {
            Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(nonTerminalNode);
            typeSymbol.ifPresent(symbol -> this.targetTypeValue = CommonUtils.getTypeSignature(symbol));
        }

        public void addResourceAccessPath(SeparatedNodeList<Node> nodes) {
            ExpressionList.Builder expressionListBuilder = new ExpressionList.Builder();
            expressionListBuilder.key(HTTP_API_GET_PATH);
            expressionListBuilder.type("http:QueryParamType");
            expressionListBuilder.optional(true);
            expressionBuilder.setDocumentation(HTTP_API_GET_PATH_DOC);

            if (nodes != null) {
                for (Node node : nodes) {
                    expressionBuilder.key("param");
                    semanticModel.typeOf(node).ifPresent(expressionBuilder::type);
                    expressionBuilder.value(node.toString());
                    expressionBuilder.typeKind(Expression.ExpressionTypeKind.BTYPE);
                    expressionListBuilder.value(expressionBuilder.build());
                }
            }
            this.params = expressionListBuilder.build();
        }

        @Override
        public FlowNode build() {
            addProperty(HTTP_API_GET_CLIENT_KEY, this.client);
            addProperty(HTTP_API_GET_PATH_KEY, this.paths);
            addProperty(HTTP_API_GET_HEADERS_KEY, this.headers);
            addProperty(VARIABLE_KEY, this.variable);
            return new HttpGet(nodeProperties);
        }
    }
}
