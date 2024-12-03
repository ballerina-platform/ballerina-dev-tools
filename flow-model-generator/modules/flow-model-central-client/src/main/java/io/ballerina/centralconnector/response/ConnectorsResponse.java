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

package io.ballerina.centralconnector.response;

import org.ballerinalang.diagramutil.connector.models.connector.Connector;

import java.util.List;

/**
 * Represents a response containing a list of connectors along with pagination details.
 *
 * @param connectors The list of connectors
 * @param count      The total number of connectors
 * @param offset     The offset for pagination
 * @param limit      The limit for pagination
 * @since 2.0.0
 */
public record ConnectorsResponse(List<Connector> connectors, int count, int offset, int limit) {
}
