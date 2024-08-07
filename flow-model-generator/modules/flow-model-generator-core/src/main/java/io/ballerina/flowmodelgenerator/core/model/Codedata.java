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

package io.ballerina.flowmodelgenerator.core.model;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.tools.text.LineRange;

/**
 * Represents the properties that uniquely identifies a node in the diagram.
 *
 * @param node      The kind of the component
 * @param org       The organization which the component belongs to
 * @param module    The module which the component belongs to
 * @param object    The object of the component if it is a method or an action call
 * @param symbol    The symbol of the component
 * @param lineRange The line range of the component
 * @since 1.5.0
 */
public record Codedata(FlowNode.Kind node, String org, String module, String object, String symbol,
                       LineRange lineRange) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(node.toString());
        String[] fields = {org, module, object, symbol};

        for (String field : fields) {
            if (field != null) {
                sb.append(":").append(field);
            }
        }
        return sb.toString();
    }

    public static class Builder<T> extends FacetedBuilder<T> {

        private FlowNode.Kind node;
        private String org;
        private String module;
        private String object;
        private String symbol;
        private LineRange lineRange;

        public Builder(T parentBuilder) {
            super(parentBuilder);
        }

        public Builder<T> node(FlowNode.Kind node) {
            this.node = node;
            return this;
        }

        public Builder<T> org(String org) {
            this.org = org;
            return this;
        }

        public Builder<T> module(String module) {
            this.module = module;
            return this;
        }

        public Builder<T> object(String object) {
            this.object = object;
            return this;
        }

        public Builder<T> symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder<T> lineRange(Node node) {
            this.lineRange = node.lineRange();
            return this;
        }

        public Codedata build() {
            return new Codedata(node, org, module, object, symbol, lineRange);
        }
    }
}
