package io.ballerina.triggermodelgenerator.extension.model;

import java.util.ArrayList;
import java.util.List;

public class Function {
    private MetaData metadata;
    private List<String> qualifiers;
    private String kind;
    private Value accessor;
    private Value name;
    private List<Parameter> parameters;
    private Value returnType;
    private boolean enabled;
    private boolean optional;
    private boolean editable;
    private Codedata codedata;

    public Function() {
        this(null, null, null, null, null, null, null, false, false, false, null);
    }

    public Function(MetaData metadata, List<String> qualifiers, String kind, Value accessor, Value name,
                    List<Parameter> parameters, Value returnType, boolean enabled, boolean optional, boolean editable,
                    Codedata codedata) {
        this.metadata = metadata;
        this.qualifiers = qualifiers;
        this.kind = kind;
        this.accessor = accessor;
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.enabled = enabled;
        this.optional = optional;
        this.editable = editable;
        this.codedata = codedata;
    }

    public static Function getNewFunction() {
        return new Function(null, new ArrayList<>(), "DEFAULT", new Value(), new Value(), new ArrayList<>(), new Value(), false, false, false, null);
    }

    public MetaData getMetadata() {
        return metadata;
    }

    public void setMetadata(MetaData metadata) {
        this.metadata = metadata;
    }

    public List<String> getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(List<String> qualifiers) {
        this.qualifiers = qualifiers;
    }

    public void addQualifier(String qualifier) {
        this.qualifiers.add(qualifier);
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Value getAccessor() {
        return accessor;
    }

    public void setAccessor(Value accessor) {
        this.accessor = accessor;
    }

    public Value getName() {
        return name;
    }

    public void setName(Value name) {
        this.name = name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    public Value getReturnType() {
        return returnType;
    }

    public void setReturnType(Value returnType) {
        this.returnType = returnType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public Codedata getCodedata() {
        return codedata;
    }

    public void setCodedata(Codedata codedata) {
        this.codedata = codedata;
    }
}
