package io.ballerina.triggermodelgenerator.extension.response;

import org.eclipse.lsp4j.TextEdit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public record TriggerFunctionResponse(Map<String, List<TextEdit>> textEdits, String errorMsg, String stacktrace) {

    public TriggerFunctionResponse() {
        this(Map.of(), null, null);
    }

    public TriggerFunctionResponse(Map<String, List<TextEdit>> textEdits) {
        this(textEdits, null, null);
    }

    public TriggerFunctionResponse(Throwable e) {
        this(Map.of(), e.toString(), Arrays.toString(e.getStackTrace()));
    }
}
