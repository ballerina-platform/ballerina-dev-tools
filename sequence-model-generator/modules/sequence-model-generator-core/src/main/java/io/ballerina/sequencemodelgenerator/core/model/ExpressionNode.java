package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.sequencemodelgenerator.core.CommonUtil;

public record ExpressionNode(String type, String value) {

    public static class Factory {

        private static final String STRING_TYPE = "string";

        public static ExpressionNode create(String type, String value) {
            return new ExpressionNode(type, value);
        }

        public static ExpressionNode create(SemanticModel semanticModel, Node node) {
            String nodeString = node.toString();
            return semanticModel.typeOf(node)
                    .map(symbol -> new ExpressionNode(CommonUtil.getTypeSignature(symbol), nodeString))
                    .orElseGet(() -> new ExpressionNode(null, nodeString));
        }

        public static ExpressionNode create(SemanticModel semanticModel, Node typeNode, Node valueNode) {
            String nodeString = valueNode.toString();
            return semanticModel.typeOf(typeNode)
                    .map(symbol -> new ExpressionNode(CommonUtil.getTypeSignature(symbol), nodeString))
                    .orElseGet(() -> new ExpressionNode(null, nodeString));
        }

        public static ExpressionNode createStringType(Node node) {
            return new ExpressionNode(STRING_TYPE, node.toString());
        }

        public static ExpressionNode createType(SemanticModel semanticModel, Node node) {
            return semanticModel.typeOf(node)
                    .map(symbol -> new ExpressionNode(CommonUtil.getTypeSignature(symbol), null))
                    .orElseGet(() -> null);
        }
    }
}
