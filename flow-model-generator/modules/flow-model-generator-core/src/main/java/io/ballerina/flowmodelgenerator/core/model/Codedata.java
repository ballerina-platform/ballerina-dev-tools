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

package io.ballerina.flowmodelgenerator.core.model;

/**
 * Represents the properties that uniquely identifies a node in the diagram.
 *
 * @param node   The kind of the component
 * @param org    The organization which the component belongs to
 * @param module The module which the component belongs to
 * @param object The object of the component if it is a method or an action call
 * @param symbol The symbol of the component
 * @since 1.5.0
 */
public record Codedata(String node, String org, String module, String object, String symbol) {

}
