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

import io.ballerina.sequencemodelgenerator.core.model.Constants.InteractionType;
import io.ballerina.tools.text.LineRange;

import static io.ballerina.sequencemodelgenerator.core.model.Constants.INTERACTION;

/**
 * Represents base interaction model.
 *
 * @since 2201.8.0
 */
public class Interaction extends DNode {
    private final String sourceId;
    private final String targetId;
    private final InteractionType interactionType;

    public Interaction(String sourceId, String targetId, InteractionType interactionType, boolean isHidden,
                       LineRange location) {
        super(INTERACTION, isHidden, location);
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.interactionType = interactionType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

}
