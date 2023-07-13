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

package io.ballerina.architecturemodelgenerator.core.model.functionentrypoint;

import io.ballerina.architecturemodelgenerator.core.diagnostics.ArchitectureModelDiagnostic;
import io.ballerina.architecturemodelgenerator.core.model.ElementLocation;
import io.ballerina.architecturemodelgenerator.core.model.ModelElement;
import io.ballerina.architecturemodelgenerator.core.model.common.DisplayAnnotation;
import io.ballerina.architecturemodelgenerator.core.model.common.FunctionParameter;
import io.ballerina.architecturemodelgenerator.core.model.common.Interaction;

import java.util.List;

/**
 * Represents EntryPoint related information.
 *
 * @since 2201.4.2
 */
public class FunctionEntryPoint extends ModelElement {

    private final String functionID;
    private final String label;
    private final List<FunctionParameter> parameters;
    private final List<String> returns;
    private final List<Interaction> interactions;
    private final DisplayAnnotation annotation;
    private List<String> dependencyIDs;

    public FunctionEntryPoint(String functionID, String label, List<FunctionParameter> parameters, List<String> returns,
                      List<Interaction> interactions, DisplayAnnotation annotation, List<String> dependencyIDs,
                      ElementLocation elementLocation, List<ArchitectureModelDiagnostic> diagnostics) {
        super(elementLocation, diagnostics);
        this.functionID = functionID;
        this.label = label;
        this.parameters = parameters;
        this.returns = returns;
        this.annotation = annotation;
        this.interactions = interactions;
        this.dependencyIDs = dependencyIDs;
    }

    public String getFunctionID() {
        return functionID;
    }

    public String getLabel() {
        return label;
    }

    public List<FunctionParameter> getParameters() {
        return parameters;
    }

    public List<String> getReturns() {
        return returns;
    }

    public List<Interaction> getInteractions() {
        return interactions;
    }

    public DisplayAnnotation getAnnotation() {
        return annotation;
    }

    public void setDependencyIDs(List<String> dependencyIDs) {
        this.dependencyIDs = dependencyIDs;
    }

    public List<String> getDependencyIDs() {
        return dependencyIDs;
    }
}
