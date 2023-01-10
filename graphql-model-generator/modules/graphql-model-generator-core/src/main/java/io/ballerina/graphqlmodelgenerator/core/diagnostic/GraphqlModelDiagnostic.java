package io.ballerina.graphqlmodelgenerator.core.diagnostic;

public class GraphqlModelDiagnostic {
    private DiagnosticMessages diagnosticMessage;
    private String filePath;

    public GraphqlModelDiagnostic(DiagnosticMessages diagnosticMessage, String filePath) {
        this.diagnosticMessage = diagnosticMessage;
        this.filePath = filePath;
    }
}
