/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.architecturemodelgenerator.core.generators.service;

import io.ballerina.architecturemodelgenerator.core.generators.ModelGenerator;
import io.ballerina.architecturemodelgenerator.core.generators.service.nodevisitors.ServiceDeclarationNodeVisitor;
import io.ballerina.architecturemodelgenerator.core.model.service.Dependency;
import io.ballerina.architecturemodelgenerator.core.model.service.Service;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Build service model based on a given Ballerina service.
 *
 * @since 2201.2.2
 */
public class ServiceModelGenerator extends ModelGenerator {

    private final Map<String, Service> services = new HashMap<>();
    private final List<Dependency> dependencies = new LinkedList<>();

    public ServiceModelGenerator(PackageCompilation packageCompilation, Module module) {
        super(packageCompilation, module);
    }

    public Map<String, Service> getServices() {
        return services;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void generate() {
        for (DocumentId documentId :getModule().documentIds()) {
            SyntaxTree syntaxTree = getModule().document(documentId).syntaxTree();
            Path filePath = getModuleRootPath().resolve(syntaxTree.filePath());
            ServiceDeclarationNodeVisitor serviceNodeVisitor = new ServiceDeclarationNodeVisitor(
                    getPackageCompilation(), getSemanticModel(), syntaxTree, getModule().packageInstance(), filePath);
            syntaxTree.rootNode().accept(serviceNodeVisitor);
            serviceNodeVisitor.getServices().forEach(service -> {
                services.put(service.getServiceId().getId(), service);
            });
            dependencies.addAll(serviceNodeVisitor.getDependencies());
        }
    }
}
