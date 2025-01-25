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

package io.ballerina.testmanagerservice.extension.model;

import io.ballerina.tools.text.LineRange;

/**
 * Represents a test function.
 *
 * @param functionName name of the function
 * @param lineRange    line range of the function body
 * @param config       Config annotation values of the function
 *
 * @since 2.0.0
 */
public record TestFunction(String functionName, LineRange lineRange, TestFunctionConfig config) {
}
