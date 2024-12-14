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

package io.ballerina.designmodelgenerator.core.model;

import io.ballerina.designmodelgenerator.core.CommonUtils;

/**
 * Represents the design graph of a Ballerina package.
 *
 * @since 2.0.0
 */
public abstract class DesignGraphNode {

    private final String uuid;
    private boolean enableFlowModel = false;

    public DesignGraphNode(boolean enableFlowModel) {
        this.uuid = CommonUtils.generateUUID();
        this.enableFlowModel = enableFlowModel;
    }

    public DesignGraphNode() {
        this.uuid = CommonUtils.generateUUID();
    }

    public String getUuid() {
        return uuid;
    }

    public boolean isFlowModelEnabled() {
        return enableFlowModel;
    }
}
