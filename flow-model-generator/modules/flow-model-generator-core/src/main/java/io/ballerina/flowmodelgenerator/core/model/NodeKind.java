package io.ballerina.flowmodelgenerator.core.model;

/**
 * This is a class used to identify the type of the JSON kind. The kind is used for identifying both nodes and
 * branches.
 *
 * @since 1.4.0
 */
public enum NodeKind {
    // Flow nodes
    EVENT_HTTP_API,
    IF,
    ACTION_CALL,
    FUNCTION_CALL,
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
    LOCK,
    FAIL, NEW_DATA,
    UPDATE_DATA,
    XML_PAYLOAD,
    STOP,
    FOREACH,
    DATA_MAPPER,
    COMMENT,
    SWITCH,
    FUNCTION,

    // Branches
    CONDITIONAL,
    ELSE,
    ON_FAILURE,
    BODY
}
