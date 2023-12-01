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

package io.ballerina.sequencemodelgenerator.core;

/**
 * Constants used for Sequence Diagram model generation errors.
 *
 * @since 2201.8.5
 */
public class Constants {
    public static final String EMPTY_SEMANTIC_MODEL_MSG =
            "Provided Ballerina file path doesn't contain a valid semantic model";
    public static final String INVALID_NODE_MSG = "Couldn't find a valid node at the given position";
    public static final String ISSUE_IN_VISITING_ROOT_NODE = "Error occurred while visiting root node  : %s";
    public static final String UNABLE_TO_FIND_SYMBOL = "Unable to find symbol for the given node";
    public static final String ISSUE_IN_MODEL_GENERATION =
            "Error occurred while visiting nodes to generate the model : %s";
}
