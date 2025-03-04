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

package io.ballerina.flowmodelgenerator.extension.request;

import io.ballerina.tools.text.LineRange;

import java.util.Map;

/**
 * Represents a unified search request for connectors, functions, etc.
 *
 * @param searchKind the kind of search to perform (corresponds to SearchCommand.Kind)
 * @param filePath the path of the file (may be null for certain search kinds)
 * @param position the line range of the current position (may be null for certain search kinds)
 * @param queryMap the map containing queryMap parameters
 * @since 2.0.0
 */
public record SearchRequest(String searchKind, String filePath, LineRange position, Map<String, String> queryMap) {

}
