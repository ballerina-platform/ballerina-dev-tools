/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.extension.request;

import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;

/**
 * Represents the request for the flow model getFlowDesignModel API.
 *
 * @param filePath  file path of the source file
 * @param startLine start line of the source range
 * @param endLine   end line of the source range
 * @since 1.4.0
 */
public record FlowModelGeneratorRequest(String filePath, LinePosition startLine, LinePosition endLine) {

    public LineRange lineRange() {
        return LineRange.from(filePath, startLine, endLine);
    }
}
