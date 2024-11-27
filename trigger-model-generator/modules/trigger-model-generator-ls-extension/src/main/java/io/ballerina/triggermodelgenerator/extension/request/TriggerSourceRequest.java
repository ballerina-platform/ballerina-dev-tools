package io.ballerina.triggermodelgenerator.extension.request;

import io.ballerina.triggermodelgenerator.extension.model.Trigger;

public record TriggerSourceRequest(String filePath, Trigger trigger) {
}
