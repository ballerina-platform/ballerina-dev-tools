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

package io.ballerina.flowmodelgenerator.core.central;

import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.Item;

import java.util.List;

/**
 * The central interface to obtain information about the connectors.
 *
 * @since 1.4.0
 */
public interface Central {

    /**
     * Get the node template for the given codedata.
     *
     * @param node   The node kind
     * @param module The module name
     * @param symbol The symbol name
     * @return The node template
     */
    FlowNode getNodeTemplate(FlowNode.Kind node, String module, String symbol);

    /**
     * Get the available connections.
     *
     * @return The available connections
     */
    List<Item> getAvailableConnections();
}