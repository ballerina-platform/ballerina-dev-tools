package io.ballerina.sequencemodelgenerator.core.utils;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.*;

import static io.ballerina.sequencemodelgenerator.core.model.Constants.TYPE_MAP;
public class ModelGeneratorUtils {
    public static TypeSymbol getRawType(TypeSymbol typeDescriptor) {
        return typeDescriptor.typeKind() == TypeDescKind.TYPE_REFERENCE
                ? ((TypeReferenceTypeSymbol) typeDescriptor).typeDescriptor() : typeDescriptor;
    }

    public static String getResourcePath(SeparatedNodeList<Node> accessPathNodes, SemanticModel semanticModel) {

        StringBuilder resourcePathBuilder = new StringBuilder();
        for (Node accessPathNode : accessPathNodes) {
            if (resourcePathBuilder.length() > 0) {
                resourcePathBuilder.append("/");
            }
            if (accessPathNode.kind() == SyntaxKind.IDENTIFIER_TOKEN) {
                resourcePathBuilder.append(((IdentifierToken) accessPathNode).text());
            } else if (accessPathNode.kind() == SyntaxKind.COMPUTED_RESOURCE_ACCESS_SEGMENT) {
                ComputedResourceAccessSegmentNode accessSegmentNode =
                        (ComputedResourceAccessSegmentNode) accessPathNode;
                ExpressionNode expressionNode = accessSegmentNode.expression();
                if (expressionNode.kind() == SyntaxKind.STRING_LITERAL) {
                    resourcePathBuilder.append(String.format("[%s]", TYPE_MAP.get(SyntaxKind.STRING_LITERAL)));
                } else if (expressionNode.kind().equals(SyntaxKind.NUMERIC_LITERAL)) {
                    SyntaxKind numericKind = ((BasicLiteralNode) expressionNode).literalToken().kind();
                    if (numericKind.equals(SyntaxKind.DECIMAL_FLOATING_POINT_LITERAL_TOKEN)) {
                        resourcePathBuilder.append(String.format("[%s]", TYPE_MAP.get(
                                SyntaxKind.DECIMAL_FLOATING_POINT_LITERAL_TOKEN)));
                    } else if (numericKind.equals(SyntaxKind.DECIMAL_INTEGER_LITERAL_TOKEN)) {
                        resourcePathBuilder.append(String.format("[%s]", SyntaxKind.DECIMAL_INTEGER_LITERAL_TOKEN));
                    } else {
                        resourcePathBuilder.append(String.format("[%s]", SyntaxKind.NUMERIC_LITERAL));
                    }
                } else if (expressionNode.kind().equals(SyntaxKind.BOOLEAN_LITERAL)) {
                    resourcePathBuilder.append(String.format("[%s]", SyntaxKind.BOOLEAN_LITERAL));
                } else if (expressionNode.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE ||
                        expressionNode.kind() == SyntaxKind.FIELD_ACCESS) {
                    String varType = semanticModel.typeOf(expressionNode).get().signature();
                    resourcePathBuilder.append("[").append(varType.trim()).append("]");
                }
            }
        }
        return resourcePathBuilder.toString();
    }

    public static String getQualifiedNameRefNodeFuncNameText(QualifiedNameReferenceNode nameNode) {
        return nameNode.modulePrefix().text() + ((Token) nameNode.colon()).text() + nameNode.identifier().text();
    }
}
