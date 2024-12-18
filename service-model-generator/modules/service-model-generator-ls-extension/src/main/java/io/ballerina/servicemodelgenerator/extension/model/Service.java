/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
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

import static io.ballerina.servicemodelgenerator.extension.Utils.getValueString;

public class Service {
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

    public Service() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

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

    public static Service getNewService() {
        return new Service(null, null, null, null, null, null, null, null, null, null, null, null, new HashMap<>(),
                null, new ArrayList<>());
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

    public void updateServiceType(Value sourceServiceType) {
        Value targetServiceType = properties.get("serviceType");
        if (Objects.isNull(targetServiceType) || Objects.isNull(sourceServiceType)) {
            return;
        }
        targetServiceType.setEnabled(sourceServiceType.isEnabledWithValue());
        String serviceTypeName = sourceServiceType.getValue();
        String[] serviceTypeNames = serviceTypeName.split(":", 2);
        if (serviceTypeNames.length > 1) {
            serviceTypeName = serviceTypeNames[1];
        }
        targetServiceType.setValue(serviceTypeName);
        targetServiceType.setValueType(sourceServiceType.getValueType());
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

    public Value setBasePath(Value basePath) {
        return properties.put("basePath", basePath);
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
}
