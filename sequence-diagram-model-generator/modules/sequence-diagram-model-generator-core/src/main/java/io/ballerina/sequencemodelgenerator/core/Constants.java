package io.ballerina.sequencemodelgenerator.core;

public class Constants {
    public static final String EMPTY_SEMANTIC_MODEL_MSG =
            "Provided Ballerina file path doesn't contain a valid semantic model";
    public static final String INVALID_NODE_MSG = "Couldn't find a valid node at the given position";

    public static final String ISSUE_IN_VISITING_ROOT_NODE = "Error occurred while visiting root node  : %s";
    public static final String UNABLE_TO_FIND_SYMBOL = "Unable to find symbol for the given node";

    public static final String ISSUE_IN_MODEL_GENERATION = "Error occurred while visiting nodes to generate the model : %s";
}
