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

package io.ballerina.architecturemodelgenerator.core.model.common;

import io.ballerina.architecturemodelgenerator.core.diagnostics.ArchitectureModelDiagnostic;
import io.ballerina.architecturemodelgenerator.core.model.ModelElement;
import io.ballerina.architecturemodelgenerator.core.model.SourceLocation;

import java.util.List;

/**
 * Represents display annotation.
 *
 * @since 2201.2.2
 */
public class DisplayAnnotation extends ModelElement {

    private final String id;
    private final String label;

    public DisplayAnnotation() {
        super(null, null);
        this.id = "";
        this.label = "";
    }

    public DisplayAnnotation(String id, String label, SourceLocation elementLocation,
                             List<ArchitectureModelDiagnostic> diagnostics) {
        super(elementLocation, diagnostics);
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
