/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.workermodelgenerator.core;

import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.tools.text.LineRange;
import io.ballerina.workermodelgenerator.core.model.CodeLocation;

/**
 * Common utility functions used in the project.
 *
 * @since 2201.9.0
 */
public class CommonUtils {

    /**
     * Removes the quotes from the given string.
     *
     * @param inputString the input string
     * @return the string without quotes
     */
    public static String removeQuotes(String inputString) {
        return inputString.replaceAll("^\"|\"$", "");
    }

    /**
     * Returns the code location of the given node.
     *
     * @param node the node
     * @return the code location of the node
     */
    public static CodeLocation getCodeLocationFromNode(NonTerminalNode node) {
        LineRange lineRange = node.lineRange();
        return new CodeLocation(lineRange.startLine(), lineRange.endLine());
    }
}
