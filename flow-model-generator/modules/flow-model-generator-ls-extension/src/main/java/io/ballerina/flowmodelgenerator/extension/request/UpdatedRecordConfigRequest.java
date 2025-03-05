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

import io.ballerina.flowmodelgenerator.core.model.Codedata;

/**
 * A request to configure to obtain the record type model to configure a record.
 *
 * @param filePath       path to the respective file
 * @param codedata       code data of the type
 * @param typeConstraint type constraint for the record
 * @param expr           expression to be updated
 * @since 2.0.0
 */
public record UpdatedRecordConfigRequest(String filePath, Codedata codedata, String typeConstraint, String expr) {
}
