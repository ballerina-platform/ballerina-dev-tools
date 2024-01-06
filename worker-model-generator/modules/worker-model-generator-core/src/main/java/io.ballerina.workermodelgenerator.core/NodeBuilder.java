/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.workermodelgenerator.core;

import io.ballerina.tools.text.LinePosition;
import io.ballerina.workermodelgenerator.core.model.CanvasPosition;
import io.ballerina.workermodelgenerator.core.model.CodeLocation;
import io.ballerina.workermodelgenerator.core.model.InputPort;
import io.ballerina.workermodelgenerator.core.model.OutputPort;
import io.ballerina.workermodelgenerator.core.model.WorkerNode;
import io.ballerina.workermodelgenerator.core.model.WorkerNodeJsonBuilder;
import io.ballerina.workermodelgenerator.core.model.properties.NodeProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder implementation for creating a {@link WorkerNode} instance.
 *
 * @since 2201.9.0
 */
public class NodeBuilder implements WorkerNodeJsonBuilder {

    // Json variables
    private String id;
    private String templateId;
    private CodeLocation codeLocation;
    private CanvasPosition canvasPosition;
    private final List<InputPort> inputPorts;
    private final List<OutputPort> outputPorts;
    private NodeProperties properties;
    private String metadata;

    public NodeBuilder() {
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
    }

    @Override
    public void setName(String id) {
        this.id = id;
    }

    @Override
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    @Override
    public void addInputPort(String id, String type, String name, String sender) {
        this.inputPorts.add(new InputPort(id, type, name, sender));
    }

    @Override
    public void addOutputPort(String id, String type, String name, String receiver) {
        this.outputPorts.add(new OutputPort(id, type, name, receiver));
    }

    @Override
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    @Override
    public void setCodeLocation(LinePosition start, LinePosition end) {
        this.codeLocation = new CodeLocation(start, end);
    }

    @Override
    public void setCanvasPosition(int x, int y) {
        this.canvasPosition = new CanvasPosition(x, y);
    }

    public void setProperties(NodeProperties properties) {
        this.properties = properties;
    }

    @Override
    public WorkerNode build() {
        return new WorkerNode(id, templateId, codeLocation, canvasPosition, inputPorts, outputPorts, properties,
                metadata);
    }

}
