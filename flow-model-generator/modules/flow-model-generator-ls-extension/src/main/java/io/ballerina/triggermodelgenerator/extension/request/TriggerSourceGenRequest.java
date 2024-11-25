package io.ballerina.triggermodelgenerator.extension.request;

import io.ballerina.triggermodelgenerator.extension.model.Codedata;
import io.ballerina.triggermodelgenerator.extension.model.Trigger;

public record TriggerSourceGenRequest(String filePath, Trigger trigger) {
}
