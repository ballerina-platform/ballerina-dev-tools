package io.ballerina.testmanagerservice.extension.model;

import io.ballerina.tools.text.LineRange;

import java.util.List;

public record FunctionTreeNode(String functionName, LineRange lineRange, String kind, List<String> groups) {
}
