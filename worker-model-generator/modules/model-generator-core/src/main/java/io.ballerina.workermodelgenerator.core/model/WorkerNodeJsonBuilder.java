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

import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.workermodelgenerator.core.TemplateKind;

/**
 * Builder for creating a {@link WorkerNode} instance
 *
 * @since 2201.9.0
 */
public interface WorkerNodeJsonBuilder {

    /**
     * Sets the id of the node.
     *
     * @param id id of the node
     */
    void setId(String id);

    /**
     * Sets the type of the node.
     *
     * @param templateKind type of the node
     */
    void setTemplateKind(TemplateKind templateKind);

    /**
     * Sets code location of the node.
     *
     * @param start start position of the node
     * @param end   end position of the node
     */
    void setCodeLocation(LinePosition start, LinePosition end);

    /**
     * Sets the location of the node in the canvas.
     *
     * @param x x coordinate of the node
     * @param y y coordinate of the node
     */
    void setCanvasPosition(int x, int y);

    /**
     * Adds an input port to the node.
     *
     * @param id     id of the port
     * @param type   type of the port
     * @param name   name of the port
     * @param sender id of the sender node
     */
    void addInputPort(String id, TypeDescKind type, String name, String sender);

    /**
     * Adds an output port to the node.
     *
     * @param id       id of the port
     * @param type     type of the port
     * @param receiver id of the receiver node
     */
    void addOutputPort(String id, TypeDescKind type, String receiver);

    /**
     * Builds the node.
     *
     * @return built node
     */
    WorkerNode build();
}
