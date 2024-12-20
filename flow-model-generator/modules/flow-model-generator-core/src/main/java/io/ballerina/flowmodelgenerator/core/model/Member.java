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

import java.util.List;


/**
 * Represents a member of a type construct.
 *
 * @param kind          Kind of the member.
 * @param refs          References to the type descriptor.
 * @param type          Display name for the type.
 * @param name          Name of the member.
 * @param defaultValue  Default value of the member.
 * @param docs          Documentation of the member
 * @param annotations   Annotations of the member.
 * @since 2.0.0
 */
public record Member(
        MemberKind kind,
        List<String> refs,
        String type,
        String name,
        String defaultValue,
        String docs,
        List<TypeData.Annotation> annotations
) {
    public static class MemberBuilder {
        private Member.MemberKind kind;
        private List<String> refs;
        private String type;
        private String name;
        private String defaultValue;
        private String docs;
        private List<TypeData.Annotation> annotations;

        public MemberBuilder() {
        }

        public MemberBuilder kind(Member.MemberKind kind) {
            this.kind = kind;
            return this;
        }

        public MemberBuilder refs(List<String> refs) {
            this.refs = refs;
            return this;
        }

        public MemberBuilder type(String type) {
            this.type = type;
            return this;
        }

        public MemberBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MemberBuilder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public MemberBuilder docs(String docs) {
            this.docs = docs;
            return this;
        }

        public MemberBuilder annotations(List<TypeData.Annotation> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Member build() {
            Member member = new Member(kind, List.copyOf(refs), type, name, defaultValue,
                    docs, annotations != null ? List.copyOf(annotations) : null);
            this.kind = null;
            this.refs = null;
            this.type = null;
            this.name = null;
            this.defaultValue = null;
            this.docs = null;
            this.annotations = null;
            return member;
        }
    }

    public enum MemberKind {
        FIELD, TYPE, NAME
    }
}
