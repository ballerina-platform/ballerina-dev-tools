package io.ballerina.triggermodelgenerator.extension.request;

import io.ballerina.triggermodelgenerator.extension.model.Codedata;
import io.ballerina.triggermodelgenerator.extension.model.Trigger;

public record TriggerModifierRequest(String filePath, Trigger trigger, Codedata codedata) {
}
