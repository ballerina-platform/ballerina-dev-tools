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

package io.ballerina.modelgenerator.commons;

/**
 * Represents an annotation.
 *
 * @param annotationName The name of the annotation
 * @param displayName The display name of the annotation
 * @param description The description of the annotation
 * @param typeConstrain The type constrain of the annotation
 * @param packageIdentifier The package identifier of the annotation
 * @param orgName The organization name of the annotation
 * @param moduleName The module name of the annotation
 *
 * @since 2.2.0
 */
public record Annotation(
        String annotationName,
        String displayName,
        String description,
        String typeConstrain,
        String packageIdentifier,
        String orgName,
        String moduleName
) {
}

