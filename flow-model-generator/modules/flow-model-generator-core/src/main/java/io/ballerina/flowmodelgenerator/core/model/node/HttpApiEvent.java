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
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

import java.util.Map;

/**
 * Represents the properties of a HttpApiEvent node.
 *
 * @since 2201.9.0
 */
public class HttpApiEvent extends FlowNode {

    public static final String EVENT_HTTP_API_LABEL = "HTTP API";
    private static final String EVENT_HTTP_API_METHOD = "Method";
    private static final String EVENT_HTTP_API_METHOD_KEY = "method";
    private static final String EVENT_HTTP_API_METHOD_DOC = "HTTP Method";
    private static final String EVENT_HTTP_API_PATH = "Path";
    private static final String EVENT_HTTP_API_PATH_KEY = "path";
    private static final String EVENT_HTTP_API_PATH_DOC = "HTTP Path";

    protected HttpApiEvent(Map<String, Expression> nodeProperties) {
        super(EVENT_HTTP_API_LABEL, Kind.EVENT_HTTP_API, true, nodeProperties);
    }

    @Override
    public String toSource() {
        return null;
    }

    public static class Builder extends FlowNode.NodePropertiesBuilder {

        private Expression method;
        private Expression path;

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        public Builder resourceSymbol(ResourceMethodSymbol resourceMethodSymbol) {
            expressionBuilder
                    .key(EVENT_HTTP_API_METHOD)
                    .typeKind(Expression.ExpressionTypeKind.IDENTIFIER)
                    .setEditable()
                    .setDocumentation(EVENT_HTTP_API_METHOD_DOC);
            resourceMethodSymbol.getName().ifPresent(name -> expressionBuilder.value(name));
            this.method = expressionBuilder.build();

            expressionBuilder
                    .key(EVENT_HTTP_API_PATH)
                    .typeKind(Expression.ExpressionTypeKind.URI_PATH)
                    .setEditable()
                    .setDocumentation(EVENT_HTTP_API_PATH_DOC)
                    .value(resourceMethodSymbol.resourcePath().signature());
            this.path = expressionBuilder.build();
            return this;
        }

        @Override
        public FlowNode build() {
            addProperty(EVENT_HTTP_API_METHOD_KEY, method);
            addProperty(EVENT_HTTP_API_PATH_KEY, path);
            return new HttpApiEvent(nodeProperties);
        }
    }
}
