package io.ballerina.triggermodelgenerator.extension.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Service {
    private MetaData metadata;
    private Value serviceType;
    private Value listener;
    private boolean enabled;
    private List<Function> functions;
    private Map<String, Value> properties;

    public Service() {
        this(null, null, null, false, null, null);
    }

    public Service(MetaData metadata, Value serviceType, Value listener, boolean enabled,
                   List<Function> functions, Map<String, Value> properties) {
        this.metadata = metadata;
        this.serviceType = serviceType;
        this.listener = listener;
        this.enabled = enabled;
        this.functions = functions;
        this.properties = properties;
    }

    public static Service getNewService() {
        return new Service(null, new Value(), new Value(), false, new ArrayList<>(), Map.of("basePath", new Value()));
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

    public Value getBasePath() {
        return properties == null ? null : properties.get("basePath");
    }

    public void setBasePath(Value basePath) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.put("basePath", basePath);
    }

    public Value getListener() {
        return listener;
    }

    public void setListener(Value listener) {
        this.listener = listener;
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

    public Map<String, Value> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Value> properties) {
        this.properties = properties;
    }

    public Value getProperty(String key) {
        return properties == null ? null : properties.get(key);
    }
}
