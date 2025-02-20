/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;

import java.util.List;
import java.util.Map;

/**
 * Represents a function.
 *
 * @param accessor      Accessor of the function.
 * @param qualifiers    Qualifiers of the function.
 * @param parameters    Parameters of the function.
 * @param restParameter Rest parameter of the function.
 * @param kind          Kind of the function.
 * @param name          Resource path of the function.
 * @param description   Description of the function.
 * @param returnType    Return type of the function.
 * @param refs          Type references associated with the return type of the function.
 * @param properties    Properties of the function.
 * @since 2.0.0
 */
public record Function(
        String accessor,
        List<String> qualifiers,
        List<Member> parameters,
        Member restParameter,
        FunctionKind kind,
        String name,    // TODO: Need a structured schema for resource path
        String description,
        Object returnType,
        List<String> refs,
        Map<String, Property> properties
) {
    public enum FunctionKind {
        FUNCTION,
        RESOURCE,
        REMOTE
    }

    public static class FunctionBuilder {
        private String accessor;
        private List<String> qualifiers;
        private List<Member> parameters;
        private Member restParameter;
        private FunctionKind kind;
        private String name;
        private String docs;
        private Object returnType;
        private List<String> refs;
        private FormBuilder<FunctionBuilder> formBuilder;
        private ModuleInfo moduleInfo;
        private SemanticModel semanticModel;
        private DiagnosticHandler diagnosticHandler;

        public FunctionBuilder() {
        }

        public FunctionBuilder semanticModel(SemanticModel semanticModel) {
            this.semanticModel = semanticModel;
            return this;
        }

        public FunctionBuilder diagnosticHandler(DiagnosticHandler diagnosticHandler) {
            this.diagnosticHandler = diagnosticHandler;
            return this;
        }

        public FunctionBuilder defaultModuleName(ModuleInfo moduleInfo) {
            this.moduleInfo = moduleInfo;
            return this;
        }

        public FunctionBuilder accessor(String accessor) {
            this.accessor = accessor;
            return this;
        }

        public FunctionBuilder qualifiers(List<String> qualifiers) {
            this.qualifiers = qualifiers;
            return this;
        }

        public FunctionBuilder parameters(List<Member> parameters) {
            this.parameters = parameters;
            return this;
        }

        public FunctionBuilder restParameter(Member restParameter) {
            this.restParameter = restParameter;
            return this;
        }

        public FunctionBuilder kind(FunctionKind kind) {
            this.kind = kind;
            return this;
        }

        public FunctionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public FunctionBuilder docs(String docs) {
            this.docs = docs;
            return this;
        }

        public FunctionBuilder returnType(Object returnType) {
            this.returnType = returnType;
            return this;
        }

        public FunctionBuilder refs(List<String> refs) {
            this.refs = refs;
            return this;
        }

        public FormBuilder<FunctionBuilder> properties() {
            if (this.formBuilder == null) {
                this.formBuilder = new FormBuilder<>(semanticModel, diagnosticHandler, moduleInfo, this);
            }
            return this.formBuilder;
        }

        public Function build() {
            return new Function(accessor, qualifiers, parameters, restParameter, kind, name, docs,
                    returnType, refs, formBuilder == null ? null : formBuilder.build());
        }
    }
}
