/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.Map;

/**
 * Represents the GraphQL model.
 *
 * @since 2201.5.0
 */
public class GraphqlModel {
    private final Service graphqlService;
    private final Map<String, RecordComponent> records;
    private final Map<String, ServiceClassComponent> serviceClasses;
    private final Map<String, EnumComponent> enums;
    private final Map<String, UnionComponent> unions;
    private final Map<String, InterfaceComponent> interfaces;
    private final Map<String, HierarchicalResourceComponent> hierarchicalResources;

    public GraphqlModel(Service graphqlService, Map<String, RecordComponent> records,
                        Map<String, ServiceClassComponent> serviceClasses, Map<String, EnumComponent> enums,
                        Map<String, UnionComponent> unions, Map<String, InterfaceComponent> interfaces,
                        Map<String, HierarchicalResourceComponent> hierarchicalResources) {
        this.graphqlService = graphqlService;
        this.records = records;
        this.serviceClasses = serviceClasses;
        this.enums = enums;
        this.unions = unions;
        this.interfaces = interfaces;
        this.hierarchicalResources = hierarchicalResources;
    }

    public Service getGraphqlService() {
        return graphqlService;
    }

    public Map<String, RecordComponent> getRecords() {
        return records;
    }

    public Map<String, ServiceClassComponent> getServiceClasses() {
        return serviceClasses;
    }

    public Map<String, EnumComponent> getEnums() {
        return enums;
    }

    public Map<String, UnionComponent> getUnions() {
        return unions;
    }

    public Map<String, InterfaceComponent> getInterfaces() {
        return interfaces;
    }

    public Map<String, HierarchicalResourceComponent> getHierarchicalResources() {
        return hierarchicalResources;
    }
}
