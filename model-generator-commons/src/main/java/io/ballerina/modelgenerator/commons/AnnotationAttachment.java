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

package io.ballerina.modelgenerator.commons;

import io.ballerina.compiler.api.symbols.AnnotationAttachPoint;

import java.util.List;

/**
 * Represents an annotation attachment.
 *
 * @param annotName The name of the annotation
 * @param attachmentPoints The attachment points of the annotation
 * @param displayName The display name of the annotation
 * @param description The description of the annotation
 * @param typeName The type name of the annotation
 * @param packageInfo The package information of the annotation
 *
 * @since 2.0.0
 */
public record AnnotationAttachment(String annotName, List<AnnotationAttachPoint> attachmentPoints, String displayName,
                                   String description, String typeName, String packageInfo) {
}
