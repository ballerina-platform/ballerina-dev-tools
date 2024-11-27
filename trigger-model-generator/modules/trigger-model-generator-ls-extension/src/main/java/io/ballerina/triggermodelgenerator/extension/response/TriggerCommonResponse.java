package io.ballerina.triggermodelgenerator.extension.response;

import org.eclipse.lsp4j.TextEdit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public record TriggerCommonResponse(Map<String, List<TextEdit>> textEdits, String errorMsg, String stacktrace) {

    public TriggerCommonResponse() {
        this(Map.of(), null, null);
    }

    public TriggerCommonResponse(Map<String, List<TextEdit>> textEdits) {
        this(textEdits, null, null);
    }

    public TriggerCommonResponse(Throwable e) {
        this(Map.of(), e.toString(), Arrays.toString(e.getStackTrace()));
    }
}
