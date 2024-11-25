package io.ballerina.triggermodelgenerator.extension.request;

import java.util.List;

public record TriggerRequest(String id, String organization, String packageName, String moduleName, String serviceName, String query, List<String> keyWords) {
}
