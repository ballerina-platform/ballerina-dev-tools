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
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.flowmodelgenerator.core.model.Expression;

/**
 * Represents the properties of a HttpApiEvent node.
 *
 * @param method The method of the resource
 * @param path   The path of the resource
 * @since 2201.9.0
 */
public record HttpApiEvent(Expression method, Expression path) implements NodeProperties {

    public static final String EVENT_HTTP_API_KEY = "HTTP API";
    public static final String EVENT_HTTP_API_METHOD = "Method";
    public static final String EVENT_HTTP_API_METHOD_DOC = "HTTP Method";
    public static final String EVENT_HTTP_API_PATH = "Path";
    public static final String EVENT_HTTP_API_PATH_DOC = "HTTP Path";

    public static class Builder extends NodePropertiesBuilder {

        private Expression method;
        private Expression path;

        public Builder(SemanticModel semanticModel) {
            super(semanticModel);
        }

        public void setSymbol(ResourceMethodSymbol resourceMethodSymbol) {
            expressionBuilder.key("Method");
            expressionBuilder.typeKind(Expression.ExpressionTypeKind.IDENTIFIER);
            expressionBuilder.setEditable();
            resourceMethodSymbol.getName().ifPresent(name -> expressionBuilder.value(name));
            this.method = expressionBuilder.build();

            expressionBuilder.key("Path");
            expressionBuilder.typeKind(Expression.ExpressionTypeKind.URI_PATH);
            expressionBuilder.setEditable();
            expressionBuilder.value(resourceMethodSymbol.resourcePath().signature());
            this.path = expressionBuilder.build();
        }

        @Override
        public NodeProperties build() {
            return new HttpApiEvent(method, path);
        }
    }
}
