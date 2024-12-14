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

package io.ballerina.designmodelgenerator.core.model;

import java.util.List;

/**
 * Represents a service definition node.
 *
 * @since 2.0.0
 */
public class Service extends DesignGraphNode {

    private final String displayName;
    private final Location location;
    private final String listener;
    private final List<String> connections;
    private final List<Function> functions;
    private final List<Function> remoteFunctions;
    private final List<ResourceFunction> resourceFunctions;
    private final String absolutePath;
    private final String sortText;
    private final String type;
    private final String icon;

    public Service(String name, String absolutePath, Location location, String sortText, String type, String icon,
                   String listener, List<String> connections,
                   List<Function> functions, List<Function> remoteFunctions, List<ResourceFunction> resourceFunctions) {
        super(true);
        this.displayName = name;
        this.absolutePath = absolutePath;
        this.location = location;
        this.sortText = sortText;
        this.type = type;
        this.icon = icon;
        this.listener = listener;
        this.connections = connections;
        this.functions = functions;
        this.remoteFunctions = remoteFunctions;
        this.resourceFunctions = resourceFunctions;
    }

    public String getIcon() {
        return icon;
    }

    public String getSortText() {
        return sortText;
    }

    public String getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Location getLocation() {
        return location;
    }

    public String getListener() {
        return listener;
    }

    public List<String> getConnections() {
        return connections;
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public List<Function> getRemoteFunctions() {
        return remoteFunctions;
    }

    public List<ResourceFunction> getResourceFunctions() {
        return resourceFunctions;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    @Override
    public int hashCode() {
        return location.startLine().hashCode() + location.endLine().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Service service)) {
            return false;
        }
        return service.displayName.equals(this.displayName)
                && service.absolutePath.equals(this.absolutePath)
                && service.listener.equals(this.listener)
                && service.connections.size() == this.connections.size()
                && service.functions.size() == this.functions.size()
                && service.remoteFunctions.size() == this.remoteFunctions.size()
                && service.resourceFunctions.size() == this.resourceFunctions.size();
    }
}
