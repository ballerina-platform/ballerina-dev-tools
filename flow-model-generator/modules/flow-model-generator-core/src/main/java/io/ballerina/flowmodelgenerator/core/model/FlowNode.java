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
import io.ballerina.flowmodelgenerator.core.model.properties.NodeProperties;
import io.ballerina.tools.text.LineRange;

import java.util.Objects;

/**
 * Represents a node in the flow model.
 *
 * @param id             unique identifier of the node
 * @param label          label of the node
 * @param lineRange      line range of the node
 * @param kind           kind of the node
 * @param returning      whether the node is returning
 * @param terminating    whether the node is terminating
 * @param fixed          whether the node is fixed
 * @param nodeProperties properties that are specific to the node kind
 * @since 2201.9.0
 */
public record FlowNode(String id, String label, LineRange lineRange, NodeKind kind, boolean returning,
                       boolean terminating,
                       boolean fixed, NodeProperties nodeProperties) {

    public enum NodeKind {
        EVENT_HTTP_API,
        IF,
        HTTP_API_GET_CALL,
        HTTP_API_POST_CALL,
        RETURN
    }

    /**
     * Represents a builder for the flow node.
     *
     * @since 2201.9.0
     */
    public static class Builder {

        private String label;
        private LineRange lineRange;
        private NodeKind kind;
        private boolean returning;
        private boolean terminating;
        private boolean fixed;
        private NodeProperties nodeProperties;

        public void label(String label) {
            this.label = label;
        }

        public void lineRange(Node node) {
            this.lineRange = node.lineRange();
        }

        public void kind(NodeKind kind) {
            this.kind = kind;
        }

        public void returning(boolean returning) {
            this.returning = returning;
        }

        public void terminating(boolean terminating) {
            this.terminating = terminating;
        }

        public void fixed(boolean fixed) {
            this.fixed = fixed;
        }

        public void nodeProperties(NodeProperties nodeProperties) {
            this.nodeProperties = nodeProperties;
        }

        public FlowNode build() {
            return new FlowNode(String.valueOf(Objects.hash(lineRange)), label, lineRange, kind, returning, terminating,
                    fixed, nodeProperties);
        }
    }
}