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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the design model of a Ballerina project.
 *
 * @param automation
 * @param connections
 * @param listeners
 * @param services
 *
 * @since 2.0.0
 */
public record DesignModel(Automation automation, List<Connection> connections,
                          List<Listener> listeners, List<Service> services) {

    /**
     * Builder used to create a DesignModel instance.
     */
    public static class DesignModelBuilder {
        private Automation automation;
        private List<Connection> connections;
        private List<Listener> listeners;
        private final List<Service> services = new ArrayList<>();

        public void setAutomation(Automation automation) {
            this.automation = automation;
        }

        public DesignModelBuilder setConnections(List<Connection> connections) {
            this.connections = connections;
            return this;
        }

        public DesignModelBuilder setListeners(List<Listener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public void addService(Service service) {
            this.services.add(service);
        }

        public DesignModel build() {
            return new DesignModel(automation, connections, listeners, services);
        }
    }
}
