package io.ballerina.flowmodelgenerator.core.db.model;

public record FunctionResult(
        String name,
        String functionDescription,
        String returnType,
        String packageName,
        String org,
        String version
) {
}
