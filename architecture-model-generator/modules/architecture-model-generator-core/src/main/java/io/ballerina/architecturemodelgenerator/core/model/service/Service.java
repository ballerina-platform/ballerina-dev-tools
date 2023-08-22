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

package io.ballerina.architecturemodelgenerator.core.model.service;

import io.ballerina.architecturemodelgenerator.core.diagnostics.ArchitectureModelDiagnostic;
import io.ballerina.architecturemodelgenerator.core.model.ElementLocation;
import io.ballerina.architecturemodelgenerator.core.model.ModelElement;
import io.ballerina.architecturemodelgenerator.core.model.common.DisplayAnnotation;
import io.ballerina.architecturemodelgenerator.core.model.common.EntryPointID;

import java.util.List;

/**
 * Provides service related information.
 *
 * @since 2201.2.2
 */
public class Service extends ModelElement {

    private final String path;
    private final EntryPointID serviceId;
    private final String serviceType;
    private final List<Resource> resources;
    private final DisplayAnnotation annotation;
    private final List<RemoteFunction> remoteFunctions;
    private final List<EntryPointID> dependencyIDs;

    public Service(String path, EntryPointID serviceId, String serviceType, List<Resource> resources,
                   DisplayAnnotation annotation, List<RemoteFunction> remoteFunctions, List<EntryPointID> dependencyIDs,
                   ElementLocation elementLocation, List<ArchitectureModelDiagnostic> diagnostics) {
        super(elementLocation, diagnostics);
        this.path = path;
        this.serviceId = serviceId;
        this.serviceType = serviceType;
        this.resources = resources;
        this.annotation = annotation;
        this.remoteFunctions = remoteFunctions;
        this.dependencyIDs = dependencyIDs;
    }

    public String getPath() {
        return path;
    }

    public EntryPointID getServiceId() {
        return serviceId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public DisplayAnnotation getAnnotation() {
        return annotation;
    }

    public List<RemoteFunction> getRemoteFunctions() {
        return remoteFunctions;
    }

    public List<EntryPointID> getDependencyIDs() {
        return dependencyIDs;
    }
}
