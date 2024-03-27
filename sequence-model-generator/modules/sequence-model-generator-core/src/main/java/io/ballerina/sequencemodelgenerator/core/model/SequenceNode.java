package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SequenceNode {

    public static final String CONDITION_LABEL = "condition";
    public static final String BODY_LABEL = "body";

    public static final String IF_THEN_LABEL = "Then";
    public static final String IF_ELSE_LABEL = "Else";

    private NodeKind kind;
    private List<Branch> branches;
    private Map<String, Object> properties;
    private LineRange location;

    public SequenceNode(NodeKind kind, List<Branch> branches, Map<String, Object> properties, LineRange location) {
        this.kind = kind;
        this.branches = branches;
        this.properties = properties;
        this.location = location;
    }

    public record Branch(String label, List<SequenceNode> children) {

    }

    public enum NodeKind {
        IF,
        WHILE,
        FOREACH,
        MATCH,
        INTERACTION,
        RETURN
    }

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

        public Builder location(Node node) {
            this.location = node.lineRange();
            return this;
        }

        public SequenceNode build() {
            return new SequenceNode(kind, branches, properties, location);
        }
    }
}
