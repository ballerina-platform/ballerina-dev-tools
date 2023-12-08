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

package io.ballerina.workermodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.NamedWorkerDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.workermodelgenerator.core.model.Flow;
import io.ballerina.workermodelgenerator.core.model.FlowJsonBuilder;
import io.ballerina.workermodelgenerator.core.model.WorkerNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builder implementation for creating a {@link Flow} instance.
 *
 * @since 2201.9.0
 */
class FlowBuilder extends NodeVisitor implements FlowJsonBuilder {

    private String id;
    private String name;
    private String filePath;
    private final List<WorkerNode> nodes;
    private final SemanticModel semanticModel;

    public FlowBuilder(String id, String name, String filePath, SemanticModel semanticModel) {
        this.nodes = new ArrayList<>();
        setId(id);
        setName(name);
        setFilePath(filePath);
        this.semanticModel = semanticModel;
    }

    @Override
    public void visit(NamedWorkerDeclarationNode namedWorkerDeclarationNode) {
        NodeBuilder nodeBuilder = new NodeBuilder(semanticModel);

        // Set the metadata information of the node
        nodeBuilder.setName(namedWorkerDeclarationNode.workerName().text());
        nodeBuilder.setCodeLocation(namedWorkerDeclarationNode.lineRange().startLine(),
                namedWorkerDeclarationNode.lineRange().endLine());

        // Process the annotation information
        if (namedWorkerDeclarationNode.annotations().size() > 0) {
            AnnotationFinder annotationFinder = new AnnotationFinder();
            namedWorkerDeclarationNode.annotations().get(0).accept(annotationFinder);
            Map<String, String> annotationConfig = annotationFinder.getAnnotationConfig();

            int xCord = 0, yCord = 0;
            for (Map.Entry<String, String> entry : annotationConfig.entrySet()) {
                switch (entry.getKey()) {
                    case Constants.WORKER_TEMPLATE_ID -> nodeBuilder.setTemplateId(entry.getValue());
                    case Constants.WORKER_X_COORDINATE -> xCord = Integer.parseInt(entry.getValue());
                    case Constants.WORKER_Y_COORDINATE -> yCord = Integer.parseInt(entry.getValue());
                }
            }
            nodeBuilder.setCanvasPosition(xCord, yCord);
        }

        // Analyze the body of the worker
        BlockStatementNode blockStatementNode = namedWorkerDeclarationNode.workerBody();
        blockStatementNode.statements().forEach(statement -> statement.accept(nodeBuilder));
        addNode(nodeBuilder.build());
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void addNode(WorkerNode node) {
        this.nodes.add(node);
    }

    @Override
    public Flow build() {
        return new Flow(id, name, filePath, nodes);
    }
}
