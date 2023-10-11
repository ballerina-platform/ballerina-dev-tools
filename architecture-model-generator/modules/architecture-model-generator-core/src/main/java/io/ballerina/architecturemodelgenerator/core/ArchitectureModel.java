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
import io.ballerina.architecturemodelgenerator.core.model.service.Connection;
import io.ballerina.architecturemodelgenerator.core.model.service.Service;

import java.util.List;
import java.util.Map;

/**
 * Represents intermediate model to represent multi-service projects.
 *
 * @since 2201.2.2
 */
public class ArchitectureModel {

    private final String modelVersion;
    private final String id;
    private final String orgName;
    private final String version;
    private final boolean hasCompilationErrors;
    private final List<ArchitectureModelDiagnostic> diagnostics;
    private final Map<String, Service> services;
    private final Map<String, Entity> entities;
    private final FunctionEntryPoint functionEntryPoint;
    private final List<Connection> connections;

    public ArchitectureModel(String modelVersion, String id, String orgName, String version,
                             List<ArchitectureModelDiagnostic> diagnostics, Map<String, Service> services,
                             Map<String, Entity> entities, FunctionEntryPoint functionEntryPoint,
                             boolean hasCompilationErrors, List<Connection> connections) {
        this.modelVersion = modelVersion;
        this.id = id;
        this.orgName = orgName;
        this.version = version;
        this.diagnostics = diagnostics;
        this.services = services;
        this.entities = entities;
        this.functionEntryPoint = functionEntryPoint;
        this.hasCompilationErrors = hasCompilationErrors;
        this.connections = connections;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getId() {
        return String.format("%s/%s:%s", orgName, id, version);
    }

    public String getOrgName() {
        return orgName;
    }

    public String getVersion() {
        return version;
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

    public List<Connection> getConnections() {
        return connections;
    }
}
