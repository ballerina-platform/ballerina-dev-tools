package io.ballerina.triggermodelgenerator.extension.model;

import io.ballerina.triggermodelgenerator.extension.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.triggermodelgenerator.extension.Utils.getValueString;

public class Trigger {
    private int id;
    private String name;
    private String type;
    private String displayName;
    private String documentation;
    private String moduleName;
    private String orgName;
    private String version;
    private String packageName;
    private String listenerProtocol;
    private String icon;
    private DisplayAnnotation displayAnnotation;
    private Map<String, Value> properties;
    private Service service;

    public Trigger() {
        this(0, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Trigger(int id, String name, String type, String displayName, String documentation, String moduleName,
                   String orgName, String version, String packageName, String listenerProtocol, String icon,
                   DisplayAnnotation displayAnnotation, Map<String, Value> properties, Service service) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.displayName = displayName;
        this.documentation = documentation;
        this.moduleName = moduleName;
        this.orgName = orgName;
        this.version = version;
        this.packageName = packageName;
        this.listenerProtocol = listenerProtocol;
        this.icon = icon;
        this.displayAnnotation = displayAnnotation;
        this.properties = properties;
        this.service = service;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getListenerProtocol() {
        return listenerProtocol;
    }

    public void setListenerProtocol(String listenerProtocol) {
        this.listenerProtocol = listenerProtocol;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public DisplayAnnotation getDisplayAnnotation() {
        return displayAnnotation;
    }

    public void setDisplayAnnotation(DisplayAnnotation displayAnnotation) {
        this.displayAnnotation = displayAnnotation;
    }

    public Map<String, Value> getProperties() {
        return properties != null ? properties : new HashMap<>();
    }

    public Value getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }

    public void addProperty(String key, Value value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }

    public void setProperties(Map<String, Value> properties) {
        this.properties = properties;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getListenerDeclaration() {
        List<String> params = new ArrayList<>();
        properties.forEach((key, value) -> {
            if (isListenerInitProperty(value) && value.isEnabled()) {
                params.add(String.format("%s = %s", key, getValueString(value)));
            }
        });
        return String.format("new %s:Listener(%s)", listenerProtocol, String.join(", ", params));
    }

    public boolean isListenerInitProperty(Value value) {
        Codedata codedata = value.getCodedata();
        if (Objects.isNull(codedata)) {
            return false;
        }
        return codedata.isInListenerInit();
    }

    public boolean isBasePathProperty(Value value) {
        Codedata codedata = value.getCodedata();
        if (Objects.isNull(codedata)) {
            return false;
        }
        return codedata.isBasePath();
    }

    public Optional<Value> getBasePathProperty() {
        for (Map.Entry<String, Value> entry : properties.entrySet()) {
            if (isBasePathProperty(entry.getValue()) && entry.getValue().isEnabled()) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public Optional<String> getBasePath() {
        return getBasePathProperty().map(Utils::getValueString);
    }

    public void setBasePath(String basePath) {
        getBasePathProperty().ifPresent(value -> value.setValue(basePath));
    }
}
