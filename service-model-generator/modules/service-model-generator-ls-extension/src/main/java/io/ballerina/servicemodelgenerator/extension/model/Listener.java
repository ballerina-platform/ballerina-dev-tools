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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.ballerina.servicemodelgenerator.extension.util.Utils.getValueString;

public class Listener {
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

    public Listener(String id, String name, String type, String displayName, String description,
                    DisplayAnnotation displayAnnotation, String moduleName, String orgName, String version,
                    String packageName, String listenerProtocol, String icon, Map<String, Value> properties,
                    Codedata codedata) {
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
        this.codedata = codedata;
    }

    public boolean isListenerInitProperty(Value value) {
        Codedata codedata = value.getCodedata();
        return Objects.nonNull(codedata) && Objects.nonNull(codedata.getType()) &&
                codedata.getType().equals("LISTENER_INIT_PARAM");
    }

    public boolean isRequiredArgument(Value value) {
        Codedata codedata = value.getCodedata();
        return Objects.nonNull(codedata) && Objects.nonNull(codedata.getArgType()) &&
                codedata.getArgType().equals("REQUIRED");
    }

    public String getDeclaration() {
        List<String> params = new ArrayList<>();
        StringBuilder listenerDeclaration = new StringBuilder();
        listenerDeclaration.append("listener ");
        listenerDeclaration.append(listenerProtocol);
        listenerDeclaration.append(":Listener ");
        listenerDeclaration.append(getValueString(properties.get("name")));
        listenerDeclaration.append(" = new ");
        listenerDeclaration.append("(");
        properties.forEach((key, value) -> {
            if (value.isEnabledWithValue() && isListenerInitProperty(value)) {
                if (isRequiredArgument(value)) {
                    params.add(getValueString(value));
                } else {
                    params.add(String.format("%s = %s", key, getValueString(value)));
                }
            }
        });
        listenerDeclaration.append(String.join(", ", params));
        listenerDeclaration.append(");");
        return listenerDeclaration.toString();
    }

    public String getOrgName() {
        return orgName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Value getProperty(String key) {
        return properties.get(key);
    }

    public Map<String, Value> getProperties() {
        return properties;
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

    public String getPackageName() {
        return packageName;
    }

    public Codedata getCodedata() {
        return codedata;
    }

    public void setCodedata(Codedata codedata) {
        this.codedata = codedata;
    }

    public void setProperties(Map<String, Value> properties) {
        this.properties = properties;
    }
}
