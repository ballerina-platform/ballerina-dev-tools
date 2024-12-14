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

package io.ballerina.designmodelgenerator.core;

import io.ballerina.designmodelgenerator.core.model.Connection;
import io.ballerina.designmodelgenerator.core.model.Listener;
import io.ballerina.designmodelgenerator.core.model.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Intermediate model to store the intermediate representation of the code.
 *
 * @since 2.0.0
 */
public class IntermediateModel {

    protected final Map<String, FunctionModel> functionModelMap;
    protected final Map<String, ServiceModel> serviceModelMap;
    protected final Map<String, Listener> listeners;
    protected final Map<String, Connection> connectionMap;

    public IntermediateModel() {
        this.functionModelMap = new HashMap<>();
        this.serviceModelMap = new HashMap<>();
        this.listeners = new HashMap<>();
        this.connectionMap = new HashMap<>();
    }

    public static class ServiceModel {
        protected List<FunctionModel> remoteFunctions;
        protected List<FunctionModel> resourceFunctions;
        protected List<FunctionModel> otherFunctions;
        protected Location location;
        protected String absolutePath;
        protected String displayName;
        protected String sortText;
        protected final List<String> namedListeners = new ArrayList<>();
        protected final List<Listener> anonListeners = new ArrayList<>();

        public ServiceModel(String displayName, String absolutePath, String sortText, Location location) {
            this.displayName = displayName;
            this.absolutePath = absolutePath;
            this.sortText = sortText;
            this.location = location;
            this.remoteFunctions = new ArrayList<>();
            this.resourceFunctions = new ArrayList<>();
            this.otherFunctions = new ArrayList<>();
        }
    }

    public static class FunctionModel {
        protected final String name;
        protected final Set<String> dependentFuncs;
        protected boolean analyzed;
        protected boolean visited;
        protected final Set<String> allDependentConnections;
        protected Location location;
        protected String path;
        protected String displayName;
        protected final Set<String> connections = new HashSet<>();

        public FunctionModel(String name) {
            this.name = name;
            this.dependentFuncs = new HashSet<>();
            this.analyzed = false;
            this.visited = false;
            this.allDependentConnections = new HashSet<>();
        }
    }
}
