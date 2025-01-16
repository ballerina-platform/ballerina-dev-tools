package io.ballerina.flowmodelgenerator.core.model;

/**
 * This is a class used to identify the type of the JSON kind. The kind is used for identifying both nodes and
 * branches.
 *
 * @since 2.0.0
 */
public enum NodeKind {
    // Flow nodes
    EVENT_START,
    IF,
    REMOTE_ACTION_CALL,
    RESOURCE_ACTION_CALL,
    FUNCTION_CALL,
    METHOD_CALL,
    NEW_CONNECTION,
    RETURN,
    EXPRESSION,
    ERROR_HANDLER,
    WHILE,
    CONTINUE,
    BREAK,
    PANIC,
    START,
    TRANSACTION,
    RETRY,
    LOCK,
    FAIL,
    COMMIT,
    ROLLBACK,
    VARIABLE,
    CONFIG_VARIABLE,
    ASSIGN,
    XML_PAYLOAD,
    JSON_PAYLOAD,
    BINARY_DATA,
    STOP,
    FOREACH,
    DATA_MAPPER,
    COMMENT,
    MATCH,
    FUNCTION,
    FORK,
    PARALLEL_FLOW,
    WAIT,

    // Branches
    CONDITIONAL,
    ELSE,
    ON_FAILURE,
    BODY,
    WORKER,

    // Types
    RECORD,
    ENUM,
    ARRAY,
    UNION,
    ERROR,
    DATA_MAPPER_CALL
}
