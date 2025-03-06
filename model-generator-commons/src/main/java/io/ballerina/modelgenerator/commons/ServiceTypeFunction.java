package io.ballerina.modelgenerator.commons;

import java.util.List;

public record ServiceTypeFunction(
        int functionId,
        String name,
        String description,
        String accessor,
        String kind,
        String returnType,
        int returnTypeEditable,
        String importStatements,
        List<ServiceTypeFunctionParameter> parameters
) {

    public record ServiceTypeFunctionParameter(
            int parameterId,
            String name,
            String label,
            String description,
            String kind,
            String type, // Store JSON as String
            String defaultValue,
            String importStatements
    ) {
    }
}

