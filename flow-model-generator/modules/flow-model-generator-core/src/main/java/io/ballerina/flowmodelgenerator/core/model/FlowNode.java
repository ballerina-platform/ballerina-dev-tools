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

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a node in the flow model.
 *
 * @param id          The unique identifier of the node.
 * @param metadata    The metadata of the node.
 * @param codedata    The code data of the node.
 * @param returning   Whether the node is returning.
 * @param branches    The branches of the node.
 * @param properties  The properties of the node.
 * @param diagnostics The diagnostics of the node.
 * @param flags       The flags of the node.
 * @since 2.0.0
 */
public record FlowNode(
        String id,
        Metadata metadata,
        Codedata codedata,
        boolean returning,
        List<Branch> branches,
        Map<String, Property> properties,
        Diagnostics diagnostics,
        int flags
) {

    public Optional<Property> getProperty(String key) {
        return Optional.ofNullable(properties).map(props -> props.get(key));
    }

    public Optional<Branch> getBranch(String label) {
        return Optional.ofNullable(branches)
                .flatMap(bs -> bs.stream().filter(branch -> branch.label().equals(label)).findFirst());
    }

    public boolean hasFlag(int flag) {
        return (flags & flag) == flag;
    }

    public static final int NODE_FLAG_CHECKED = 1 << 0;
    public static final int NODE_FLAG_CHECKPANIC = 1 << 1;
    public static final int NODE_FLAG_FINAL = 1 << 2;
    public static final int NODE_FLAG_REMOTE = 1 << 10;
    public static final int NODE_FLAG_RESOURCE = 1 << 11;
}
