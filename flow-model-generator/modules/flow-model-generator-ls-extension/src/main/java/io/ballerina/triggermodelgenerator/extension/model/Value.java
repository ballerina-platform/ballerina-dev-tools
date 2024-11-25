package io.ballerina.triggermodelgenerator.extension.model;

import java.util.List;
import java.util.Map;

public class Value {
    private MetaData metadata;
    private boolean enabled;
    private boolean editable;
    private String value;
    private String valueType;
    private String valueTypeConstraint;
    private boolean isType;
    private String placeholder;
    private boolean optional;
    private boolean advanced;
    private Map<String, Value> properties;
    private List<String> items;

    public Value() {
        this(null, false, false, null, null, null, false, null, false, false, null, null);
    }

    public Value(MetaData metadata, boolean enabled, boolean editable, String value, String valueType,
                 String valueTypeConstraint, boolean isType, String placeholder, boolean optional,
                 boolean advanced, Map<String, Value> properties, List<String> items) {
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
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
}
