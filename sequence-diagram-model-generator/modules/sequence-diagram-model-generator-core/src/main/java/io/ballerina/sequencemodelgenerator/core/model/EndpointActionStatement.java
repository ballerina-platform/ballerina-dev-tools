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

import io.ballerina.sequencemodelgenerator.core.model.Constants.ActionType;
import io.ballerina.sequencemodelgenerator.core.model.Constants.InteractionType;
import io.ballerina.tools.text.LineRange;

/**
 * Represents the statement with connector interactions.
 * example :  json response = check self.httpEp->/users/[name];
 *
 * @since 2201.8.5
 */
public class EndpointActionStatement extends Interaction {
    private final String actionName;
    private final String actionPath;
    private final String methodName;
    private final ActionType actionType;

    public EndpointActionStatement(String sourceId, String targetId, String actionName, String methodName,
                                   String actionPath, boolean isHiddenInSequenceDiagram,
                                   ActionType actionType, LineRange location) {
        super(sourceId, targetId, InteractionType.ENDPOINT_INTERACTION, isHiddenInSequenceDiagram, location);
        this.actionName = actionName;
        this.methodName = methodName;
        this.actionPath = actionPath;
        this.actionType = actionType;
    }

    public String getActionName() {
        return actionName;
    }

    public String getActionPath() {
        return actionPath;
    }

    public String getMethodName() {
        return methodName;
    }

    public ActionType getActionType() {
        return actionType;
    }
}
