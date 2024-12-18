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

package io.ballerina.flowmodelgenerator.extension.request;

import java.util.List;

/**
 * Represents a request to generate the service from OpenAPI contract.
 *
 * @param openApiContractPath Location for OpenAPI contract
 * @param projectPath         Location for the generated services
 * @param name                Service type name
 * @param listeners           Listener names
 * @since 1.4.0
 */
public record OpenAPIServiceGenerationRequest(String openApiContractPath, String projectPath, String name,
                                              List<String> listeners) {

}
