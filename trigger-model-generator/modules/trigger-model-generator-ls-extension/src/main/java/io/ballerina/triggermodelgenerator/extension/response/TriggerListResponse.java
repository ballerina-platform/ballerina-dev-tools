package io.ballerina.triggermodelgenerator.extension.response;

import io.ballerina.triggermodelgenerator.extension.model.TriggerBasicInfo;

import java.util.List;

public record TriggerListResponse(List<TriggerBasicInfo> local) {
}
