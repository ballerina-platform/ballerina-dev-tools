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

package io.ballerina.architecturemodelgenerator.core.diagnostics;

import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;

/**
 * Represent the location of Architecture Model Diagnostic.
 *
 * @since 2201.7.0
 */
public class ArchitectureModelDiagnosticLocation implements Location {

    private final LineRange lineRange;
    private final TextRange textRange;

    public ArchitectureModelDiagnosticLocation(LineRange lineRange, TextRange textRange) {
        this.lineRange = lineRange;
        this.textRange = textRange;
    }

    @Override
    public LineRange lineRange() {
        return lineRange;
    }

    @Override
    public TextRange textRange() {
        return textRange;
    }
}