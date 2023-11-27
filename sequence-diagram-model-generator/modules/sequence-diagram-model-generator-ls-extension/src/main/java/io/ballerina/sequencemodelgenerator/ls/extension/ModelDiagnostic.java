package io.ballerina.sequencemodelgenerator.ls.extension;

/**
 * Represents the model diagnostic which consist the validity of the model and the error message.
 *
 * @since 2201.8.0
 */

public class ModelDiagnostic {
    private boolean isIncompleteModel;
    private String errorMsg;

    public ModelDiagnostic(boolean isIncompleteModel, String errorMsg) {
        this.isIncompleteModel = isIncompleteModel;
        this.errorMsg = errorMsg;
    }
}
