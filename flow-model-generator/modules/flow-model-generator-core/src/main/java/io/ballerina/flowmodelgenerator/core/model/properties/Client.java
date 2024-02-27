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

import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.tools.text.LineRange;

import java.util.Objects;

/**
 * Represents a client.
 *
 * @param id        The id of the client
 * @param label     The label of the client
 * @param kind      The kind of the client
 * @param lineRange The line range of the client
 * @param scope     The scope of the client
 * @param value     The value of the client
 * @param flags     The flags of the client
 * @since 2201.9.0
 */
public record Client(String id, String label, ClientKind kind, LineRange lineRange, ClientScope scope, String value,
                     int flags) {

    private final static String OTHER_CLIENT = "Client";

    public enum ClientKind {

        HTTP,
        OTHER
    }

    public enum ClientScope {
        LOCAL,
        OBJECT,
        GLOBAL
    }

    /**
     * Represents the builder for client in the diagram.
     */
    public static class Builder {

        private String label;
        private ClientKind kind;
        private LineRange lineRange;
        private ClientScope scope;
        private String value;
        private int flags;

        public Builder() {
            this.kind = ClientKind.OTHER;
            this.label = OTHER_CLIENT;
            this.scope = ClientScope.GLOBAL;
        }

        public void setTypedBindingPattern(TypedBindingPatternNode typedBindingPatternNode) {
            this.lineRange = typedBindingPatternNode.lineRange();
            this.value = typedBindingPatternNode.bindingPattern().toString();
        }

        public void setVariableSymbol(VariableSymbol variableSymbol) {
            variableSymbol.getLocation().ifPresent(location -> this.lineRange = location.lineRange());
            variableSymbol.getName().ifPresent(name -> this.value = name);
        }

        public void setKind(String type) {
            if (type.equals(HttpGet.HTTP_API_GET_CLIENT_TYPE)) {
                this.kind = ClientKind.HTTP;
                this.label = HttpGet.HTTP_API_GET_CLIENT;
            }
        }

        public void setScope(ClientScope scope) {
            this.scope = scope;
        }

        public Client build() {
            String id = String.valueOf(Objects.hash(lineRange));
            return new Client(id, label, kind, lineRange, scope, value, flags);
        }
    }

}
