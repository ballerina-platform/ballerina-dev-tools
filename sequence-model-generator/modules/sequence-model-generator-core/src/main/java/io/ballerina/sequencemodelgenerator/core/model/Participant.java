package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record Participant(String id, String name, ParticipantKind kind, String moduleName, List<SequenceNode> nodes,
                          LineRange location) {

    public enum ParticipantKind {
        FUNCTION,
        WORKER,
        ENDPOINT
    }

    public static class Builder {

        private String name;
        private ParticipantKind kind;
        private String moduleName;
        private List<SequenceNode> nodes;
        private LineRange location;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder kind(ParticipantKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder moduleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public Builder nodes(List<SequenceNode> nodes) {
            this.nodes = new ArrayList<>(nodes);
            return this;
        }

        public Builder location(Node node) {
            this.location = node.lineRange();
            return this;
        }

        public Participant build() {
            String id = String.valueOf(Objects.hash(location));
            return new Participant(id, name, kind, moduleName, nodes, location);
        }
    }
}
