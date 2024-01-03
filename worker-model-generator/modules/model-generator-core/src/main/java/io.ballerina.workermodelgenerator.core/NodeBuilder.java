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
