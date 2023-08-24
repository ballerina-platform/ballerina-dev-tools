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
import io.ballerina.architecturemodelgenerator.core.model.ModelElement;
import io.ballerina.architecturemodelgenerator.core.model.SourceLocation;
import io.ballerina.architecturemodelgenerator.core.model.common.DisplayAnnotation;

import java.util.List;

/**
 * Provides service related information.
 *
 * @since 2201.2.2
 */
public class Service extends ModelElement {

    private final String id;
    private final String label;
    private final String type;
    private final List<ResourceFunction> resourceFunctions;
    private final List<RemoteFunction> remoteFunctions;
    private final DisplayAnnotation annotation;
    private final List<String> dependencies;

    public Service(String id, String label, String type, List<ResourceFunction> resourceFunctions,
                   List<RemoteFunction> remoteFunctions, DisplayAnnotation annotation, List<String> dependencyIds,
                   SourceLocation sourceLocation, List<ArchitectureModelDiagnostic> diagnostics) {
        super(sourceLocation, diagnostics);
        this.id = id;
        this.label = label;
        this.type = type;
        this.resourceFunctions = resourceFunctions;
        this.annotation = annotation;
        this.remoteFunctions = remoteFunctions;
        this.dependencies = dependencyIds;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public List<ResourceFunction> getResourceFunctions() {
        return resourceFunctions;
    }

    public DisplayAnnotation getAnnotation() {
        return annotation;
    }

    public List<RemoteFunction> getRemoteFunctions() {
        return remoteFunctions;
    }

    public List<String> getDependencies() {
        return dependencies;
    }
}
