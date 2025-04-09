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

package io.ballerina.architecturemodelgenerator.core;

import io.ballerina.architecturemodelgenerator.core.diagnostics.ArchitectureModelDiagnostic;
import io.ballerina.architecturemodelgenerator.core.diagnostics.DiagnosticMessage;
import io.ballerina.architecturemodelgenerator.core.diagnostics.DiagnosticNode;
import io.ballerina.architecturemodelgenerator.core.generators.entity.EntityModelGenerator;
import io.ballerina.architecturemodelgenerator.core.generators.entrypoint.FunctionEntryPointModelGenerator;
import io.ballerina.architecturemodelgenerator.core.generators.service.ServiceModelGenerator;
import io.ballerina.architecturemodelgenerator.core.model.entity.Entity;
import io.ballerina.architecturemodelgenerator.core.model.functionentrypoint.FunctionEntryPoint;
import io.ballerina.architecturemodelgenerator.core.model.service.Connection;
import io.ballerina.architecturemodelgenerator.core.model.service.Service;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Construct component model fpr project with multiple service.
 *
 * @since 2201.2.2
 */
public class ArchitectureModelBuilder {

    public ArchitectureModel constructComponentModel(Package currentPackage) {
        return constructComponentModel(currentPackage, null);
    }

    public ArchitectureModel constructComponentModel(Package currentPackage, PackageCompilation packageCompilation) {
        Map<String, Service> services = new HashMap<>();
        // todo: Change to TypeDefinition
        Map<String, Entity> entities = new HashMap<>();
        List<ArchitectureModelDiagnostic> diagnostics = new ArrayList<>();
        List<Connection> allDependencies = new ArrayList<>();
        AtomicReference<FunctionEntryPoint> functionEntryPoint = new AtomicReference<>();
        AtomicBoolean hasDiagnosticErrors = new AtomicBoolean(false);
        String packageOrg = currentPackage.packageOrg().value();
        String packageName = currentPackage.packageName().value();
        String packageVersion = currentPackage.packageVersion().value().toString();

        currentPackage.modules().forEach(module -> {
            PackageCompilation currentPackageCompilation = packageCompilation == null ?
                    PackageUtil.getCompilation(currentPackage) : packageCompilation;
            if (currentPackageCompilation.diagnosticResult().hasErrors() && !hasDiagnosticErrors.get()) {
                hasDiagnosticErrors.set(true);
            }

            ServiceModelGenerator serviceModelGenerator = new ServiceModelGenerator(currentPackageCompilation, module);
            try {
                serviceModelGenerator.generate();
                services.putAll(serviceModelGenerator.getServices());
                allDependencies.addAll(serviceModelGenerator.getDependencies());
            } catch (Exception e) {
                DiagnosticMessage message = DiagnosticMessage.failedToGenerate(DiagnosticNode.SERVICES,
                        e.getMessage());
                ArchitectureModelDiagnostic diagnostic = new ArchitectureModelDiagnostic(
                        message.getCode(), message.getDescription(), message.getSeverity(), null, null
                );
                diagnostics.add(diagnostic);
            }

            EntityModelGenerator entityModelGenerator = new EntityModelGenerator(currentPackageCompilation, module);
            try {
                entities.putAll(entityModelGenerator.generate());
            } catch (Exception e) {
                DiagnosticMessage message = DiagnosticMessage.failedToGenerate(DiagnosticNode.ENTITIES,
                        e.getMessage());
                ArchitectureModelDiagnostic diagnostic = new ArchitectureModelDiagnostic(
                        message.getCode(), message.getDescription(), message.getSeverity(), null, null
                );
                diagnostics.add(diagnostic);
            }

            FunctionEntryPointModelGenerator functionEntryPointModelGenerator =
                    new FunctionEntryPointModelGenerator(currentPackageCompilation, module);
            functionEntryPointModelGenerator.generate();
            FunctionEntryPoint generatedFunctionEntryPoint = functionEntryPointModelGenerator.getFunctionEntryPoint();
            if (generatedFunctionEntryPoint != null) {
                functionEntryPoint.set(generatedFunctionEntryPoint);
                allDependencies.addAll(functionEntryPointModelGenerator.getDependencies());
            }
        });

        return new ArchitectureModel(Constants.MODEL_VERSION, packageName, packageOrg, packageVersion, diagnostics,
                services, entities, functionEntryPoint.get(), hasDiagnosticErrors.get(), allDependencies);
    }
}
