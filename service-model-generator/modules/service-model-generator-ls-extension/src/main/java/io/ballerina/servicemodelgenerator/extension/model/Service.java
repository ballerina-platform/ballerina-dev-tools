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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.ballerina.servicemodelgenerator.extension.util.Utils.getValueString;

/**
 * Represents a service declaration.
 *
 * @since 2.0.0
 */
public class Service {
    private final String id;
    private final String name;
    private final String type;
    private final String displayName;
    private final String description;
    private final DisplayAnnotation displayAnnotation;
    private final String moduleName;
    private final String orgName;
    private final String version;
    private final String packageName;
    private final String listenerProtocol;
    private final String icon;
    private Map<String, Value> properties;
    private Codedata codedata;
    private List<Function> functions;

    public Service(String id, String name, String type, String displayName, String description,
                   DisplayAnnotation displayAnnotation, String moduleName, String orgName, String version,
                   String packageName, String listenerProtocol, String icon, Map<String, Value> properties,
                   Codedata codedata, List<Function> functions) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.displayAnnotation = displayAnnotation;
        this.moduleName = moduleName;
        this.orgName = orgName;
        this.version = version;
        this.packageName = packageName;
        this.listenerProtocol = listenerProtocol;
        this.icon = icon;
        this.properties = properties;
        this.functions = functions;
        this.codedata = codedata;
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

    public Value getListener() {
        return properties.get("listener");
    }

    public Value getProperty(String key) {
        return properties.get(key);
    }

    public void setServiceType(Value serviceType) {
        properties.put("serviceType", serviceType);
    }

    public Value getServiceType() {
        return properties.get("serviceType");
    }

    public String getServiceTypeName() {
        Value serviceType = properties.get("serviceType");
        if (Objects.isNull(serviceType)) {
            return null;
        }
        return listenerProtocol + ":" + getValueString(serviceType);
    }

    public Value getBasePath() {
        return properties.get("basePath");
    }

    public Value getStringLiteralProperty() {
        return properties.get("stringLiteral");
    }

    public void setBasePath(Value basePath) {
        properties.put("basePath", basePath);
    }

    public void setStringLiteral(Value basePath) {
        properties.put("stringLiteral", basePath);
    }

    public Value getOpenAPISpec() {
        return properties.get("spec");
    }

    public Value getServiceContractTypeNameValue() {
        return properties.get("serviceTypeName");
    }

    public String getServiceContractTypeName() {
        Value serviceContractType = properties.get("serviceTypeName");
        if (Objects.isNull(serviceContractType)) {
            return "Service";
        }
        return getValueString(serviceContractType);
    }

    public void setServiceContractTypeName(Value serviceContractType) {
        properties.put("serviceTypeName", serviceContractType);
    }

    public Value getDesignApproach() {
        return properties.get("designApproach");
    }

    public String getOrgName() {
        return orgName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getListenerProtocol() {
        return listenerProtocol;
    }

    public String getIcon() {
        return icon;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public DisplayAnnotation getDisplayAnnotation() {
        return displayAnnotation;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, Value> getProperties() {
        return properties;
    }

    public void addProperties(Map<String, Value> properties) {
        if (Objects.isNull(properties)) {
            return;
        }
        if (Objects.nonNull(this.properties)) {
            this.properties.putAll(properties);
        } else {
            this.properties = properties;
        }
    }

    public static Service getEmptyServiceModel() {
        return new Service.ServiceModelBuilder()
                .setFunctions(new ArrayList<>())
                .setProperties(new HashMap<>())
                .build();
    }

    public static class ServiceModelBuilder {
        private String id;
        private String name;
        private String type;
        private String displayName;
        private String description;
        private DisplayAnnotation displayAnnotation;
        private String moduleName;
        private String orgName;
        private String version;
        private String packageName;
        private String listenerProtocol;
        private String icon;
        private Map<String, Value> properties;
        private Codedata codedata;
        private List<Function> functions;

        public ServiceModelBuilder() {
            this.properties = new HashMap<>();
            this.functions = new ArrayList<>();
        }

        public ServiceModelBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public ServiceModelBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public ServiceModelBuilder setType(String type) {
            this.type = type;
            return this;
        }

        public ServiceModelBuilder setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public ServiceModelBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public ServiceModelBuilder setDisplayAnnotation(DisplayAnnotation displayAnnotation) {
            this.displayAnnotation = displayAnnotation;
            return this;
        }

        public ServiceModelBuilder setModuleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public ServiceModelBuilder setOrgName(String orgName) {
            this.orgName = orgName;
            return this;
        }

        public ServiceModelBuilder setVersion(String version) {
            this.version = version;
            return this;
        }

        public ServiceModelBuilder setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public ServiceModelBuilder setListenerProtocol(String listenerProtocol) {
            this.listenerProtocol = listenerProtocol;
            return this;
        }

        public ServiceModelBuilder setIcon(String icon) {
            this.icon = icon;
            return this;
        }

        public ServiceModelBuilder setProperties(Map<String, Value> properties) {
            this.properties = properties;
            return this;
        }

        public ServiceModelBuilder setCodedata(Codedata codedata) {
            this.codedata = codedata;
            return this;
        }

        public ServiceModelBuilder setFunctions(List<Function> functions) {
            this.functions = functions;
            return this;
        }

        public Service build() {
            return new Service(id, name, type, displayName, description, displayAnnotation, moduleName, orgName,
                    version, packageName, listenerProtocol, icon, properties, codedata, functions);
        }
    }
}
