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
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.ExpressionList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

/**
 * Represents the properties of a HTTP POST node.
 *
 * @param client     The client of the HTTP POST node
 * @param path       The path of the HTTP POST node
 * @param message    The message of the HTTP POST node
 * @param headers    The headers of the HTTP POST node
 * @param targetType The target type of the HTTP return
 * @param mediaType  The media type of the HTTP POST node
 * @param params     The query parameters of the HTTP POST node
 * @param variable   The variable of the HTTP POST node
 * @since 2201.9.0
 */
public record HttpPost(Expression client, Expression path, Expression message, Expression headers,
                       Expression targetType, Expression mediaType, ExpressionList params, Expression variable)
        implements NodeProperties {

    public static final String HTTP_API_POST_KEY = "HTTP POST";

    public static final String HTTP_API_POST_MESSAGE = "Message";
    public static final String HTTP_API_POST_MESSAGE_TYPE = "http:RequestMessage";
    public static final String HTTP_API_POST_MESSAGE_DOC = "HTTP Post Message";

    public static final String HTTP_API_MEDIA_TYPE = "Media Type";
    public static final String HTTP_API_MEDIA_TYPE_TYPE = "string?";
    public static final String HTTP_API_MEDIA_TYPE_DOC = "HTTP Post Media Type";

    /**
     * Represents a builder for the HTTP POST node properties.
     */
    public static class Builder extends NodePropertiesBuilder {

        private Expression client;
        private Expression paths;
        private Expression headers;
        private Expression targetType;
        private Expression message;
        private Expression mediaType;
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
                        setParamValue(HttpGet.HTTP_API_GET_PATH, paramValue, HttpGet.HTTP_API_GET_PATH_DOC);
                        this.paths = expressionBuilder.build();
                    }
                    case "headers" -> {
                        this.expressionBuilder.type(HttpGet.HTTP_API_GET_HEADERS_TYPE);
                        expressionBuilder.optional(true);
                        expressionBuilder.setEditable();
                        setParamValue(HttpGet.HTTP_API_GET_HEADERS, paramValue, HttpGet.HTTP_API_GET_HEADERS_DOC);
                        this.headers = expressionBuilder.build();
                    }
                    case "targetType" -> {
                        expressionBuilder.value(targetTypeValue);
                        expressionBuilder.type(HttpGet.HTTP_API_GET_TARGET_TYPE_TYPE);
                        expressionBuilder.setEditable();
                        setParamValue(HttpGet.HTTP_API_GET_TARGET_TYPE, paramValue,
                                HttpGet.HTTP_API_GET_TARGET_TYPE_DOC);
                        this.targetType = expressionBuilder.build();
                    }
                    case "message" -> {
                        expressionBuilder.type(HttpPost.HTTP_API_POST_MESSAGE_TYPE);
                        expressionBuilder.setEditable();
                        setParamValue(HttpPost.HTTP_API_POST_MESSAGE, paramValue, HttpPost.HTTP_API_POST_MESSAGE_DOC);
                        this.message = expressionBuilder.build();
                    }
                    case "mediaType" -> {
                        expressionBuilder.type(HttpPost.HTTP_API_MEDIA_TYPE_TYPE);
                        expressionBuilder.setEditable();
                        expressionBuilder.optional(true);
                        setParamValue(HttpPost.HTTP_API_MEDIA_TYPE, paramValue, HttpPost.HTTP_API_MEDIA_TYPE_DOC);
                        this.mediaType = expressionBuilder.build();
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
            expressionBuilder.key(HttpGet.HTTP_API_GET_CLIENT);
            expressionBuilder.type(HttpGet.HTTP_API_GET_CLIENT_TYPE);
            expressionBuilder.value(expressionNode.toString());
            expressionBuilder.typeKind(Expression.ExpressionTypeKind.BTYPE);
            expressionBuilder.setEditable();
            expressionBuilder.setDocumentation(HttpGet.HTTP_API_GET_CLIENT_DOC);
            this.client = expressionBuilder.build();
        }

        public void addTargetTypeValue(NonTerminalNode nonTerminalNode) {
            Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(nonTerminalNode);
            typeSymbol.ifPresent(symbol -> this.targetTypeValue = CommonUtils.getTypeSignature(symbol));
        }

        public void addResourceAccessPath(SeparatedNodeList<Node> nodes) {
            ExpressionList.Builder expressionListBuilder = new ExpressionList.Builder();
            expressionListBuilder.key(HttpGet.HTTP_API_GET_PATH);
            expressionListBuilder.type("http:QueryParamType");
            expressionListBuilder.optional(true);
            expressionBuilder.setDocumentation(HttpGet.HTTP_API_GET_PATH_DOC);

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

        public NodeProperties build() {
            return new HttpPost(client, paths, message, headers, targetType, mediaType, params, variable);
        }
    }

}
