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

package io.ballerina.graphqlmodelgenerator.core;

/**
 * Represents constants used by the model generator.
 *
 * @since 2201.5.0
 */
public class Constants {

    public static final String INVALID_NODE_MSG = "Provided ST node doesn't support Schema generation";
    public static final String EMPTY_SCHEMA_MSG = "Cannot generate complete Schema object for the provided ST node";
    public static final String EMPTY_SEMANTIC_MODEL_MSG =
            "Provided Ballerina file path doesn't contain a valid semantic model";
    public static final String UNEXPECTED_ERROR_MSG = "Unexpected error occurred while generating GraphQL model : %s";
    public static final String MODEL_GENERATION_ERROR_MSG = "Issue when generating the GraphQL model from Schema : %s";
}
