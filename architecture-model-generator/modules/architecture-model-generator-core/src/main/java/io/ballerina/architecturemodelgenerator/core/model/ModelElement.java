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

package io.ballerina.architecturemodelgenerator.core.model;

import io.ballerina.architecturemodelgenerator.core.diagnostics.ArchitectureModelDiagnostic;

import java.util.List;

/**
 * Represents the abstract model for a component model item.
 *
 * @since 2201.3.1
 */
public abstract class ModelElement {

    private final SourceLocation sourceLocation;
    private final List<ArchitectureModelDiagnostic> diagnostics;

    public ModelElement(SourceLocation sourceLocation, List<ArchitectureModelDiagnostic> diagnostics) {
        this.sourceLocation = sourceLocation;
        this.diagnostics = diagnostics;
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public List<ArchitectureModelDiagnostic> getDiagnostics() {
        return diagnostics;
    }
}
