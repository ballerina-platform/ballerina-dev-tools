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

package io.ballerina.architecturemodelgenerator.extension;

import io.ballerina.architecturemodelgenerator.core.diagnostics.ArchitectureModelDiagnostic;

import java.util.List;

/**
 * Response class for Architecture Model Responses.
 *
 * @since 2201.6.0
 */
public abstract class ModelResponse {

    private List<ArchitectureModelDiagnostic> diagnostics;

    protected ModelResponse(List<ArchitectureModelDiagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }

    public List<ArchitectureModelDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(List<ArchitectureModelDiagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void addDiagnostics(List<ArchitectureModelDiagnostic> architectureModelDiagnostics) {
        this.diagnostics.addAll(architectureModelDiagnostics);
    }
}
