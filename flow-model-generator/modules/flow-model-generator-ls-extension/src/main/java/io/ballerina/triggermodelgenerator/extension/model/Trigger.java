package io.ballerina.triggermodelgenerator.extension.model;

import java.util.List;

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
    private Value listener;
    private List<Service> services;

    public Trigger() {
        this(0, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Trigger(int id, String name, String type, String displayName, String documentation, String moduleName,
                   String orgName, String version, String packageName, String listenerProtocol, String icon,
                   DisplayAnnotation displayAnnotation, Value listener, List<Service> services) {
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
        this.listener = listener;
        this.services = services;
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

    public Value getListener() {
        return listener;
    }

    public void setListener(Value listener) {
        this.listener = listener;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }
}
