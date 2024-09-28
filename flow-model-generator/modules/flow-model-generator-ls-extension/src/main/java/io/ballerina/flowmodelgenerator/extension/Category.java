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

package io.ballerina.flowmodelgenerator.extension;

import org.ballerinalang.diagramutil.connector.models.connector.Type;

import java.util.List;

/**
 * Represents a category of variables.
 *
 * @param name  the name of the category
 * @param types the list of variables in the category
 * @since 1.4.0
 */
public record Category(String name, List<Variable> types) {

    public static final String MODULE_CATEGORY = "Module Variables";
    public static final String CONFIGURABLE_CATEGORY = "Configurable Variables";
    public static final String LOCAL_CATEGORY = "Local Variables";

    public record Variable(String name, Type type) {
    }

}
