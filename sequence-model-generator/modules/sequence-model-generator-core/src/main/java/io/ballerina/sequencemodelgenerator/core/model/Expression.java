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

package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.sequencemodelgenerator.core.CommonUtil;

/**
 * Represents an expression in a sequence node.
 *
 * @param type  type of the expression
 * @param value value of the expression
 * @since 2.0.0
 */
public record Expression(String type, String value) {

    /**
     * Represents a factory to create an {@link Expression} instance.
     *
     * @since 2.0.0
     */
    public static class Factory {

        private static final String STRING_TYPE = "string";

        private Factory() {
        }

        public static Expression create(SemanticModel semanticModel, Node node) {
            String nodeString = getNodeString(node);
            return semanticModel.typeOf(node)
                    .map(symbol -> new Expression(CommonUtil.getTypeSignature(symbol), nodeString))
                    .orElseGet(() -> new Expression(null, nodeString));
        }

        public static Expression create(SemanticModel semanticModel, Node typeNode, Node valueNode) {
            String nodeString = getNodeString(valueNode);
            return semanticModel.typeOf(typeNode)
                    .map(symbol -> new Expression(CommonUtil.getTypeSignature(symbol), nodeString))
                    .orElseGet(() -> new Expression(null, nodeString));
        }

        public static Expression createStringType(Node node) {
            return new Expression(STRING_TYPE, getNodeString(node));
        }

        public static Expression createType(SemanticModel semanticModel, Node node, boolean ignoreNil) {
            return semanticModel.typeOf(node)
                    .filter(symbol -> ignoreNil && symbol.typeKind() != TypeDescKind.NIL)
                    .map(symbol -> new Expression(CommonUtil.getTypeSignature(symbol), null))
                    .orElseGet(() -> null);
        }

        private static String getNodeString(Node node) {
            return node.toString().strip();
        }
    }
}
