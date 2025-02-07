package io.ballerina.testmanagerservice.extension.model;

import io.ballerina.tools.text.LineRange;

import java.util.List;

/**
 * Node to represent a test function in the test explorer tree.
 *
 * @param functionName name of the function
 * @param lineRange line range of the function
 * @param kind kind of the function
 * @param groups groups of the function
 *
 * @since 2.0.0
 */
public record FunctionTreeNode(String functionName, LineRange lineRange, String kind, List<String> groups) {
}
