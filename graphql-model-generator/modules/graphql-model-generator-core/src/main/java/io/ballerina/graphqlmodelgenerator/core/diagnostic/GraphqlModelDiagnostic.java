package io.ballerina.graphqlmodelgenerator.core.diagnostic;

public class GraphqlModelDiagnostic {
    private DiagnosticMessage diagnosticMessage;
    private String filePath;

    public GraphqlModelDiagnostic(DiagnosticMessage diagnosticMessage, String filePath) {
        this.diagnosticMessage = diagnosticMessage;
        this.filePath = filePath;
    }
}
