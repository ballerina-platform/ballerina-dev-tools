package io.ballerina.triggermodelgenerator.extension.model;

public class Parameter {
    private MetaData metadata;
    private String kind;
    private Value type;
    private Value name;
    private Value defaultValue;
    private boolean enabled;
    private boolean editable;
    private boolean optional;

    public Parameter() {
        this(null, null, null, null, null, false, false, false);
    }

    public Parameter(MetaData metadata, String kind, Value type, Value name, Value defaultValue, boolean enabled,
                     boolean editable, boolean optional) {
        this.metadata = metadata;
        this.kind = kind;
        this.type = type;
        this.name = name;
        this.defaultValue = defaultValue;
        this.enabled = enabled;
        this.editable = editable;
        this.optional = optional;
    }

    public static Parameter getNewParameter() {
        return new Parameter(null, null, new Value(), new Value(), null, false, false, false);
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
}
