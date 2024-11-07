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

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;

/**
 * Generates information for a node.
 *
 * @since 1.4.0
 */
public class BasicNodeGenerator {

    private final Gson gson;
    private final FlowNode nodeToCheckUsages;

    public BasicNodeGenerator(JsonElement nodeToCheckUsages) {
        this.gson = new Gson();
        this.nodeToCheckUsages = new Gson().fromJson(nodeToCheckUsages, FlowNode.class);
    }

    public LinePosition getNodeStartLocation() {
        LineRange lineRange = nodeToCheckUsages.codedata().lineRange();
        return lineRange.startLine();
    }
}
