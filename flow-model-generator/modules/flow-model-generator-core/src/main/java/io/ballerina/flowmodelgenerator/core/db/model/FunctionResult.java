package io.ballerina.flowmodelgenerator.core.db.model;

public record FunctionResult(
        String name,
        String description,
        String returnType,
        String packageName,
        String org,
        String version
) {
}
