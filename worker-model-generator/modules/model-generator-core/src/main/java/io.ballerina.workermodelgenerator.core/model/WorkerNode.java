/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.workermodelgenerator.core.model;

import io.ballerina.workermodelgenerator.core.TemplateKind;

import java.util.List;

/**
 * Represents a worker node in a flow.
 *
 * @param id             id of the node
 * @param templateKind   kind of the node
 * @param codeLocation   location of the node in the source code
 * @param canvasPosition position of the node in the canvas
 * @param inputPorts     input ports of the node
 * @param outputPorts    output ports of the node
 * @since 2201.9.0
 */
public record WorkerNode(String id, TemplateKind templateKind, CodeLocation codeLocation, CanvasPosition canvasPosition,
                         List<InputPort> inputPorts, List<OutputPort> outputPorts) {

}
