package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.sequencemodelgenerator.core.CommonUtil;

public record Expression(String type, String value) {

    public static class Factory {

        private static final String STRING_TYPE = "string";

        public static Expression create(String type, String value) {
            return new Expression(type, value);
        }

        public static Expression create(SemanticModel semanticModel, Node node) {
            String nodeString = node.toString().strip();
            return semanticModel.typeOf(node)
                    .map(symbol -> new Expression(CommonUtil.getTypeSignature(symbol), nodeString))
                    .orElseGet(() -> new Expression(null, nodeString));
        }

        public static Expression create(SemanticModel semanticModel, Node typeNode, Node valueNode) {
            String nodeString = valueNode.toString().strip();
            return semanticModel.typeOf(typeNode)
                    .map(symbol -> new Expression(CommonUtil.getTypeSignature(symbol), nodeString))
                    .orElseGet(() -> new Expression(null, nodeString));
        }

        public static Expression createStringType(Node node) {
            return new Expression(STRING_TYPE, node.toString().strip());
        }

        public static Expression createType(SemanticModel semanticModel, Node node) {
            return semanticModel.typeOf(node)
                    .map(symbol -> new Expression(CommonUtil.getTypeSignature(symbol), null))
                    .orElseGet(() -> null);
        }
    }
}
