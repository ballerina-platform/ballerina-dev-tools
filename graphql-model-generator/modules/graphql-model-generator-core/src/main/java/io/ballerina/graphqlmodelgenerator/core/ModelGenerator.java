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

package io.ballerina.graphqlmodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.graphqlmodelgenerator.core.exception.GraphqlModelGenerationException;
import io.ballerina.graphqlmodelgenerator.core.model.GraphqlModel;
import io.ballerina.graphqlmodelgenerator.core.model.Service;
import io.ballerina.graphqlmodelgenerator.core.utils.CommonUtil;
import io.ballerina.graphqlmodelgenerator.core.utils.ModelGenerationUtils;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.stdlib.graphql.commons.types.Schema;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.Range;

import java.nio.file.Path;

import static io.ballerina.graphqlmodelgenerator.core.Constants.EMPTY_SCHEMA_MSG;
import static io.ballerina.graphqlmodelgenerator.core.Constants.INVALID_NODE_MSG;
import static io.ballerina.graphqlmodelgenerator.core.Constants.MODEL_GENERATION_ERROR_MSG;
import static io.ballerina.stdlib.graphql.compiler.Utils.getSchemaObject;

/**
 * Represents the root model generator.
 *
 * @since 2201.5.0
 */
public class ModelGenerator {

    public GraphqlModel getGraphqlModel(Project project, LineRange position, SemanticModel semanticModel) throws
            GraphqlModelGenerationException {
        Package packageName = project.currentPackage();
        DocumentId docId;
        Document doc;
        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            Path filePath = Path.of(position.fileName());
            docId = project.documentId(filePath);
            ModuleId moduleId = docId.moduleId();
            doc = project.currentPackage().module(moduleId).document(docId);
        } else {
            Module currentModule = packageName.getDefaultModule();
            docId = currentModule.documentIds().iterator().next();
            doc = currentModule.document(docId);
        }

        SyntaxTree syntaxTree = doc.syntaxTree();
        Range range = CommonUtil.toRange(position);
        NonTerminalNode node = CommonUtil.findSTNode(range, syntaxTree);
        if (node.kind() != SyntaxKind.SERVICE_DECLARATION && node.kind() != SyntaxKind.MODULE_VAR_DECL) {
            throw new GraphqlModelGenerationException(INVALID_NODE_MSG);
        }

        Schema schemaObject = getSchemaObject(node, semanticModel, project);
        if (schemaObject.getTypes().isEmpty()) {
            throw new GraphqlModelGenerationException(EMPTY_SCHEMA_MSG);
        }
        String serviceName = "";
        if (node.kind() == SyntaxKind.SERVICE_DECLARATION) {
            ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) node;
            serviceName = ModelGenerationUtils.getServiceBasePath(serviceDeclarationNode);
        } else if (node.kind() == SyntaxKind.MODULE_VAR_DECL) {
            ModuleVariableDeclarationNode moduleVarDclNode = (ModuleVariableDeclarationNode) node;
            serviceName = moduleVarDclNode.typedBindingPattern().bindingPattern().toSourceCode();
        }

        return constructGraphqlModel(schemaObject, serviceName, position, syntaxTree);
    }

    public GraphqlModel constructGraphqlModel(Schema schemaObj, String serviceName, LineRange nodeLocation,
                                              SyntaxTree syntaxTree) throws GraphqlModelGenerationException {
        try {
            ServiceModelGenerator serviceModelGenerator = new ServiceModelGenerator(schemaObj, serviceName,
                    nodeLocation, syntaxTree);
            Service graphqlService = serviceModelGenerator.generate();

            InteractedComponentModelGenerator componentModelGenerator = new
                    InteractedComponentModelGenerator(schemaObj, syntaxTree);
            componentModelGenerator.generate();

            return new GraphqlModel(graphqlService, componentModelGenerator.getRecords(),
                    componentModelGenerator.getServiceClasses(), componentModelGenerator.getEnums(),
                    componentModelGenerator.getUnions(), componentModelGenerator.getInterfaces(),
                    componentModelGenerator.getHierarchicalResources());
        } catch (Exception e) {
            throw new GraphqlModelGenerationException(String.format(MODEL_GENERATION_ERROR_MSG, e.getMessage()));
        }
    }
}
