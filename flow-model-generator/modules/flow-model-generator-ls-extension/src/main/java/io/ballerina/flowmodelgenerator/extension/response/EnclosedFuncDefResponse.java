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

package io.ballerina.flowmodelgenerator.extension.response;

import io.ballerina.tools.text.LinePosition;

/**
 * Represents the response for the flow model getEnclosedFlowDesignModel API.
 *
 * @since 2.0.0
 */
public class EnclosedFuncDefResponse extends AbstractFlowModelResponse {
    private String filePath;
    private LinePosition startLine;
    private LinePosition endLine;

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setStartLine(LinePosition startLine) {
        this.startLine = startLine;
    }

    public void setEndLine(LinePosition endLine) {
        this.endLine = endLine;
    }

    public String getFilePath() {
        return filePath;
    }

    public LinePosition getStartLine() {
        return startLine;
    }

    public LinePosition getEndLine() {
        return endLine;
    }
}
