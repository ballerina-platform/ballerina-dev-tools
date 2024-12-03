package io.ballerina.flowmodelgenerator.core.model;

import java.util.List;

public record Member(
        MemberKind kind,
        String ref,
        String type,
        String name,
        String defaultValue,
        String docs,
        List<TypeData.Annotation> annotations
) {
    public static class MemberBuilder {
        private Member.MemberKind kind;
        private String ref;
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

        public MemberBuilder ref(String ref) {
            this.ref = ref;
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
            Member member = new Member(kind, ref, type, name, defaultValue, docs, annotations);
            this.kind = null;
            this.ref = null;
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
