package io.ballerina.triggermodelgenerator.extension.response;

import io.ballerina.triggermodelgenerator.extension.model.Trigger;

import java.util.Arrays;

public record TriggerModelGenResponse(Trigger trigger, String errorMsg, String stacktrace) {
    public TriggerModelGenResponse() {
        this(null, null, null);
    }

    public TriggerModelGenResponse(Trigger trigger) {
        this(trigger, null, null);
    }

    public TriggerModelGenResponse(Throwable e) {
        this(null, e.toString(), Arrays.toString(e.getStackTrace()));
    }

}
