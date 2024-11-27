package io.ballerina.triggermodelgenerator.extension.model;

import java.util.ArrayList;
import java.util.List;

public class Service {
    private MetaData metadata;
    private Codedata codedata;
    private Value serviceType;
    private boolean enabled;
    private List<Function> functions;

    public Service() {
        this(null,null, null, false, null);
    }

    public Service(MetaData metadata, Codedata codedata, Value serviceType, boolean enabled, List<Function> functions) {
        this.metadata = metadata;
        this.serviceType = serviceType;
        this.enabled = enabled;
        this.functions = functions;
        this.codedata = codedata;
    }

    public static Service getNewService() {
        return new Service(null, null, new Value(), false, new ArrayList<>());
    }

    public MetaData getMetadata() {
        return metadata;
    }

    public void setMetadata(MetaData metadata) {
        this.metadata = metadata;
    }

    public Value getServiceType() {
        return serviceType;
    }

    public void setServiceType(Value serviceType) {
        this.serviceType = serviceType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public void setFunctions(List<Function> functions) {
        this.functions = functions;
    }

    public void addFunction(Function function) {
        this.functions.add(function);
    }

    public Codedata getCodedata() {
        return codedata;
    }

    public void setCodedata(Codedata codedata) {
        this.codedata = codedata;
    }
}
