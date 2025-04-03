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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ParameterMemberTypeData;

import java.util.List;

/**
 * Represents an expression in the flow model.
 *
 * @param metadata            metadata of the property
 * @param valueType           acceptable value types of the property
 * @param valueTypeConstraint constraint of the value type
 * @param value               value of the property
 * @param placeholder         default value of the property
 * @param optional            whether the property can be left empty
 * @param editable            whether the property is not readonly
 * @param advanced            whether the property should be shown in the advanced tab
 * @param hidden              whether the property should be hidden
 * @param diagnostics         diagnostics of the property
 * @param codedata            codedata of the property
 * @param typeMembers         member types of the type constrain
 * @param advancedValue       advanced value of the property
 * @since 2.0.0
 */
public record Property(Metadata metadata, String valueType, Object valueTypeConstraint, Object value,
                       String placeholder, boolean optional, boolean editable, boolean advanced, boolean hidden,
                       Diagnostics diagnostics, PropertyCodedata codedata, List<PropertyTypeMemberInfo> typeMembers,
                       Object advancedValue) {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final TypeToken<List<Property>> LIST_PROPERTY_TYPE_TOKEN = new TypeToken<List<Property>>() { };

    @SuppressWarnings("unchecked")
    public <T> T valueAsType(TypeToken<T> typeToken) {
        if (value instanceof List) {
            return (T) gson.fromJson(gson.toJson(value), typeToken.getType());
        }
        return (T) value;
    }

    public static final String VARIABLE_KEY = "variable";
    public static final String VARIABLE_NAME = "Variable Name";
    public static final String PARAMETER_VARIABLE_DOC = "Name of the parameter";
    public static final String IMPLICIT_VARIABLE_LABEL = "Name";
    public static final String VARIABLE_DOC = "Name of the variable";

    public static final String WORKER_NAME = "Worker Name";
    public static final String WORKER_DOC = "Name of the worker";

    public static final String NAME_KEY = "name";
    public static final String DESCRIPTION_KEY = "description";
    public static final String RETURN_DESCRIPTION_KEY = "returnDescription";
    public static final String IS_ARRAY_KEY = "isArray";
    public static final String ARRAY_SIZE = "arraySize";

    public static final String IS_PUBLIC_KEY = "isPublic";
    public static final String IS_PUBLIC_LABEL = "public";
    public static final String IS_PUBLIC_DOC = "Is this public";

    public static final String IS_PRIVATE_KEY = "isPrivate";
    public static final String IS_PRIVATE_LABEL = "private";
    public static final String IS_PRIVATE_DOC = "Is this private";

    public static final String IS_ISOLATED_KEY = "isIsolated";
    public static final String IS_ISOLATED_LABEL = "isolated";
    public static final String IS_ISOLATED_DOC = "Is this isolated";

    public static final String IS_READ_ONLY_KEY = "isReadOnly";
    public static final String IS_READ_ONLY_LABEL = "readonly";
    public static final String IS_READ_ONLY_DOC = "Is this readonly";

    public static final String IS_DISTINCT_KEY = "isDistinct";
    public static final String IS_DISTINCT_LABEL = "distinct";
    public static final String IS_DISTINCT_DOC = "Is this distinct";

    public static final String NETWORK_QUALIFIER_KEY = "networkQualifier";
    public static final String NETWORK_QUALIFIER_LABEL = "Network Qualifier";
    public static final String NETWORK_QUALIFIER_DOC = "Network qualifier";

    public static final String TYPE_KEY = "type";
    public static final String TYPE_LABEL = "Variable Type";
    public static final String PARAMETER_TYPE_DOC = "Type of the parameter";
    public static final String IMPLICIT_TYPE_LABEL = "Type";
    public static final String DESCRIPTION_LABEL = "Description";
    public static final String DESCRIPTION_TYPE_DOC = "Description of the function";
    public static final String RETURN_DESCRIPTION_TYPE_DOC = "Description of the return value";
    public static final String PARAMETER_DESCRIPTION_TYPE_DOC = "Description of the parameter";

    public static final String TYPE_DOC = "Type of the variable";

    public static final String TYPE_NAME_LABEL = "Type name";
    public static final String TYPE_NAME_DOC = "Unique name to identify the type";
    public static final String TYPE_DESC_LABEL = "Type description";
    public static final String TYPE_DESC_DOC = "Detailed description about the type";
    public static final String TYPE_IS_ARRAY_LABEL = "Is array type";
    public static final String TYPE_IS_ARRAY_DOC = "Is this type an array or list value";
    public static final String TYPE_ARRAY_SIZE_LABEL = "Size of the array";
    public static final String TYPE_ARRAY_SIZE_DOC = "Array dimensions";

    public static final String QUALIFIERS_KEY = "qualifiers";
    public static final String TYPE_QUALIFIERS_LABEL = "Type Qualifiers";
    public static final String TYPE_QUALIFIERS_DOC = "Qualifiers of the type";

    public static final String RETURN_TYPE_LABEL = "Return Type";
    public static final String RETURN_TYPE_DOC = "Type of the return value";

    public static final String EXPRESSION_KEY = "expression";
    public static final String EXPRESSION_LABEL = "Expression";
    public static final String EXPRESSION_DOC = "Expression";

    public static final String CONDITION_KEY = "condition";
    public static final String CONDITION_LABEL = "Condition";
    public static final String CONDITION_DOC = "Boolean Condition";

    public static final String IGNORE_KEY = "ignore";
    public static final String IGNORE_LABEL = "Ignore";
    public static final String IGNORE_DOC = "Ignore the error value";

    public static final String ON_ERROR_VARIABLE_KEY = "errorVariable";
    public static final String ON_ERROR_VARIABLE_LABEL = "Error Variable";
    public static final String ON_ERROR_VARIABLE_DOC = "Name of the error variable";

    public static final String ON_ERROR_TYPE_KEY = "errorType";
    public static final String ON_ERROR_TYPE_LABEL = "Error Type";
    public static final String ON_ERROR_TYPE_DOC = "Type of the error";

    public static final String COLLECTION_KEY = "collection";
    public static final String COLLECTION_LABEL = "Collection";
    public static final String COLLECTION_DOC = "Collection to iterate";

    public static final String CHECK_ERROR_KEY = "checkError";
    public static final String CHECK_ERROR_LABEL = "Check Error";
    public static final String CHECK_ERROR_DOC = "Trigger error flow";

    public static final String SCOPE_KEY = "scope";
    public static final String SCOPE_LABEL = "Connection Scope";
    public static final String SCOPE_DOC = "Scope of the connection, Global or Local";
    public static final String GLOBAL_SCOPE = "Global";
    public static final String SERVICE_SCOPE = "Service";
    public static final String OBJECT_SCOPE = "Object";
    public static final String LOCAL_SCOPE = "Local";

    public static final String CONNECTION_KEY = "connection";
    public static final String CONNECTION_LABEL = "Connection";
    public static final String CONNECTION_DOC = "Connection to use";
    public static final String METHOD_EXPRESSION_LABEL = "Object";
    public static final String METHOD_EXPRESSION_DOC = "The object which you want to call the method on";

    public static final String RESOURCE_PATH_KEY = "resourcePath";
    public static final String RESOURCE_PATH_LABEL = "Resource Path";
    public static final String RESOURCE_PATH_DOC = "Resource Path";

    public static final String COMMENT_KEY = "comment";
    public static final String COMMENT_LABEL = "Comment";
    public static final String COMMENT_DOC = "Comment to describe the flow";

    public static final String PATTERNS_KEY = "patterns";
    public static final String PATTERNS_LABEL = "Patterns";
    public static final String PATTERNS_DOC = "List of binding patterns";
    public static final String PATTERN_LABEL = "Pattern";
    public static final String PATTERN_DOC = "Binding pattern";

    public static final String GUARD_KEY = "guard";
    public static final String GUARD_DOC = "Guard expression";

    public static final String RETRY_COUNT_KEY = "retryCount";
    public static final String RETRY_COUNT_LABEL = "Retry Count";
    public static final String RETRY_COUNT_DOC = "Number of retries";

    public static final String DEFAULTABLE_KEY = "defaultable";
    public static final String DEFAULT_VALUE_LABEL = "Default value";
    public static final String DEFAULT_VALUE_DOC = "Default value for the config, if empty your need to " +
            "provide a value at runtime";

    public static final String PARAMETER_LABEL = "Parameter";
    public static final String PARAMETER_DOC = "Function parameter";

    public static final String FUNCTION_NAME_KEY = "functionName";
    public static final String PARAMETERS_KEY = "parameters";

    public String toSourceCode() {
        if (value == null || value.toString().isEmpty()) {
            return placeholder == null ? "" : placeholder;
        }
        return value.toString();
    }

    public enum ValueType {
        EXPRESSION,
        IDENTIFIER,
        STRING,
        TYPE,
        ENUM,
        SINGLE_SELECT,
        MULTIPLE_SELECT,
        VIEW,
        INCLUSION,
        UNION,
        FLAG,
        MAPPING_EXPRESSION_SET,
        EXPRESSION_SET,
        FIXED_PROPERTY,
        RAW_TEMPLATE,
        REPEATABLE_PROPERTY
    }

    public static ValueType valueTypeFrom(String s) {
        return switch (s) {
            case "inclusion" -> ValueType.INCLUSION;
            case "union" -> ValueType.UNION;
            default -> ValueType.EXPRESSION;
        };
    }

    public static class Builder<T> extends FacetedBuilder<T> implements DiagnosticHandler.DiagnosticCapable {

        private String type;
        private Object value;
        private String placeholder;
        private boolean optional;
        private boolean editable;
        private boolean advanced;
        private boolean hidden;
        private Object typeConstraint;
        private Metadata.Builder<Builder<T>> metadataBuilder;
        private Diagnostics.Builder<Builder<T>> diagnosticsBuilder;
        private PropertyCodedata.Builder<Builder<T>> codedataBuilder;
        private List<PropertyTypeMemberInfo> typeMembers;
        private Object advancedValue;

        public Builder(T parentBuilder) {
            super(parentBuilder);
        }

        public Builder<T> type(TypeSymbol typeSymbol) {
            this.type = CommonUtils.getTypeSignature(null, typeSymbol, false);
            return this;
        }

        public Builder<T> type(ValueType type) {
            this.type = type.name();
            return this;
        }

        public Builder<T> typeConstraint(Object typeConstraint) {
            this.typeConstraint = typeConstraint;
            return this;
        }

        public Builder<T> value(Object value) {
            this.value = value;
            return this;
        }

        public Builder<T> advancedValue(Object advancedValue) {
            this.advancedValue = advancedValue;
            return this;
        }

        public Builder<T> optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public Builder<T> defaultable(boolean defaultable) {
            this.optional = defaultable;
            this.advanced = defaultable;
            return this;
        }

        public Builder<T> advanced(boolean advanced) {
            this.advanced = advanced;
            return this;
        }

        public Builder<T> editable() {
            this.editable = true;
            return this;
        }

        public Builder<T> editable(boolean editable) {
            this.editable = editable;
            return this;
        }

        public Builder<T> placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder<T> hidden() {
            this.hidden = true;
            return this;
        }

        public Metadata.Builder<Builder<T>> metadata() {
            if (this.metadataBuilder == null) {
                this.metadataBuilder = new Metadata.Builder<>(this);
            }
            return this.metadataBuilder;
        }

        public Builder<T> typeMembers(List<ParameterMemberTypeData> memberTypeData) {
            this.typeMembers = memberTypeData.stream().map(memberType -> new PropertyTypeMemberInfo(memberType.type(),
                    memberType.packageInfo(), memberType.kind(), false)).toList();
            return this;
        }

        public Builder<T> typeMembers(List<ParameterMemberTypeData> memberTypeData, String selectedType) {
            this.typeMembers = memberTypeData.stream().map(memberType -> new PropertyTypeMemberInfo(memberType.type(),
                    memberType.packageInfo(), memberType.kind(), memberType.type().equals(selectedType))).toList();
            return this;
        }

        public PropertyCodedata.Builder<Builder<T>> codedata() {
            if (this.codedataBuilder == null) {
                this.codedataBuilder = new PropertyCodedata.Builder<>(this);
            }
            return this.codedataBuilder;
        }

        @Override
        public Diagnostics.Builder<Builder<T>> diagnostics() {
            if (this.diagnosticsBuilder == null) {
                this.diagnosticsBuilder = new Diagnostics.Builder<>(this);
            }
            return this.diagnosticsBuilder;
        }

        public Property build() {
            Property property =
                    new Property(metadataBuilder == null ? null : metadataBuilder.build(), type, typeConstraint, value,
                            placeholder, optional, editable, advanced, hidden,
                            diagnosticsBuilder == null ? null : diagnosticsBuilder.build(),
                            codedataBuilder == null ? null : codedataBuilder.build(), typeMembers, advancedValue);
            this.metadataBuilder = null;
            this.type = null;
            this.typeConstraint = null;
            this.value = null;
            this.placeholder = null;
            this.optional = false;
            this.editable = false;
            this.advanced = false;
            this.hidden = false;
            this.diagnosticsBuilder = null;
            this.codedataBuilder = null;
            this.typeMembers = null;
            this.advancedValue = null;
            return property;
        }
    }
}
