package io.ballerina.graphqlmodelgenerator.core.diagnostic;

public class DiagnosticUtil {
    public static GraphqlModelDiagnostic createDiagnostic(DiagnosticMessages diagnosticMessage, String filePath) {
        GraphqlModelDiagnostic graphqlModelDiagnostic = new GraphqlModelDiagnostic(diagnosticMessage, filePath);
        return graphqlModelDiagnostic;
    }
}
