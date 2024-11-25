package io.ballerina.triggermodelgenerator.extension.request;

import java.util.List;

public record TriggerListRequest(String organization, String packageName, String query, String keyWord) {
}
