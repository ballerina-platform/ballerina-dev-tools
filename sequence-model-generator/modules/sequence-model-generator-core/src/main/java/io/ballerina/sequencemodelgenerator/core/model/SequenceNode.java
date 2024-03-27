package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.sequencemodelgenerator.core.CommonUtil;
import io.ballerina.tools.text.LineRange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SequenceNode {

    private NodeKind kind;
    private Branch[] branches;
    private Map<String, Object> properties;
    private LineRange location;

    public SequenceNode(NodeKind kind, Branch[] branches, Map<String, Object> properties, LineRange location) {
        this.kind = kind;
        this.branches = branches;
        this.properties = properties;
        this.location = location;
    }

    public record Branch(String label, SequenceNode[] children) {

    }

    public enum NodeKind {
        IF,
        WHILE,
        FOREACH,
        MATCH,
        INTERACTION
    }

    public static class Builder {

        protected final SemanticModel semanticModel;

        protected NodeKind kind;
        protected Branch[] branches;
        protected Map<String, Object> properties;
        protected LineRange location;

        public Builder(SemanticModel semanticModel) {
            this.semanticModel = semanticModel;
            this.properties = new HashMap<>();
        }

        public Builder kind(NodeKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder branches(Branch[] branches) {
            this.branches = branches;
            return this;
        }

        public Builder property(String key, List<ExpressionNode> value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder property(String key, ExpressionNode value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder property(String key, Node node) {
            this.properties.put(key, ExpressionNode.Factory.create(semanticModel, node));
            return this;
        }

        public Builder location(Node node) {
            this.location = node.lineRange();
            return this;
        }

        public SequenceNode build() {
            return null;
        }
    }
}
