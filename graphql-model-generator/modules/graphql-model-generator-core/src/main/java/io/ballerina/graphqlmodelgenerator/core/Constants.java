package io.ballerina.graphqlmodelgenerator.core;

public class Constants {
    public static final String SCHEMA_STRING_FIELD = "schemaString";
    public static final String SERVICE_CONFIG_IDENTIFIER = "ServiceConfig";
    public static final String SCHEMA_PREFIX = "schema";
    public static final String GRAPHQL_EXTENSION = ".graphql";
    public static final String SLASH = "/";
    public static final String UNDERSCORE = "_";
    public static final String PERIOD = ".";
    public static final String EMPTY_STRING = "";

    public static final String MESSAGE_MISSING_ANNOTATION = "Annotation is missing in GraphQL service";
    public static final String MESSAGE_MISSING_SERVICE_CONFIG = "GraphQL SchemaConfig annotation is missing";
    public static final String MESSAGE_MISSING_FIELD_SCHEMA_STRING =
            "'schemaString' field is missing in GraphQL ServiceConfig";
    public static final String MESSAGE_CANNOT_READ_SCHEMA_STRING = "Cannot read decoded schema string";
    public static final String MESSAGE_INVALID_SCHEMA_STRING = "Invalid schema string found";
    public static final String MESSAGE_MISSING_BAL_FILE = "Provided Ballerina file path does not exist";
    public static final String MESSAGE_CANNOT_READ_BAL_FILE = "Cannot read provided Ballerina file (Permission denied)";

    public static final String INVALID_NODE_MSG = "Provided ST node doesn't support Schema generation";
    public static final String EMPTY_SCHEMA_MSG = "Cannot generate complete Schema object for the provided ST node";
    public static final String EMPTY_SEMANTIC_MODEL_MSG =
            "Provided Ballerina file path doesn't contain a valid semantic model";
    public static final String UNEXPECTED_ERROR_MSG = "Unexpected error occurred while generating GraphQL model : %s";
    public static final String MODEL_GENERATION_ERROR_MSG = "Issue when generating the GraphQL model from Schema : %s";
}
