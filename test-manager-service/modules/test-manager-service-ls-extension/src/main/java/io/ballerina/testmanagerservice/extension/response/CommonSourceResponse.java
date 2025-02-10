/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.testmanagerservice.extension.response;

import org.eclipse.lsp4j.TextEdit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Represents a common response for source related operations.
 *
 * @param textEdits List of text edits
 * @param errorMsg Error message
 * @param stacktrace Stack trace of the error
 *
 * @since 2.0.0
 */
public record CommonSourceResponse(Map<String, List<TextEdit>> textEdits, String errorMsg, String stacktrace) {

    public CommonSourceResponse() {
        this(Map.of(), null, null);
    }

    public CommonSourceResponse(Map<String, List<TextEdit>> textEdits) {
        this(textEdits, null, null);
    }

    public CommonSourceResponse(Throwable e) {
        this(Map.of(), e.toString(), Arrays.toString(e.getStackTrace()));
    }
}
