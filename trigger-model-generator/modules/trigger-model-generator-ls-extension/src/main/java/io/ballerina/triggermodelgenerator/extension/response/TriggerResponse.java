package io.ballerina.triggermodelgenerator.extension.response;

import io.ballerina.triggermodelgenerator.extension.model.Trigger;

public record TriggerResponse(Trigger trigger) {

    public TriggerResponse() {
        this(null);
    }
}
