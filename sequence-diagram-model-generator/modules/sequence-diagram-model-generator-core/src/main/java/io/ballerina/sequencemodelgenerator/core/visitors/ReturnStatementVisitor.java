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

package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;

/**
 * Visitor to identify the return statements.
 *
 * @since 2201.8.5
 */
public class ReturnStatementVisitor extends NodeVisitor {
    private ReturnStatementNode returnStatement;

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        this.returnStatement = returnStatementNode;
    }

    public ReturnStatementNode getReturnStatement() {
        return returnStatement;
    }
}
