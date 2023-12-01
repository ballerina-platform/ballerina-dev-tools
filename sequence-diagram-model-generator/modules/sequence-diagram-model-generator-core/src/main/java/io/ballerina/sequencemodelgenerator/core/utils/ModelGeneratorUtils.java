/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

import static io.ballerina.sequencemodelgenerator.core.model.Constants.SQ_COMMENT;
import static io.ballerina.sequencemodelgenerator.core.model.Constants.SQ_IGNORE;

/**
 * Util functions which are specified for the model generation logic.
 *
 * @since 2201.8.5
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
        StringBuilder builder = new StringBuilder();
        return builder.append(moduleID).append("_").append(functionName).toString().trim();
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
        StringBuilder builder = new StringBuilder();
        return builder.append(moduleID).append("_").append(method).append("_").append(resourcePath).toString().trim();
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
        StringBuilder builder = new StringBuilder();
        return builder.append(moduleID).append("_").append(className).append("_").append(functionName).toString()
                .trim();
    }

    public static String generateEndpointID(ObjectTypeSymbol objectTypeSymbol, NameReferenceNode nameReferenceNode) {
        if (objectTypeSymbol.getModule().isPresent()) {
            String clientPkgName = objectTypeSymbol.getModule().get().id().toString().trim()
                    .replace(":", "_");
            StringBuilder builder = new StringBuilder();
            return builder.append(clientPkgName).append("_").append(objectTypeSymbol.signature().trim()).append("_")
                    .append(nameReferenceNode.toString().trim()).toString();
        }
        return null;
    }

    public static String generateReferenceID(Symbol symbol, String functionName) {
        String moduleID = generateModuleIDFromSymbol(symbol);
        if (moduleID == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        return builder.append(moduleID).append("_").append(functionName.trim()).toString();
    }

    public static String generateReferenceIDForMethods(String methodSignature, String functionName) {
        String moduleID = methodSignature.trim().replace(":", "_");
        StringBuilder builder = new StringBuilder();
        return builder.append(moduleID).append("_").append(functionName.trim()).toString();
    }

    public static boolean isStatementBlockCommentPresent(Node node) {
        if (!node.leadingMinutiae().isEmpty()) {
            for (Minutiae minutiae : node.leadingMinutiae()) {
                if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
                    if (minutiae.text().contains(SQ_COMMENT)) {
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
                    if (minutiae.text().contains(SQ_IGNORE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String extractBlockComment(String input) {
        String prefix = SQ_COMMENT;
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
