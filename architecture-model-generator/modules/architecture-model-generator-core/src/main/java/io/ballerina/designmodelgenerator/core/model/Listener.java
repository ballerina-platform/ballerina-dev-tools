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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a listener declaration node.
 *
 * @since 2.0.0
 */
public final class Listener extends DesignGraphNode {

    private final String symbol;
    private final Location location;
    private final Set<String> attachedServices;
    private final Kind kind;
    private final String type;
    private final List<KeyValue> args;
    private final String icon;

    public Listener(String symbol, String sortText, Location location, String type, String icon,
                    Kind kind, List<KeyValue> args) {
        super(sortText);
        this.symbol = symbol;
        this.location = location;
        this.kind = kind;
        this.attachedServices = new HashSet<>();
        this.type = type;
        this.icon = icon;
        this.args = args;
    }

    public Listener(String symbol, String sortText, Location location, String type, String icon, Kind kind,
                    List<KeyValue> args, boolean enableFlow) {
        super(enableFlow, sortText);
        this.symbol = symbol;
        this.location = location;
        this.kind = kind;
        this.attachedServices = new HashSet<>();
        this.type = type;
        this.icon = icon;
        this.args = args;
    }

    public String getIcon() {
        return icon;
    }

    public String getSymbol() {
        return symbol;
    }

    public Location getLocation() {
        return location;
    }

    public Set<String> getAttachedServices() {
        return attachedServices;
    }

    public Kind getKind() {
        return kind;
    }

    public String getType() {
        return type;
    }

    public List<KeyValue> getArgs() {
        return args;
    }

    public enum Kind {
        ANON, NAMED, IMPORTED
    }

    public record KeyValue(String key, String value) {
    }
}
