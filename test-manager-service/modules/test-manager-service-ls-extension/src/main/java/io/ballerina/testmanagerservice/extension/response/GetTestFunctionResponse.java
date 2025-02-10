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

import io.ballerina.testmanagerservice.extension.model.TestFunction;

import java.util.Arrays;

/**
 * Represents a response to get a test function.
 *
 * @param function   test function
 * @param errorMsg   error message if an error occurred
 * @param stacktrace stacktrace of the error
 *
 * @since 2.0.0
 */
public record GetTestFunctionResponse(TestFunction function, String errorMsg, String stacktrace) {

    public static GetTestFunctionResponse get() {
        return new GetTestFunctionResponse(null, null, null);
    }

    public static GetTestFunctionResponse from(TestFunction function) {
        return new GetTestFunctionResponse(function, null, null);
    }

    public static GetTestFunctionResponse from(Throwable e) {
        return new GetTestFunctionResponse(null, e.toString(), Arrays.toString(e.getStackTrace()));
    }
}
