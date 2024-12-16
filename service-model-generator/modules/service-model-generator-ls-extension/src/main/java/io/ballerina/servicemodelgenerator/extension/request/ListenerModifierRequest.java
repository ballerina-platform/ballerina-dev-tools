package io.ballerina.servicemodelgenerator.extension.request;

import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Listener;

public record ListenerModifierRequest(String filePath, Listener listener, Codedata codedata) {
}
