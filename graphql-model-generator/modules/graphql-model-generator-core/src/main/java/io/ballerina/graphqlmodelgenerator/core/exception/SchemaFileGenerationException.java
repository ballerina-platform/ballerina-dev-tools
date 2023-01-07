package io.ballerina.graphqlmodelgenerator.core.exception;

import io.ballerina.graphqlmodelgenerator.core.diagnostic.DiagnosticMessages;
import io.ballerina.graphqlmodelgenerator.core.utils.NullLocation;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.Location;

public class SchemaFileGenerationException extends Exception {
    private final Diagnostic diagnostic;

    public SchemaFileGenerationException(DiagnosticMessages diagnosticMessage, Location location) {
        super(diagnosticMessage.getDescription());
        this.diagnostic = createDiagnostic(diagnosticMessage, location);
    }

    public SchemaFileGenerationException(DiagnosticMessages diagnosticMessage, Location location, String... args) {
        super(generateDescription(diagnosticMessage, args));
        this.diagnostic = createDiagnostic(diagnosticMessage, location, args);
    }

    public String getMessage() {
        return this.diagnostic.toString();
    }

    private static String generateDescription(DiagnosticMessages message, String... args) {
        return String.format(message.getDescription(), (Object[]) args);
    }

    private static Diagnostic createDiagnostic(DiagnosticMessages diagnosticMessage, Location location,
                                               String... args) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(diagnosticMessage.getCode(),
                generateDescription(diagnosticMessage, args), diagnosticMessage.getSeverity());
        if (location == null) {
            location = NullLocation.getInstance();
        }
        Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(diagnosticInfo, location);
        return diagnostic;
    }
}
