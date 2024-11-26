package io.ballerina.triggermodelgenerator.extension.response;

import io.ballerina.triggermodelgenerator.extension.model.Service;
import io.ballerina.triggermodelgenerator.extension.model.TriggerBasicInfo;

import java.util.Arrays;

public record TriggerSvcModelGenResponse(TriggerBasicInfo triggerInfo, Service service, String errorMsg, String stacktrace) {
    public TriggerSvcModelGenResponse() {
        this(null, null, null, null);
    }

    public TriggerSvcModelGenResponse(TriggerBasicInfo triggerInfo, Service service) {
        this(triggerInfo, service, null, null);
    }

    public TriggerSvcModelGenResponse(Throwable e) {
        this(null, null, e.toString(), Arrays.toString(e.getStackTrace()));
    }

}
