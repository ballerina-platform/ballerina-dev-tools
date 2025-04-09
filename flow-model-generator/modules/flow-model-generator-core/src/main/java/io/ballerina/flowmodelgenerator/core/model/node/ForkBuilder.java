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
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 *  OF ANY KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Represents the properties of fork node in the flow model.
 *
 * @since 2.0.0
 */
public class ForkBuilder extends ParallelFlowBuilder {

    public static final String LABEL = "Fork";
    public static final String DESCRIPTION = "Create parallel workers";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.FORK);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        // Generate the fork statement
        sourceBuilder.token()
                .keyword(SyntaxKind.FORK_KEYWORD)
                .openBrace();
        List<String> workerNames = generateWorkers(sourceBuilder);
        sourceBuilder.token().closeBrace();

        // Generate the wait statement
        Boolean isNew = sourceBuilder.flowNode.codedata().isNew();
        if (isNew != null && isNew) {
            sourceBuilder.token()
                    .name("map<any|error>")
                    .whiteSpace()
                    //TODO: Set this value dynamically after adding the line position to the getSourceCode API
                    .name("waitResult")
                    .keyword(SyntaxKind.EQUAL_TOKEN)
                    .keyword(SyntaxKind.WAIT_KEYWORD)
                    .keyword(SyntaxKind.OPEN_BRACE_TOKEN)
                    .name(String.join(",", workerNames))
                    .keyword(SyntaxKind.CLOSE_BRACE_TOKEN)
                    .endOfStatement();
        }

        return sourceBuilder.textEdit().build();
    }
}
