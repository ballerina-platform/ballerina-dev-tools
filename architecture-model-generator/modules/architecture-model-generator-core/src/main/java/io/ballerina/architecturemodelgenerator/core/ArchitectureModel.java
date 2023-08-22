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
import io.ballerina.architecturemodelgenerator.core.model.entity.Entity;
import io.ballerina.architecturemodelgenerator.core.model.functionentrypoint.FunctionEntryPoint;
import io.ballerina.architecturemodelgenerator.core.model.service.Dependency;
import io.ballerina.architecturemodelgenerator.core.model.service.Service;
import io.ballerina.projects.Package;

import java.util.List;
import java.util.Map;

/**
 * Represents intermediate model to represent multi-service projects.
 *
 * @param <T> the type parameter for the model
 * @since 2201.2.2
 */
public class ArchitectureModel<T> {

    private final String version;
    private final T packageId;
    private final boolean hasCompilationErrors;
    private final List<ArchitectureModelDiagnostic> diagnostics;
    private final Map<String, Service> services;
    private final Map<String, Entity> entities;
    private final FunctionEntryPoint functionEntryPoint;

    private final List<Dependency> dependencies;

    public ArchitectureModel(String version, T packageId, List<ArchitectureModelDiagnostic> diagnostics,
                             Map<String, Service> services, Map<String, Entity> entities,
                             FunctionEntryPoint functionEntryPoint, boolean hasCompilationErrors,
                             List<Dependency> dependencies) {
        this.version = version;
        this.packageId = packageId;
        this.diagnostics = diagnostics;
        this.services = services;
        this.entities = entities;
        this.functionEntryPoint = functionEntryPoint;
        this.hasCompilationErrors = hasCompilationErrors;
        this.dependencies = dependencies;
    }

    public String getVersion() {
        return version;
    }

    public T getPackageId() {
        return packageId;
    }

    public List<ArchitectureModelDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    public Map<String, Service> getServices() {
        return services;
    }

    public Map<String, Entity> getEntities() {
        return entities;
    }

    public FunctionEntryPoint getFunctionEntryPoint() {
        return functionEntryPoint;
    }

    public boolean hasCompilationErrors() {
        return hasCompilationErrors;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * Represent current package information.
     */
    public static class PackageId {

        private final String name;
        private final String org;
        private final String version;

        public PackageId(Package currentPackage) {
            this.name = currentPackage.packageName().value();
            this.org = currentPackage.packageOrg().value();
            this.version = currentPackage.packageVersion().value().toString();
        }

        public String getName() {
            return name;
        }

        public String getOrg() {
            return org;
        }

        public String getVersion() {
            return version;
        }
    }
}
