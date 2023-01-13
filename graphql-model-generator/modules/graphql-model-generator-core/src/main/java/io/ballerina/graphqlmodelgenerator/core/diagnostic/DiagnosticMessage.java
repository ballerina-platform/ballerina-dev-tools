package io.ballerina.graphqlmodelgenerator.core.diagnostic;

import io.ballerina.tools.diagnostics.DiagnosticSeverity;

public enum DiagnosticMessage {
    SDL_SCHEMA_100("SDL_SCHEMA_100", "Given Ballerina file contains compilation error(s).", DiagnosticSeverity.ERROR),
    SDL_SCHEMA_101("SDL_SCHEMA_101", "No Ballerina services found with name \"%s\" to generate SDL schema. " +
            "These services are available in ballerina file. %s", DiagnosticSeverity.ERROR),
    SDL_SCHEMA_102("SDL_SCHEMA_102", "SDL Schema generation failed due to an error occurred " +
            "in Ballerina GraphQL Package: %s", DiagnosticSeverity.ERROR),
    SDL_SCHEMA_103("SDL_SCHEMA_103", "SDL schema generation failed: %s", DiagnosticSeverity.ERROR);

    private final String code;
    private final String description;
    private final DiagnosticSeverity severity;

    DiagnosticMessage(String code, String description, DiagnosticSeverity severity) {
        this.code = code;
        this.description = description;
        this.severity = severity;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public DiagnosticSeverity getSeverity() {
        return severity;
    }
}