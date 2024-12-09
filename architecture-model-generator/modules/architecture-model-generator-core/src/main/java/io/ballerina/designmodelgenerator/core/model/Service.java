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
 * @param name name of the service
 * @param location location of the service
 * @param listener listener of the service
 * @param connections dependent connections of the service
 * @param functions normal function in the service
 * @param remoteFunctions remote function in the service
 * @param resourceFunctions resource function in the service
 *
 * @since 2.0.0
 */
public record Service(String name, Location location, String listener, List<String> connections,
                      List<Function> functions, List<Function> remoteFunctions,
                      List<ResourceFunction> resourceFunctions) {
}
