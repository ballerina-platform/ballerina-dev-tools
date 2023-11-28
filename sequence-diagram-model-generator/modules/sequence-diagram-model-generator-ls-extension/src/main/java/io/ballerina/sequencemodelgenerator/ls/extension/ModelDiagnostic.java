package io.ballerina.sequencemodelgenerator.ls.extension;

/**
 * Represents the model diagnostic which consist the validity of the model and the error message.
 *
 * @since 2201.8.0
 */

public class ModelDiagnostic {
    private final boolean isIncompleteModel;
    private final String errorMsg;

    public ModelDiagnostic(boolean isIncompleteModel, String errorMsg) {
        this.isIncompleteModel = isIncompleteModel;
        this.errorMsg = errorMsg;
    }

    public boolean isIncompleteModel() {
        return isIncompleteModel;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
