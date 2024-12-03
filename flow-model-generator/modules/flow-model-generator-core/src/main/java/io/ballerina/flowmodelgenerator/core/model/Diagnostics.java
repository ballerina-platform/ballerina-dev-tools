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

package io.ballerina.flowmodelgenerator.core.model;

import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a collection of diagnostic information.
 *
 * @param hasDiagnostics whether there are diagnostics
 * @param diagnostics    list of diagnostic information
 * @since 2.0.0
 */
public record Diagnostics(boolean hasDiagnostics, List<Info> diagnostics) {

    /**
     * Represents diagnostic information with severity and message.
     *
     * @param severity severity of the diagnostic
     * @param message  message of the diagnostic
     * @since 2.0.0
     */
    public record Info(DiagnosticSeverity severity, String message) { }

    public static class Builder<T> extends FacetedBuilder<T> {

        private boolean hasDiagnostics;
        private List<Info> diagnostics;

        protected Builder(T parentBuilder) {
            super(parentBuilder);
            this.hasDiagnostics = false;
            this.diagnostics = new ArrayList<>();
        }

        public Builder<T> hasDiagnostics() {
            this.hasDiagnostics = true;
            return this;
        }

        public Builder<T> diagnostics(List<Info> diagnostics) {
            this.diagnostics = diagnostics;
            return this;
        }

        public Builder<T> diagnostic(DiagnosticSeverity severity, String message) {
            this.diagnostics.add(new Info(severity, message));
            return this;
        }

        public Diagnostics build() {
            return new Diagnostics(hasDiagnostics || !diagnostics.isEmpty(),
                    diagnostics.isEmpty() ? null : diagnostics);
        }
    }
}
