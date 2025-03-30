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

package io.ballerina.designmodelgenerator.extension;

import io.ballerina.artifactsgenerator.ArtifactGenerationDebouncer;
import io.ballerina.artifactsgenerator.EventGenerator;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.DocumentServiceContext;
import org.ballerinalang.langserver.commons.LanguageServerContext;
import org.ballerinalang.langserver.commons.client.ExtendedLanguageClient;
import org.ballerinalang.langserver.commons.eventsync.EventKind;
import org.ballerinalang.langserver.commons.eventsync.spi.EventSubscriber;

import java.util.Optional;

/**
 * Publishes the artifacts to the client.
 *
 * @since 2.3.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.eventsync.spi.EventSubscriber")
public class PublishArtifactsSubscriber implements EventSubscriber {

    public static final String NAME = "Publish artifacts subscriber";

    @Override
    public EventKind eventKind() {
        return EventKind.PROJECT_UPDATE;
    }

    @Override
    public void onEvent(ExtendedLanguageClient client, DocumentServiceContext context,
                        LanguageServerContext serverContext) {
        Optional<SyntaxTree> syntaxTree = context.currentSyntaxTree();
        Optional<SemanticModel> semanticModel = context.currentSemanticModel();
        if (syntaxTree.isEmpty() || semanticModel.isEmpty()) {
            return;
        }

        // Use the debouncer to schedule the artifact generation
        ArtifactGenerationDebouncer.getInstance().debounce(context.fileUri(), () -> {
            client.publishArtifacts(EventGenerator.artifactChanges(syntaxTree.get(), semanticModel.get()));
        });
    }

    @Override
    public String getName() {
        return NAME;
    }
}
