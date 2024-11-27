package io.ballerina.triggermodelgenerator.extension.model;

import io.ballerina.tools.text.LineRange;

public class Codedata {
    private LineRange lineRange;
    private boolean inListenerInit;
    private boolean isBasePath;

    public Codedata(LineRange lineRange) {
        this(lineRange, false, false);
    }

    public Codedata(LineRange lineRange, boolean inListenerInit, boolean isBasePath) {
        this.lineRange = lineRange;
        this.inListenerInit = inListenerInit;
        this.isBasePath = isBasePath;
    }

    public LineRange getLineRange() {
        return lineRange;
    }

    public void setLineRange(LineRange lineRange) {
        this.lineRange = lineRange;
    }

    public boolean isInListenerInit() {
        return inListenerInit;
    }

    public void setInListenerInit(boolean inListenerInit) {
        this.inListenerInit = inListenerInit;
    }

    public boolean isBasePath() {
        return isBasePath;
    }

    public void setBasePath(boolean isBasePath) {
        this.isBasePath = isBasePath;
    }
}
