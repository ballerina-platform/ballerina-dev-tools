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

package io.ballerina.workermodelgenerator.core.model;

/**
 * Builder interface for creating a {@link Flow} instance.
 *
 * @since 2201.9.0
 */
public interface FlowJsonBuilder {

    /**
     * Sets the id of the flow.
     *
     * @param id id of the flow
     */
    void setId(String id);

    /**
     * Sets the name of the flow.
     *
     * @param name name of the flow
     */
    void setName(String name);

    /**
     * Sets the file path of the flow.
     *
     * @param filePath file path of the flow
     */
    void setFilePath(String filePath);

    /**
     * Adds a node to the flow.
     *
     * @param node node to be added
     */
    void addNode(WorkerNode node);

    /**
     * Sets the code location of the body of the flow.
     *
     * @param bodyCodeLocation code location of the body of the flow
     */
    void setBodyCodeLocation(CodeLocation bodyCodeLocation);

    /**
     * Builds the flow.
     *
     * @return built flow
     */
    Flow build();
}
