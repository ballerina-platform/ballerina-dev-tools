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

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Value {
    private MetaData metadata;
    private boolean enabled;
    private boolean editable;
    private String value;
    private List<String> values;
    private String valueType;
    private String valueTypeConstraint;
    private boolean isType;
    private String placeholder;
    private boolean optional;
    private boolean advanced;
    private Map<String, Value> properties;
    private List<String> items;
    private Codedata codedata;
    private List<Value> choices;
    private boolean addNewButton = false;

    public Value() {
        this(null, false, true, null, null, null, false, null, false, false, null, null, null);
    }

    public Value(String value, String valueType, boolean isEnabled) {
        this(null, isEnabled, true, value, valueType, null, false, null, false, false, null, null, null);
    }

    public Value(MetaData metadata, boolean enabled, boolean editable, String value, String valueType,
                 String valueTypeConstraint, boolean isType, String placeholder, boolean optional,
                 boolean advanced, Map<String, Value> properties, List<String> items, Codedata codedata) {
        this.metadata = metadata;
        this.enabled = enabled;
        this.editable = editable;
        this.value = value;
        this.valueType = valueType;
        this.valueTypeConstraint = valueTypeConstraint;
        this.isType = isType;
        this.placeholder = placeholder;
        this.optional = optional;
        this.advanced = advanced;
        this.properties = properties;
        this.items = items;
        this.codedata = codedata;
    }

    public MetaData getMetadata() {
        return metadata;
    }

    public void setMetadata(MetaData metadata) {
        this.metadata = metadata;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEnabledWithValue() {
        return enabled && ((value != null && !value.isEmpty()) || (placeholder != null && !placeholder.isEmpty())
                || (values != null && !values.isEmpty()));
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

    public String getValue() {
        List<String> values = this.values;
        if (Objects.nonNull(values) && !values.isEmpty()) {
            return String.join(", ", values);
        }
        return value;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public void addValue(String value) {
        if (Objects.isNull(this.values)) {
            this.values = List.of(value);
        } else {
            this.values.add(value);
        }
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getValueTypeConstraint() {
        return valueTypeConstraint;
    }

    public void setValueTypeConstraint(String valueTypeConstraint) {
        this.valueTypeConstraint = valueTypeConstraint;
    }

    public boolean isType() {
        return isType;
    }

    public void setType(boolean isType) {
        this.isType = isType;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
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

    public Map<String, Value> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Value> properties) {
        this.properties = properties;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public Codedata getCodedata() {
        return codedata;
    }

    public void setCodedata(Codedata codedata) {
        this.codedata = codedata;
    }

    public List<Value> getChoices() {
        return choices;
    }

    public void setChoices(List<Value> choices) {
        this.choices = choices;
    }

    public Value getProperty(String key) {
        return properties.get(key);
    }

    public boolean isAddNewButton() {
        return addNewButton;
    }
}
