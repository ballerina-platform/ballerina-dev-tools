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
 * @param node         The kind of the component
 * @param org          The organization which the component belongs to
 * @param module       The module which the component belongs to
 * @param object       The object of the component if it is a method or an action call
 * @param symbol       The symbol of the component
 * @param version      The version of the component
 * @param lineRange    The line range of the component
 * @param sourceCode   The source code of the component
 * @param parentSymbol The parent symbol of the component
 * @param resourcePath The path of the resource function
 * @param id           The unique identifier of the component if exists
 * @param isNew        Whether the component is a node template
 * @param isGenerated  The component is auto generated or not
 * @since 2.0.0
 */
public record Codedata(NodeKind node, String org, String module, String object, String symbol,
                       String version, LineRange lineRange, String sourceCode, String parentSymbol,
                       String resourcePath, Integer id, Boolean isNew, Boolean isGenerated,
                       String inferredReturnType) {

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

    public String getImportSignature() {
        return org + "/" + module;
    }

    public String getModulePrefix() {
        return module.substring(module.lastIndexOf('.') + 1);
    }

    public static class Builder<T> extends FacetedBuilder<T> {

        private NodeKind node;
        private String org;
        private String module;
        private String object;
        private String symbol;
        private String version;
        private LineRange lineRange;
        private String sourceCode;
        private String parentSymbol;
        private String resourcePath;
        private Integer id;
        private Boolean isNew;
        private Boolean isGenerated;
        private String inferredReturnType;

        public Builder(T parentBuilder) {
            super(parentBuilder);
        }

        public Builder<T> node(NodeKind node) {
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

        public Builder<T> version(String version) {
            this.version = version;
            return this;
        }

        public Builder<T> nodeInfo(Node node) {
            this.lineRange = node.lineRange();
            this.sourceCode = node.toSourceCode().strip();
            return this;
        }

        public Builder<T> lineRange(LineRange lineRange) {
            this.lineRange = lineRange;
            return this;
        }

        public Builder<T> sourceCode(String sourceCode) {
            this.sourceCode = sourceCode;
            return this;
        }

        public Builder<T> parentSymbol(String parentSymbol) {
            this.parentSymbol = parentSymbol;
            return this;
        }

        public Builder<T> resourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
            return this;
        }

        public Builder<T> id(int id) {
            this.id = id;
            return this;
        }

        public Builder<T> isNew() {
            this.isNew = true;
            return this;
        }

        public Builder<T> isGenerated(Boolean isGenerated) {
            this.isGenerated = isGenerated;
            return this;
        }

        public Builder<T> inferredReturnType(String inferredReturnType) {
            this.inferredReturnType = inferredReturnType;
            return this;
        }

        public Codedata build() {
            return new Codedata(node, org, module, object, symbol, version, lineRange, sourceCode, parentSymbol,
                    resourcePath, id, isNew, isGenerated, inferredReturnType);
        }
    }
}
