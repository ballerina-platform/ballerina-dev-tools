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

package io.ballerina.architecturemodelgenerator.core.generators.entrypoint;

import io.ballerina.architecturemodelgenerator.core.generators.ModelGenerator;
import io.ballerina.architecturemodelgenerator.core.generators.entrypoint.nodevisitors.FunctionEntryPointVisitor;
import io.ballerina.architecturemodelgenerator.core.model.functionentrypoint.FunctionEntryPoint;
import io.ballerina.architecturemodelgenerator.core.model.service.Connection;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Build entry point model based on a given Ballerina package.
 *
 * @since 2201.4.0
 */
public class FunctionEntryPointModelGenerator extends ModelGenerator {

    private FunctionEntryPoint functionEntryPoint = null;

    private final List<Connection> dependencies = new LinkedList<>();

    public FunctionEntryPoint getFunctionEntryPoint() {
        return functionEntryPoint;
    }

    public List<Connection> getDependencies() {
        return dependencies;
    }

    public FunctionEntryPointModelGenerator(PackageCompilation packageCompilation, Module module) {
        super(packageCompilation, module);
    }

    public void generate() {
        for (DocumentId documentId :getModule().documentIds()) {
            SyntaxTree syntaxTree = getModule().document(documentId).syntaxTree();
            Path filePath = getModuleRootPath().resolve(syntaxTree.filePath());
            FunctionEntryPointVisitor functionEntryPointVisitor = new FunctionEntryPointVisitor(
                    getPackageCompilation(), getSemanticModel(), syntaxTree, getModule().packageInstance(), filePath);
            syntaxTree.rootNode().accept(functionEntryPointVisitor);
            FunctionEntryPoint entryPointVisited = functionEntryPointVisitor.getFunctionEntryPoint();
            if (entryPointVisited != null) {
                functionEntryPoint = entryPointVisited;
                dependencies.addAll(functionEntryPointVisitor.getDependencies());
            }
        }
    }
}
