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
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NamedWorkerDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.workermodelgenerator.core.analyzer.Analyzer;
import io.ballerina.workermodelgenerator.core.model.CodeLocation;
import io.ballerina.workermodelgenerator.core.model.Endpoint;
import io.ballerina.workermodelgenerator.core.model.Flow;
import io.ballerina.workermodelgenerator.core.model.FlowJsonBuilder;
import io.ballerina.workermodelgenerator.core.model.WorkerNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private CodeLocation bodyCodeLocation;
    private CodeLocation fileSourceRange;
    private final List<Endpoint> endpoints;
    private final SemanticModel semanticModel;
    private final ModulePartNode modulePartNode;
    private final Map<String, String> endpointMap;

    public FlowBuilder(SemanticModel semanticModel, ModulePartNode modulePartNode, Map<String, String> endpointMap) {
        this.nodes = new ArrayList<>();
        this.semanticModel = semanticModel;
        this.modulePartNode = modulePartNode;
        this.endpointMap = endpointMap;
        this.endpoints = new ArrayList<>();
    }

    @Override
    public void visit(NamedWorkerDeclarationNode namedWorkerDeclarationNode) {
        NodeBuilder nodeBuilder = new NodeBuilder();

        // Set the metadata information of the node
        nodeBuilder.setName(namedWorkerDeclarationNode.workerName().text());
        nodeBuilder.setCodeLocation(namedWorkerDeclarationNode.lineRange().startLine(),
                namedWorkerDeclarationNode.lineRange().endLine());

        // Process the annotation information
        String templateId = "";
        if (namedWorkerDeclarationNode.annotations().size() > 0) {
            AnnotationFinder annotationFinder = new AnnotationFinder();
            namedWorkerDeclarationNode.annotations().get(0).accept(annotationFinder);
            Map<String, String> annotationConfig = annotationFinder.getAnnotationConfig();

            int xCord = 0, yCord = 0;
            for (Map.Entry<String, String> entry : annotationConfig.entrySet()) {
                switch (entry.getKey()) {
                    case Constants.WORKER_TEMPLATE_ID -> {
                        templateId = entry.getValue();
                        nodeBuilder.setTemplateId(templateId);
                    }
                    case Constants.WORKER_X_COORDINATE -> xCord = Integer.parseInt(entry.getValue());
                    case Constants.WORKER_Y_COORDINATE -> yCord = Integer.parseInt(entry.getValue());
                    case Constants.WORKER_METADATA -> nodeBuilder.setMetadata(entry.getValue());
                    //TODO: Handle invalid annotations
                    default -> {
                    }
                }
            }
            nodeBuilder.setCanvasPosition(xCord, yCord);
        }

        // Analyze and build the worker node
        Analyzer analyzer = Analyzer.getAnalyzer(templateId, nodeBuilder, semanticModel, modulePartNode, endpointMap);
        namedWorkerDeclarationNode.workerBody().accept(analyzer);
        nodeBuilder.setProperties(analyzer.buildProperties());
        addNode(nodeBuilder.build());
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        // Obtain the code body location of the flow
        FunctionBodyNode functionBodyNode = functionDefinitionNode.functionBody();
        setBodyCodeLocation(new CodeLocation(functionBodyNode.lineRange().startLine(),
                functionBodyNode.lineRange().endLine()));

        // Process the flow metadata information from the annotations
        Optional<MetadataNode> metadata = functionDefinitionNode.metadata();
        metadata.ifPresent(metadataNode -> metadataNode.accept(this));
        super.visit(functionDefinitionNode);
    }

    @Override
    public void visit(MetadataNode metadataNode) {
        AnnotationFinder annotationFinder = new AnnotationFinder();
        metadataNode.annotations().get(0).accept(annotationFinder);
        Map<String, String> annotationConfig = annotationFinder.getAnnotationConfig();

        for (Map.Entry<String, String> entry : annotationConfig.entrySet()) {
            switch (entry.getKey()) {
                case Constants.FLOW_ID -> setId(entry.getValue());
                case Constants.FLOW_NAME -> setName(entry.getValue());
                //TODO: Handle invalid annotations
                default -> {
                }
            }
        }
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
    public void setBodyCodeLocation(CodeLocation bodyCodeLocation) {
        this.bodyCodeLocation = bodyCodeLocation;
    }

    @Override
    public void setFileSourceRange(CodeLocation fileSourceRange) {
        this.fileSourceRange = fileSourceRange;
    }

    @Override
    public void addEndpoint(Endpoint endpoint) {
        this.endpoints.add(endpoint);
    }

    @Override
    public Flow build() {
        return new Flow(id, name, filePath, bodyCodeLocation, fileSourceRange, endpoints, nodes);
    }
}
