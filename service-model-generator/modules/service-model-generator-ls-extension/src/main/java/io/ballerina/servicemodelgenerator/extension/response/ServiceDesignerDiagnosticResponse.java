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

package io.ballerina.servicemodelgenerator.extension.response;

import com.google.gson.JsonElement;

import java.util.Arrays;

/**
 * Service designer diagnostic response.
 *
 * @param response the input model enriched with diagnostic information
 * @param errorMsg error message
 * @param stacktrace stack trace of the error
 *
 * @since 2.3.0
 */
public record ServiceDesignerDiagnosticResponse(JsonElement response, String errorMsg, String stacktrace) {

    public ServiceDesignerDiagnosticResponse() {
        this(null, null, null);
    }

    public ServiceDesignerDiagnosticResponse(JsonElement response) {
        this(response, null, null);
    }

    public ServiceDesignerDiagnosticResponse(Throwable e) {
        this(null, e.toString(), Arrays.toString(e.getStackTrace()));
    }
}
