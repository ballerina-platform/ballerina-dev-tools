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

import io.ballerina.artifactsgenerator.Artifact;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.ballerinalang.langserver.LSContextOperation;
import org.ballerinalang.langserver.commons.DocumentServiceContext;
import org.ballerinalang.langserver.commons.client.ExtendedLanguageClient;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.ballerinalang.langserver.contexts.ContextBuilder;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Test cases for publishing artifacts.
 *
 * @since 2.0.0
 */
public class PublishArtifactsSubscriberTest extends AbstractLSTest {

    private final PublishArtifactsSubscriber publishArtifactsSubscriber = new PublishArtifactsSubscriber();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        // Create a document service context
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        String sourcePath = getSourcePath(testConfig.source());
        Path filePath = Path.of(sourcePath);
        String fileUri;
        try {
            fileUri = new URI("file", "", sourcePath, null).toString();
        } catch (URISyntaxException e) {
            Assert.fail("Error while creating document service context", e);
            return;
        }
        DocumentServiceContext documentServiceContext = ContextBuilder.buildDocumentServiceContext(
                fileUri,
                workspaceManager,
                LSContextOperation.TXT_DID_CHANGE,
                languageServer.getServerContext()
        );

        VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier();
        List<TextDocumentContentChangeEvent> changeEvents =
                List.of(new TextDocumentContentChangeEvent(getText(sourcePath)));

        // Send the didChange notification
        try {
            workspaceManager.didChange(filePath,
                    new DidChangeTextDocumentParams(versionedTextDocumentIdentifier, changeEvents));
        } catch (WorkspaceDocumentException e) {
            Assert.fail("Error while sending didChange notification", e);
        }

        // Create a mock client using Mockito
        ExtendedLanguageClient mockClient = Mockito.mock(ExtendedLanguageClient.class);

        // Invoke the subscriber with mock client
        publishArtifactsSubscriber.onEvent(
                mockClient,
                documentServiceContext,
                languageServer.getServerContext());

        // Capture the artifacts published to the client - they are Object[] arrays
        ArgumentCaptor<Object[]> artifactsCaptor = ArgumentCaptor.forClass(Object[].class);
        Mockito.verify(mockClient).publishArtifacts(artifactsCaptor.capture());
        List<Artifact> publishedArtifacts = Arrays.stream(artifactsCaptor.getValue())
                .map(obj -> (Artifact) obj)
                .sorted(Comparator.comparing(Artifact::id))
                .toList();
        List<Artifact> expectedArtifacts = testConfig.output();

        // Assert the published artifacts
        if (!assertArray("publish artifacts", expectedArtifacts, publishedArtifacts)) {
//            updateConfig(configJsonPath,
//                    new TestConfig(testConfig.source(), testConfig.description(), publishedArtifacts));
            compareJsonElements(gson.toJsonTree(publishedArtifacts), gson.toJsonTree(expectedArtifacts));
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.source(), configJsonPath));
        }

    }

    @Override
    protected String getResourceDir() {
        return "publish_artifacts";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return PublishArtifactsSubscriberTest.class;
    }

    @Override
    protected String getApiName() {
        return "publishArtifacts";
    }

    /**
     * Represents the test configuration for the publish artifacts test.
     *
     * @param source      The source file
     * @param description The description of the test
     */
    private record TestConfig(String source, String description, List<Artifact> output) {

        public String description() {
            return description == null ? "" : description;
        }
    }

}
