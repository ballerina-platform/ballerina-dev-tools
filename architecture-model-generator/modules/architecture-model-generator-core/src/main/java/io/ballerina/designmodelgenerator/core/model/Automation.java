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
import java.util.Objects;

/**
 * Represents the main function in a ballerina package.
 *
 * @since 2.0.0
 */
public final class Automation extends DesignGraphNode {

    private final String name;
    private final String displayName;
    private final Location location;
    private final List<String> connections;
    private final String type;

    public Automation(String name, String displayName, String sortText, Location location, List<String> connections) {
        super(true, sortText);
        this.name = name;
        this.displayName = displayName;
        this.location = location;
        this.connections = connections;
        this.type = "automation";
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Location getLocation() {
        return location;
    }

    public List<String> getConnections() {
        return connections;
    }

    @Override
    public int hashCode() {
       return Objects.hash(type.hashCode(), name.hashCode(), displayName.hashCode(), location.hashCode(),
               connections.size());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Automation automation)) {
            return false;
        }
        return automation.getType().equals(type) && automation.getName().equals(name) &&
                automation.getDisplayName().equals(displayName) && automation.getLocation().equals(location) &&
                automation.getConnections().size() == connections.size();
    }
}
