/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.testmanagerservice.extension;

import io.ballerina.testmanagerservice.extension.model.FunctionTreeNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holder class to store all the test details of a module.
 *
 * @since 2.0.0
 */
public class ModuleTestDetailsHolder {

    private final Map<String, List<FunctionTreeNode>> groupsToFunctions;

    public ModuleTestDetailsHolder() {
        this.groupsToFunctions = new HashMap<>();
    }

    public void addTestFunction(String group, FunctionTreeNode testFunction) {
        List<FunctionTreeNode> testFunctions = this.groupsToFunctions.get(group);
        if (testFunctions == null) {
            testFunctions = new ArrayList<>();
            testFunctions.add(testFunction);
            this.groupsToFunctions.put(group, testFunctions);
        } else {
            testFunctions.add(testFunction);
        }
    }

    public void addTestFunctions(List<String> groups, FunctionTreeNode testFunction) {
        for (String group : groups) {
            addTestFunction(group, testFunction);
        }
    }

    public Map<String, List<FunctionTreeNode>> getGroupsToFunctions() {
        return groupsToFunctions;
    }
}
