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

package io.ballerina.graphqlmodelgenerator.core.utils;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.graphqlmodelgenerator.core.model.Interaction;
import io.ballerina.stdlib.graphql.commons.types.Field;
import io.ballerina.stdlib.graphql.commons.types.InputValue;
import io.ballerina.stdlib.graphql.commons.types.LinePosition;
import io.ballerina.stdlib.graphql.commons.types.Position;
import io.ballerina.stdlib.graphql.commons.types.Type;
import io.ballerina.stdlib.graphql.commons.types.TypeKind;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.stdlib.graphql.commons.utils.Utils.removeEscapeCharacter;

/**
 * Represents the util functions which is required in generating the graphQL model.
 *
 * @since 2201.5.0
 */
public class ModelGenerationUtils {
    private static final String NON_NULL_FORMAT = "%s!";
    private static final String LIST_FORMAT = "[%s]";
    private static final String ARGS_TYPE_FORMAT = "%s = %s";

    /**
     * Generate the field type in graphql sdl syntax.
     */
    public static String getFormattedFieldType(Type type) {
        if (type.getOfType() == null) {
            return type.getName();
        } else if (type.getKind().equals(TypeKind.NON_NULL)) {
            return getFormattedString(NON_NULL_FORMAT, getFormattedFieldType(type.getOfType()));
        } else if (type.getKind().equals(TypeKind.LIST)) {
            return getFormattedString(LIST_FORMAT, getFormattedFieldType(type.getOfType()));
        } else {
            return type.getName();
        }
    }

    /**
     * Format the given string to match the graphql sdl syntax.
     */
    public static String getFormattedString(String format, String... args) {
        return String.format(format, (Object[]) args);
    }

    /**
     * Get the name of the given field type.
     * This data is used when generating the interactions for a given component.
     */
    public static String getFieldType(Type type) {
        if (type.getOfType() == null) {
            if (type.getKind().equals(TypeKind.SCALAR)) {
                return null;
            }
            return type.getName();
        } else if (type.getKind().equals(TypeKind.NON_NULL)) {
            return getFieldType(type.getOfType());
        } else if (type.getKind().equals(TypeKind.LIST)) {
            return getFieldType(type.getOfType());
        } else {
            return type.getName();
        }
    }

    /**
     * Get the file path of the given field.
     */
    public static String getPathOfFieldType(Type type) {
        if (type.getOfType() == null) {
            if (type.getKind().equals(TypeKind.SCALAR)) {
                return null;
            } else {
                return (type.getPosition() != null ? type.getPosition().getFilePath() : null);
            }
        } else if (type.getKind().equals(TypeKind.NON_NULL)) {
            return getPathOfFieldType(type.getOfType());
        } else if (type.getKind().equals(TypeKind.LIST)) {
            return getPathOfFieldType(type.getOfType());
        }
        return null;
    }


    /**
     * Get the Type of the given parameter.
     */
    public static Type getType(Type type) {
        if (type.getOfType() == null) {
            return type;
        }
        if (type.getKind().equals(TypeKind.NON_NULL)) {
            return getType(type.getOfType());
        } else if (type.getKind().equals(TypeKind.LIST)) {
            return getType(type.getOfType());
        } else {
            return type;
        }
    }

    /**
     * Get service base path from the given service declaration node.
     */
    public static String getServiceBasePath(ServiceDeclarationNode serviceDefinition) {
        StringBuilder currentServiceName = new StringBuilder();
        NodeList<Node> serviceNameNodes = serviceDefinition.absoluteResourcePath();
        for (Node serviceBasedPathNode : serviceNameNodes) {
            currentServiceName.append(removeEscapeCharacter(serviceBasedPathNode.toString()));
        }
        return (currentServiceName.toString().trim());
    }

    /**
     * Get the interaction list for the given field.
     * Here the fields represents remote/resource functions or other components represented in the graphQL model.
     */
    public static List<Interaction> getInteractionList(Field field) {
        List<Interaction> links = new ArrayList<>();
        String link = ModelGenerationUtils.getFieldType(field.getType());
        if (link != null) {
            if (!link.isBlank()) {
                links.add(new Interaction(link, ModelGenerationUtils.getPathOfFieldType(field.getType())));
            }
        }
        return links;
    }

    /**
     * Get the list of interactions for the input value.
     */
    public static List<Interaction> getInteractionList(InputValue inputValue) {
        List<Interaction> links = new ArrayList<>();
        String link = ModelGenerationUtils.getFieldType(inputValue.getType());
        if (link != null) {
            links.add(new Interaction(link, ModelGenerationUtils.getPathOfFieldType(inputValue.getType())));
        }
        return links;
    }

    /**
     * Generate the type of the inputValue in graphql sdl syntax.
     */
    public static String createArgType(InputValue arg) {
        if (arg.getDefaultValue() == null) {
            return getFormattedFieldType(arg.getType());
        } else {
            return getFormattedString(ARGS_TYPE_FORMAT, getFormattedFieldType(arg.getType()), arg.getDefaultValue());
        }
    }

    /**
     * Get the range of the node for the given position.
     * This is used to find the node range for the resource and remote functions in graphQL service.
     */
    public static Position findNodeRange(Position position, SyntaxTree syntaxTree) {
        if (position == null) {
            return null;
        }
        LineRange lineRange = CommonUtil.toLineRange(position);
        Range range = CommonUtil.toRange(lineRange);
        Node methodNode = CommonUtil.findSTNode(range, syntaxTree);
        return new Position(position.getFilePath(),
                new LinePosition(methodNode.lineRange().startLine().line(),
                        methodNode.lineRange().startLine().offset()),
                new LinePosition(methodNode.lineRange().endLine().line(), methodNode.lineRange().endLine().offset()));
    }
}
