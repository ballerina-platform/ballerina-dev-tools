package io.ballerina.workermodelgenerator.core.model;

/**
 * Represents a Ballerina expression.
 *
 * @param expression   Ballerina expression string.
 * @param codeLocation Code location of the expression.
 * @since 2201.9.0
 */
public record BalExpression(String expression, CodeLocation codeLocation) {

}
