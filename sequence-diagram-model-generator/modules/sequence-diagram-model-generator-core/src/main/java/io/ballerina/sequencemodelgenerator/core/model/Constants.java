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

package io.ballerina.sequencemodelgenerator.core.model;

/**
 * Constants used in the sequence diagram model.
 *
 * @since 2201.8.0
 */
public class Constants {
//    public static final Map<SyntaxKind, String> TYPE_MAP;
    public static final String PARTICIPANT = "Participant";
    public static final String INTERACTION = "Interaction";

//    static {
//        Map<SyntaxKind, String> typeMap = new HashMap<>();
//        typeMap.put(SyntaxKind.STRING_LITERAL, "string");
//        typeMap.put(SyntaxKind.BOOLEAN_LITERAL, "boolean");
//        typeMap.put(SyntaxKind.DECIMAL_FLOATING_POINT_LITERAL_TOKEN, "float");
//        typeMap.put(SyntaxKind.NUMERIC_LITERAL, "decimal");
//        typeMap.put(SyntaxKind.DECIMAL_INTEGER_LITERAL_TOKEN, "float");
//        TYPE_MAP = Collections.unmodifiableMap(typeMap);
//    }

    /**
     * Enum for participant kind.
     */
    public enum ParticipantKind {
        WORKER,
        ENDPOINT,
    }

    /**
     * Enum for endpoint action types.
     */
    public enum ActionType {
        RESOURCE_ACTION,
        REMOTE_ACTION,
        ;
    }

    /**
     * Enum for interaction types.
     */
    public enum InteractionType {
        ENDPOINT_INTERACTION,
        FUNCTION_INTERACTION,
        METHOD_INTERACTION,
        RETURN_ACTION,
    }
}
