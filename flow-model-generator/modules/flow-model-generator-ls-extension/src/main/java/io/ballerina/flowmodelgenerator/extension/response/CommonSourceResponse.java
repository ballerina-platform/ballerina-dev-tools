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

package io.ballerina.flowmodelgenerator.extension.response;

import org.eclipse.lsp4j.TextEdit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Represents the response for common source.
 *
 * @since 2.0.0
 */
public class CommonSourceResponse extends AbstractFlowModelResponse {

    private Map<String, List<TextEdit>> textEdits;

    public CommonSourceResponse() {
    }

    public Map<String, List<TextEdit>> textEdits() {
        return this.textEdits;
    }

    public void setTextEdits(Map<String, List<TextEdit>> textEdits) {
        this.textEdits = textEdits;
    }
}
