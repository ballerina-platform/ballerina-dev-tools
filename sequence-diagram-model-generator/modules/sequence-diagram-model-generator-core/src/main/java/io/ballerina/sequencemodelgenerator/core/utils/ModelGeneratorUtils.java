package io.ballerina.sequencemodelgenerator.core.utils;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.sequencemodelgenerator.core.model.Participant;

import java.util.List;

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

    public static Participant getParticipantByID(String id, List<Participant> participants) {
        for (Participant participant : participants) {
            if (participant.getId().equals(id)) {
                return participant;
            }
        }
        return null;
    }

    public static boolean isInParticipantList(String participantID, List<Participant> participants) {
        for (Participant participant : participants) {
            if (participant.getId().equals(participantID)) {
                return true;
            }
        }
        return false;
    }

    public static String generateModuleIDFromSymbol(Symbol symbol) {
        if (symbol.getModule().isPresent()) {
            return symbol.getModule().get().id().toString().trim().replace(":", "_");
        }
        return null;
    }

    public static String generateFunctionID(Symbol symbol, FunctionDefinitionNode functionDefinitionNode) {
        String moduleID = generateModuleIDFromSymbol(symbol);
        if (moduleID == null) {
            return null;
        }
        String functionName = functionDefinitionNode.functionName().text().trim();
        return moduleID + "_" + functionName;
    }

    public static String generateEndpointID(ObjectTypeSymbol objectTypeSymbol, NameReferenceNode nameReferenceNode) {
        if (objectTypeSymbol.getModule().isPresent()) {
            String clientPkgName = objectTypeSymbol.getModule().get().id().toString().trim().replace(":", "_");
            return clientPkgName + "_" + objectTypeSymbol.signature().trim() + "_" + nameReferenceNode.toString().trim();
        }
        return null;
    }

    public static String generateReferenceID(Symbol symbol, String functionName) {
        String moduleID = generateModuleIDFromSymbol(symbol);
        if (moduleID == null) {
            return null;
        }
        return moduleID + "_" + functionName.trim();
    }

    public static boolean isStatementBlockCommentPresent(Node node) {
        if (!node.leadingMinutiae().isEmpty()) {
            for (Minutiae minutiae : node.leadingMinutiae()) {
                if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
                    if (minutiae.text().contains("@sq-comment:")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isHiddenInSequenceFlagPresent(Node node) {
        if (!node.leadingMinutiae().isEmpty()) {
            for (Minutiae minutiae : node.leadingMinutiae()) {
                if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
                    if (minutiae.text().contains("@sq-ignore")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String extractBlockComment(String input) {
        String prefix = "@sq-comment:";
        int startIndex = input.indexOf(prefix);

        if (startIndex != -1) {
            String extracted = input.substring(startIndex + prefix.length()).trim();
            return extracted;
        }
        return null;
    }

    public static String removeDoubleQuotes(String input) {
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        }
        return input.trim();
    }


}
