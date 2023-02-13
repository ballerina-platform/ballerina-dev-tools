package io.ballerina.graphqlmodelgenerator.core.utils;


import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.graphqlmodelgenerator.core.model.Interaction;
import io.ballerina.stdlib.graphql.commons.types.*;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.stdlib.graphql.commons.utils.Utils.removeEscapeCharacter;

public class ModelGenerationUtils {
    private static final String NON_NULL_FORMAT = "%s!";
    private static final String LIST_FORMAT = "[%s]";
    private static final String ARGS_TYPE_FORMAT = "%s = %s";

    public static String getFormattedFieldType(Type type) {
        if (type.getKind().equals(TypeKind.NON_NULL)) {
            return getFormattedString(NON_NULL_FORMAT, getFormattedFieldType(type.getOfType()));
        } else if (type.getKind().equals(TypeKind.LIST)) {
            return getFormattedString(LIST_FORMAT, getFormattedFieldType(type.getOfType()));
        } else {
            return type.getName();
        }
    }

    public static String getFormattedString(String format, String... args) {
        return String.format(format, (Object[]) args);
    }

    public static String getFieldType(Type type) {
        if (type.getKind().equals(TypeKind.NON_NULL)) {
            return getFieldType(type.getOfType());
        } else if (type.getKind().equals(TypeKind.LIST)) {
            return getFieldType(type.getOfType());
        } else {
            if (type.getKind().equals(TypeKind.SCALAR)){
                return null;
            } else {
                return type.getName();
            }

        }
    }

    public static Type getType(Type type) {
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

    public static List<Interaction> getInteractionList(Field field){
        List<Interaction> links = new ArrayList<>();
        String link = ModelGenerationUtils.getFieldType(field.getType());
        if (link != null){
            links.add(new Interaction(link));
        }
        return links;
    }

    public static List<Interaction> getInteractionList(InputValue inputValue){
        List<Interaction> links = new ArrayList<>();
        String link = ModelGenerationUtils.getFieldType(inputValue.getType());
        if (link != null){
            links.add(new Interaction(link));
        }
        return links;
    }

    public static String createArgType(InputValue arg) {
        if (arg.getDefaultValue() == null) {
            return getFormattedFieldType(arg.getType());
        } else {
            return getFormattedString(ARGS_TYPE_FORMAT, getFormattedFieldType(arg.getType()), arg.getDefaultValue());
        }
    }

    public static Position findNodeRange(Position position, SyntaxTree syntaxTree) {
        LineRange lineRange = CommonUtil.toLineRange(position);
        Range range = CommonUtil.toRange(lineRange);
        Node methodNode = CommonUtil.findSTNode(range, syntaxTree);
        Position nodePosition = new Position(position.getFilePath(),
                new LinePosition(methodNode.lineRange().startLine().line(), methodNode.lineRange().startLine().offset()),
                new LinePosition(methodNode.lineRange().endLine().line(), methodNode.lineRange().endLine().offset()));
        return  nodePosition;
    }

}
