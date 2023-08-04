package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static final Map<SyntaxKind, String> TYPE_MAP;

    static {
        Map<SyntaxKind, String> typeMap = new HashMap<>();
        typeMap.put(SyntaxKind.STRING_LITERAL, "string");
        typeMap.put(SyntaxKind.BOOLEAN_LITERAL, "boolean");
        typeMap.put(SyntaxKind.DECIMAL_FLOATING_POINT_LITERAL_TOKEN, "float");
        typeMap.put(SyntaxKind.NUMERIC_LITERAL, "decimal");
        typeMap.put(SyntaxKind.DECIMAL_INTEGER_LITERAL_TOKEN, "float");
        TYPE_MAP = Collections.unmodifiableMap(typeMap);
    }
}
