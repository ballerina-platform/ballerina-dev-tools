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

package io.ballerina.servicemodelgenerator.extension.model;

import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.ARGUMENT_DEFAULT_VALUE_METADATA;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.ARGUMENT_NAME_METADATA;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.ARGUMENT_TYPE_METADATA;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.FIELD_DEFAULT_VALUE_METADATA;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.FIELD_NAME_METADATA;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.FIELD_TYPE_METADATA;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.PARAMETER_DEFAULT_VALUE_METADATA;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.PARAMETER_NAME_METADATA;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.PARAMETER_TYPE_METADATA;

/**
 * Represents a parameter in service method.
 *
 * @since 2.0.0
 */
public class Parameter {
    private MetaData metadata;
    private String kind;
    private Value type;
    private Value name;
    private Value defaultValue;
    private boolean enabled;
    private boolean editable;
    private boolean optional;
    private boolean advanced;
    private String httpParamType;

    public Parameter(MetaData metadata, String kind, Value type, Value name, Value defaultValue, boolean enabled,
                     boolean editable, boolean optional, boolean advanced, String httpParamType) {
        this.metadata = metadata;
        this.kind = kind;
        this.type = type;
        this.name = name;
        this.defaultValue = defaultValue;
        this.enabled = enabled;
        this.editable = editable;
        this.optional = optional;
        this.advanced = advanced;
        this.httpParamType = httpParamType;
    }

    public Parameter(Parameter parameter) {
        this.metadata = parameter.metadata;
        this.kind = parameter.kind;
        this.type = parameter.type;
        this.name = parameter.name;
        this.defaultValue = parameter.defaultValue;
        this.enabled = parameter.enabled;
        this.editable = parameter.editable;
        this.optional = parameter.optional;
        this.advanced = parameter.advanced;
        this.httpParamType = parameter.httpParamType;
    }

    public MetaData getMetadata() {
        return metadata;
    }

    public void setMetadata(MetaData metadata) {
        this.metadata = metadata;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Value getType() {
        return type;
    }

    public void setType(Value type) {
        this.type = type;
    }

    public Value getName() {
        return name;
    }

    public void setName(Value name) {
        this.name = name;
    }

    public Value getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Value defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public void setAdvanced(boolean advanced) {
        this.advanced = advanced;
    }

    public String getHttpParamType() {
        if (Objects.isNull(httpParamType)) {
            return null;
        }
        return httpParamType.charAt(0) + httpParamType.substring(1).toLowerCase(Locale.ROOT);
    }

    public void setHttpParamType(String httpParamType) {
        this.httpParamType = httpParamType;
    }

    private static Value name(MetaData metadata) {
        return new Value.ValueBuilder()
                .setMetadata(metadata)
                .valueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER)
                .enabled(true)
                .editable(true)
                .build();
    }

    private static Value type(MetaData metadata) {
        return new Value.ValueBuilder()
                .setMetadata(metadata)
                .valueType(ServiceModelGeneratorConstants.VALUE_TYPE_TYPE)
                .isType(true)
                .enabled(true)
                .editable(true)
                .build();
    }

    private static Value defaultValue(MetaData metadata) {
        return new Value.ValueBuilder()
                .setMetadata(metadata)
                .valueType(ServiceModelGeneratorConstants.VALUE_TYPE_EXPRESSION)
                .enabled(true)
                .editable(true)
                .optional(true)
                .build();
    }

    public static Parameter getNewField() {
        return new Parameter.Builder()
                .type(type(FIELD_TYPE_METADATA))
                .name(name(FIELD_NAME_METADATA))
                .defaultValue(defaultValue(FIELD_DEFAULT_VALUE_METADATA))
                .build();
    }

    public static Parameter graphQLParamSchema() {
        return new Parameter.Builder()
                .type(type(ARGUMENT_TYPE_METADATA))
                .name(name(ARGUMENT_NAME_METADATA))
                .defaultValue(defaultValue(ARGUMENT_DEFAULT_VALUE_METADATA))
                .enabled(true)
                .editable(true)
                .build();
    }

    public static Parameter functionParamSchema() {
        return new Parameter.Builder()
                .type(type(PARAMETER_TYPE_METADATA))
                .name(name(PARAMETER_NAME_METADATA))
                .defaultValue(defaultValue(PARAMETER_DEFAULT_VALUE_METADATA))
                .enabled(true)
                .editable(true)
                .build();
    }

    public static Parameter getNewParameter(boolean isGraphQL) {
        return isGraphQL ? graphQLParamSchema() : functionParamSchema();
    }

    public static class RequiredParamSorter implements Comparator<Parameter>, Serializable {

        @Serial
        private static final long serialVersionUID = 1L; // Or any long value

        @Override
        public int compare(Parameter param1, Parameter param2) {
            Value param1DefaultValue = param1.getDefaultValue();
            Value param2DefaultValue = param2.getDefaultValue();
            if (param1DefaultValue == null && param2DefaultValue == null) {
                return 0;
            } else if (param1DefaultValue == null) {
                return -1;
            } else if (param2DefaultValue == null) {
                return 1;
            }

            boolean isEnabled1 = param1DefaultValue.isEnabledWithValue();
            boolean isEnabled2 = param2DefaultValue.isEnabledWithValue();

            if (isEnabled1 == isEnabled2) {
                return 0; // Both have the same enabled state, consider them equal
            } else if (isEnabled1) {
                return 1;  // true comes after false
            } else {
                return -1; // false comes before true
            }
        }

    }

    public static class Builder {
        private MetaData metadata;
        private String kind;
        private Value type;
        private Value name;
        private Value defaultValue;
        private boolean enabled;
        private boolean editable;
        private boolean optional;
        private boolean advanced;
        private String httpParamType;

        public Builder metadata(MetaData metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder kind(String kind) {
            this.kind = kind;
            return this;
        }

        public Builder type(Value type) {
            this.type = type;
            return this;
        }

        public Builder name(Value name) {
            this.name = name;
            return this;
        }

        public Builder defaultValue(Value defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder editable(boolean editable) {
            this.editable = editable;
            return this;
        }

        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public Builder advanced(boolean advanced) {
            this.advanced = advanced;
            return this;
        }

        public Builder httpParamType(String httpParamType) {
            this.httpParamType = httpParamType;
            return this;
        }

        public Parameter build() {
            return new Parameter(metadata, kind, type, name, defaultValue, enabled, editable, optional, advanced,
                    httpParamType);
        }
    }
}
