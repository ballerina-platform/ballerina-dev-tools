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

package io.ballerina.flowmodelgenerator.core;

import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;

/**
 * Util to build complex syntax tree nodes.
 *
 * @since 1.4.0
 */
public class SyntaxNodeBuilder {

    public static TemplateExpressionNode createXMLTemplateExpressionNode() {
        return NodeFactory.createTemplateExpressionNode(
                SyntaxKind.XML_TEMPLATE_EXPRESSION,
                NodeFactory.createToken(SyntaxKind.XML_KEYWORD),
                NodeFactory.createToken(SyntaxKind.BACKTICK_TOKEN),
                NodeFactory.createNodeList(),
                NodeFactory.createToken(SyntaxKind.BACKTICK_TOKEN));
    }
}
