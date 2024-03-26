/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.sequencemodelgenerator.ls.extension;

import com.google.gson.JsonElement;

/**
 * Represents the response from the sequence diagram model generator service.
 *
 * @since 2201.9.0
 */
public class SequenceDiagramServiceResponse {
    private JsonElement sequenceDiagramModel;
    private ModelDiagnostic modelDiagnostic;

    public JsonElement getSequenceDiagramModel() {
        return sequenceDiagramModel;
    }

    public void setSequenceDiagramModel(JsonElement sequenceDiagramModel) {
        this.sequenceDiagramModel = sequenceDiagramModel;
    }

    public ModelDiagnostic getModelDiagnostic() {
        return modelDiagnostic;
    }

    public void setModelDiagnostic(ModelDiagnostic modelDiagnostic) {
        this.modelDiagnostic = modelDiagnostic;
    }
}
