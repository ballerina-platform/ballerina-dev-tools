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

import java.util.Locale;
import java.util.Objects;

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

    public Parameter() {
        this(null, null, null, null, null, false, false, false, false, null);
    }

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

    public static Parameter getNewParameter() {
        return new Parameter(null, null,
                new Value(ServiceModelGeneratorConstants.PARAMETER_TYPE_METADATA),
                new Value(ServiceModelGeneratorConstants.PARAMETER_NAME_METADATA),
                new Value(ServiceModelGeneratorConstants.PARAMETER_DEFAULT_VALUE_METADATA),
                false, false, false, false, null);
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
}
