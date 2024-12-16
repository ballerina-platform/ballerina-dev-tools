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
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a sequence node in the sequence diagram.
 *
 * @since 2.0.0
 */
public class SequenceNode {

    public static final String CONDITION_LABEL = "condition";

    public static final String IF_THEN_LABEL = "Then";
    public static final String IF_ELSE_LABEL = "Else";
    public static final String BODY_LABEL = "Body";

    private final NodeKind kind;
    private final List<Branch> branches;
    private final Map<String, Object> properties;
    private final LineRange location;

    public SequenceNode(NodeKind kind, List<Branch> branches, Map<String, Object> properties, LineRange location) {
        this.kind = kind;
        this.branches = branches;
        this.properties = properties;
        this.location = location;
    }

    public NodeKind kind() {
        return kind;
    }

    public List<Branch> branches() {
        return branches;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    public LineRange location() {
        return location;
    }

    /**
     * Represents a branch in the sequence node.
     *
     * @param label    Label of the branch
     * @param children Children of the branch
     * @since 2.0.0
     */
    public record Branch(String label, List<SequenceNode> children) {

    }

    /**
     * Represents the kind of the sequence node.
     *
     * @since 2.0.0
     */
    public enum NodeKind {
        IF,
        WHILE,
        FOREACH,
        MATCH,
        INTERACTION,
        RETURN
    }

    /**
     * Represents the builder for the {@link SequenceNode}.
     *
     * @since 2.0.0
     */
    public static class Builder {

        protected final SemanticModel semanticModel;
        protected NodeKind kind;
        protected List<Branch> branches;
        protected Map<String, Object> properties;
        protected LineRange location;

        public Builder(SemanticModel semanticModel) {
            this.semanticModel = semanticModel;
            this.properties = new HashMap<>();
            this.branches = new ArrayList<>();
        }

        public Builder kind(NodeKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder branch(String label, List<SequenceNode> nodes) {
            this.branches.add(new Branch(label, nodes));
            return this;
        }

        public Builder property(String key, List<Expression> value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder property(String key, Expression value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder property(String key, Node node) {
            this.properties.put(key, Expression.Factory.create(semanticModel, node));
            return this;
        }

        public Builder property(String key, String value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder location(Node node) {
            this.location = node.lineRange();
            return this;
        }

        public boolean hasModified() {
            return this.location != null;
        }

        public SequenceNode build() {
            return new SequenceNode(kind, branches, properties, location);
        }
    }
}
