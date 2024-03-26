package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import java.util.Map;

public abstract class SequenceNode {

    private NodeKind kind;
    private Branch[] branches;
    private Map<String, ExpressionNode> properties;
    private LineRange location;

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

        public SequenceNode build() {
            return null;
        }
    }
}
