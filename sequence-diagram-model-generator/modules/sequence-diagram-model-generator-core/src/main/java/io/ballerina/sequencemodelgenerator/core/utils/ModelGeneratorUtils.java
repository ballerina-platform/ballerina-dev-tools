package io.ballerina.sequencemodelgenerator.core.utils;

import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Minutiae;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.sequencemodelgenerator.core.model.DNode;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.ReturnAction;

import java.util.List;

/**
 * Util functions which are specified for the model generation logic.
 *
 * @since 2201.8.0
 */
public class ModelGeneratorUtils {
    public static TypeSymbol getRawType(TypeSymbol typeDescriptor) {
        return typeDescriptor.typeKind() == TypeDescKind.TYPE_REFERENCE
                ? ((TypeReferenceTypeSymbol) typeDescriptor).typeDescriptor() : typeDescriptor;
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

    public static String generateResourceID(Symbol symbol, FunctionDefinitionNode functionDefinitionNode) {
        String moduleID = generateModuleIDFromSymbol(symbol);
        if (moduleID == null) {
            return null;
        }

        StringBuilder resourcePathBuilder = new StringBuilder();
        NodeList<Node> relativeResourcePaths = functionDefinitionNode.relativeResourcePath();
        for (Node path : relativeResourcePaths) {
            resourcePathBuilder.append(path);
        }

        String resourcePath = resourcePathBuilder.toString().trim();
        String method = functionDefinitionNode.functionName().text().trim();
        String completeResourcePath = method + "_" + resourcePath;
        return moduleID + "_" + completeResourcePath;
    }

    public static String generateResourcePath(SeparatedNodeList<Node> accessPathNodes) {
        StringBuilder resourcePathBuilder = new StringBuilder("/");
        for (Node path : accessPathNodes) {
            if (resourcePathBuilder.length() > 1) {
                resourcePathBuilder.append("/");
            }
            resourcePathBuilder.append(path.toSourceCode().trim().replaceAll("\\\\-", "-"));
        }
        return resourcePathBuilder.toString().trim();
    }

    public static String generateMethodID(Symbol symbol, String className,
                                          FunctionDefinitionNode functionDefinitionNode) {
        String moduleID = generateModuleIDFromSymbol(symbol);
        if (moduleID == null) {
            return null;
        }
        String functionName = functionDefinitionNode.functionName().text().trim();
        return moduleID + "_" + className + "_" + functionName;
    }

    public static String generateEndpointID(ObjectTypeSymbol objectTypeSymbol, NameReferenceNode nameReferenceNode) {
        if (objectTypeSymbol.getModule().isPresent()) {
            String clientPkgName = objectTypeSymbol.getModule().get().id().toString().trim()
                    .replace(":", "_");
            return clientPkgName + "_" + objectTypeSymbol.signature().trim() + "_" +
                    nameReferenceNode.toString().trim();
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

    public static String generateReferenceIDForMethods(String methodSignature, String functionName) {
        String moduleID = methodSignature.trim().replace(":", "_");

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
            return input.substring(startIndex + prefix.length()).trim();
        }
        return null;
    }

    public static ReturnAction getModifiedReturnAction(Participant participant, String newTargetId) {
        if (participant.getElementBody() == null) {
            return null;
        }

        for (DNode childElement : participant.getElementBody().getChildElements()) {
            if (childElement instanceof ReturnAction) {
                ReturnAction returnAction = (ReturnAction) childElement;
                return new ReturnAction(
                        returnAction.getSourceId(),
                        newTargetId,
                        returnAction.getName(),
                        returnAction.getType(),
                        returnAction.isHidden(),
                        returnAction.getLocation()
                );
            }
        }
        return null;
    }
}
