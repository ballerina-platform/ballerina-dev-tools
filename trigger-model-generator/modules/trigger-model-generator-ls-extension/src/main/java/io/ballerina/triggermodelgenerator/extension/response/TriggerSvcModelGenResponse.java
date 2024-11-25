package io.ballerina.triggermodelgenerator.extension.response;

import io.ballerina.triggermodelgenerator.extension.model.Service;

import java.util.Arrays;

public record TriggerSvcModelGenResponse(Service service, String errorMsg, String stacktrace) {
    public TriggerSvcModelGenResponse() {
        this(null, null, null);
    }

    public TriggerSvcModelGenResponse(Service service) {
        this(service, null, null);
    }

    public TriggerSvcModelGenResponse(Throwable e) {
        this(null, e.toString(), Arrays.toString(e.getStackTrace()));
    }

}
