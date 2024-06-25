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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.flowmodelgenerator.core.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a branch of the node.
 *
 * @param label      label of the branch
 * @param kind       kind of the branch
 * @param children   children of the branch
 * @param properties properties of the branch
 * @since 1.4.0
 */
public record Branch(String label, BranchKind kind, List<FlowNode> children, Map<String, Expression> properties) {

    public static String BODY_LABEL = "Body";
    public static String ON_FAIL_LABEL = "On Fail";

    public enum BranchKind {
        BLOCK
    }

    /**
     * Represents a builder for the branch.
     *
     * @since 1.4.0
     */
    public static class Builder {

        private String label;
        private Branch.BranchKind kind;
        private final List<FlowNode> children;
        private final Map<String, Expression> properties;
        private final SemanticModel semanticModel;

        public Builder(SemanticModel semanticModel) {
            children = new ArrayList<>();
            properties = new HashMap<>();
            this.semanticModel = semanticModel;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder kind(Branch.BranchKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder node(FlowNode node) {
            this.children.add(node);
            return this;
        }

        public Builder nodes(List<FlowNode> nodes) {
            this.children.addAll(nodes);
            return this;
        }

        public Builder variable(Node node) {
            Expression.Builder expressionBuilder = new Expression.Builder();
            if (node == null) {
                return this;
            }
            CommonUtils.getTypeSymbol(semanticModel, node).ifPresent(expressionBuilder::type);
            expressionBuilder
                    .label(FlowNode.NodePropertiesBuilder.VARIABLE_LABEL)
                    .value(CommonUtils.getVariableName(node))
                    .editable()
                    .typeKind(Expression.ExpressionTypeKind.BTYPE)
                    .documentation(FlowNode.NodePropertiesBuilder.VARIABLE_DOC);
            properties.put(FlowNode.NodePropertiesBuilder.VARIABLE_KEY, expressionBuilder.build());
            return this;
        }

        public Branch build() {
            return new Branch(label, kind, children, properties.isEmpty() ? null : properties);
        }
    }
}
