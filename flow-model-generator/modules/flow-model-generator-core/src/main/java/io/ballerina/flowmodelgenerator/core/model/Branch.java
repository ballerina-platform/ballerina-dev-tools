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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;
import org.ballerinalang.langserver.common.utils.NameUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a branch of the node.
 *
 * @param label      label of the branch
 * @param kind       kind of the branch
 * @param codedata   codedata of the branch
 * @param repeatable the repeatable pattern of the branch
 * @param properties properties of the branch
 * @param children   children of the branch
 * @since 2.0.0
 */
public record Branch(String label, BranchKind kind, Codedata codedata, Repeatable repeatable,
                     Map<String, Property> properties, List<FlowNode> children) {

    public static final String BODY_LABEL = "Body";
    public static final String ON_FAILURE_LABEL = "On Failure";

    public static final Branch DEFAULT_BODY_BRANCH =
            new Builder().label(BODY_LABEL).kind(BranchKind.BLOCK).repeatable(Repeatable.ONE)
                    .codedata().node(NodeKind.BODY).stepOut().build();

    public static Branch getEmptyBranch(String label, NodeKind kind) {
        return new Builder().label(label).kind(BranchKind.BLOCK).repeatable(Repeatable.ZERO_OR_ONE)
                .codedata().node(kind).stepOut().build();
    }

    public static Branch getDefaultOnFailBranch(boolean value) {
        return new Builder().label(ON_FAILURE_LABEL).kind(BranchKind.BLOCK).repeatable(Repeatable.ZERO_OR_ONE)
                .codedata().node(NodeKind.ON_FAILURE).stepOut()
                .properties().ignore(value).onErrorVariable(null).stepOut().build();
    }

    public static Branch getDefaultWorkerBranch(Set<String> names) {
        String workerName = NameUtil.generateTypeName("worker", names);
        return new Builder()
                .label(workerName)
                .kind(BranchKind.WORKER)
                .repeatable(Repeatable.ONE_OR_MORE)
                .codedata().node(NodeKind.WORKER).stepOut()
                .properties()
                    .returnType(null)
                    .data(null, Property.WORKER_NAME, Property.WORKER_DOC, workerName)
                .stepOut()
                .build();
    }

    public enum BranchKind {
        BLOCK,
        WORKER
    }

    public enum Repeatable {
        ONE_OR_MORE("1+"),
        ZERO_OR_ONE("0..1"),
        ONE("1"),
        ZERO_OR_MORE("0+");

        private final String value;

        Repeatable(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Repeatable fromValue(String value) {
            for (Repeatable repeatable : values()) {
                if (repeatable.value.equals(value)) {
                    return repeatable;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    public Optional<Property> getProperty(String key) {
        return Optional.ofNullable(properties).map(props -> props.get(key));
    }

    /**
     * Represents a builder for the branch.
     *
     * @since 2.0.0
     */
    public static class Builder {

        private String label;
        private Branch.BranchKind kind;
        private final List<FlowNode> children;
        private Repeatable repeatable;

        protected Codedata.Builder<Builder> codedataBuilder;
        protected FormBuilder<Builder> formBuilder;
        private SemanticModel semanticModel;
        private DiagnosticHandler diagnosticHandler;
        private ModuleInfo moduleInfo;

        public Builder() {
            children = new ArrayList<>();
        }

        public Builder semanticModel(SemanticModel semanticModel) {
            this.semanticModel = semanticModel;
            return this;
        }

        public Builder diagnosticHandler(DiagnosticHandler diagnosticHandler) {
            this.diagnosticHandler = diagnosticHandler;
            return this;
        }

        public Builder defaultModuleName(ModuleInfo moduleInfo) {
            this.moduleInfo = moduleInfo;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder kind(Branch.BranchKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder node(FlowNode node) {
            this.children.add(node);
            return this;
        }

        public Builder nodes(List<FlowNode> nodes) {
            this.children.addAll(nodes);
            return this;
        }

        public Builder repeatable(Repeatable repeatable) {
            this.repeatable = repeatable;
            return this;
        }

        public FormBuilder<Builder> properties() {
            if (this.formBuilder == null) {
                this.formBuilder =
                        new FormBuilder<>(semanticModel, diagnosticHandler, moduleInfo, this);
            }
            return this.formBuilder;
        }

        public Codedata.Builder<Builder> codedata() {
            if (this.codedataBuilder == null) {
                this.codedataBuilder = new Codedata.Builder<>(this);
            }
            return this.codedataBuilder;
        }

        public Branch build() {
            return new Branch(label, kind, codedataBuilder == null ? null : codedataBuilder.build(),
                    repeatable, formBuilder == null ? null : formBuilder.build(), children);
        }
    }
}
