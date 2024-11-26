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

package io.ballerina.flowmodelgenerator.extension.request;

import io.ballerina.tools.text.LinePosition;
import org.eclipse.lsp4j.SignatureHelpContext;

/**
 * Represents a request for expression editor signature help.
 *
 * @param filePath   The file path which contains the expression
 * @param expression The modified expression
 * @param startLine  The start line of the node
 * @param offset     The offset of cursor compared to the start of the expression
 * @param context    The signature help context
 */
public record ExpressionEditorSignatureRequest(String filePath, String expression, LinePosition startLine, int offset,
                                               SignatureHelpContext context) {
}
