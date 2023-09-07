package io.ballerina.sequencemodelgenerator.ls.extension;

public class ModelDiagnostic {
    private boolean isIncompleteModel;
    private String errorMsg;

    public ModelDiagnostic(boolean isIncompleteModel, String errorMsg) {
        this.isIncompleteModel = isIncompleteModel;
        this.errorMsg = errorMsg;
    }
}
