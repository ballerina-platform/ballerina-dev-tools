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

package io.ballerina.workermodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.workermodelgenerator.core.model.Flow;
import io.ballerina.workermodelgenerator.core.model.WorkerNode;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Test cases for the worker model generator service.
 *
 * @since 2201.9.0
 */
public class ModelGeneratorTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModelGeneratorTest.class);
    private static final Path RES_DIR = Paths.get("src/test/resources/").toAbsolutePath();
    private static final Path CONFIG_DIR = RES_DIR.resolve("config");
    private static final Path SOURCE_DIR = RES_DIR.resolve("source");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private Endpoint serviceEndpoint;
    private BallerinaLanguageServer languageServer;

    @BeforeClass
    public void init() {
        this.languageServer = new BallerinaLanguageServer();
        TestUtil.LanguageServerBuilder builder = TestUtil.newLanguageServer()
                .withLanguageServer(languageServer);
        this.serviceEndpoint = builder.build();
    }

    @Test(dataProvider = "worker-model-data-provider")
    public void testGeneratedModel(Path config) throws IOException {
        Path configJsonPath = CONFIG_DIR.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String response = getResponse(testConfig.getSource(), testConfig.start, testConfig.end);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonObject jsonModel = json.getAsJsonObject("result").getAsJsonObject("workerDesignModel");
        Flow flow = gson.fromJson(jsonModel, Flow.class);

        // Assert only the file name since the absolute path may vary depending on the machine
        String balFileName = Path.of(flow.fileName()).getFileName().toString();
        boolean fileNameEquality = balFileName.equals(testConfig.getFlow().fileName());
        Flow modifiedFlow = new Flow(flow.id(), flow.name(), balFileName, flow.bodyCodeLocation(),
                flow.fileSourceRange(), flow.endpoints(), flow.nodes());

        boolean flowEquality = modifiedFlow.equals(testConfig.getFlow());
        if (!fileNameEquality || !flowEquality) {
//            updateConfig(configJsonPath, testConfig, modifiedFlow);
            logModelDifference(testConfig.getFlow(), modifiedFlow);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.getDescription(), configJsonPath));
        }
    }

    public String[] skipList() {
        return new String[]{};
    }

    @DataProvider(name = "worker-model-data-provider")
    public Object[] getConfigsList() {
//        return new Object[]{Path.of("transform_with_error.json")};
        List<String> skippedTests = Arrays.stream(this.skipList()).toList();
        try {
            return Files.walk(CONFIG_DIR)
                    .filter(path -> {
                        File file = path.toFile();
                        return file.isFile() && file.getName().endsWith(".json")
                                && !skippedTests.contains(file.getName());
                    })
                    .toArray();
        } catch (IOException e) {
            // If failed to load tests, then it's a failure
            Assert.fail("Unable to load test config", e);
            return new Object[0][];
        }
    }

    private String getResponse(String source, LinePosition start, LinePosition end) throws IOException {
        CompletableFuture<?> result = this.serviceEndpoint.request("workerDesignService/getWorkerDesignModel",
                new WorkerModelGeneratorServiceRequest(
                        SOURCE_DIR.resolve(source).toAbsolutePath().toString(), start, end));
        return TestUtil.getResponseString(result);
    }

    private void updateConfig(Path configJsonPath, TestConfig testConfig, Flow responseFlow) throws IOException {
        TestConfig updatedConfig = new TestConfig();
        updatedConfig.setDescription(testConfig.getDescription());
        updatedConfig.setSource(testConfig.getSource());
        updatedConfig.setStart(testConfig.getStart());
        updatedConfig.setEnd(testConfig.getEnd());
        updatedConfig.setFlow(responseFlow);

        String objStr = gson.toJson(updatedConfig).concat(System.lineSeparator());
        Files.writeString(configJsonPath, objStr);
    }

    private void logModelDifference(Flow expectedFlow, Flow actualFlow) {
        logPropertyDifference("id", expectedFlow.id(), actualFlow.id());
        logPropertyDifference("name", expectedFlow.name(), actualFlow.name());
        logPropertyDifference("fileName", expectedFlow.fileName(), actualFlow.fileName());

        List<WorkerNode> missingNodes = new ArrayList<>();
        List<WorkerNode> irrelevantNodes = new ArrayList<>(actualFlow.nodes());
        if (expectedFlow.nodes() == null) {
            LOG.info("No worker nodes found in the response");
            return;
        }
        for (WorkerNode expectedNode : expectedFlow.nodes()) {
            boolean removed = irrelevantNodes.remove(expectedNode);
            if (!removed) {
                missingNodes.add(expectedNode);
            }
        }
        if (!missingNodes.isEmpty() || !irrelevantNodes.isEmpty()) {
            LOG.info("Worker nodes which are in response but not in test config : " + irrelevantNodes);
            LOG.info("Worker nodes which are in test config but not in response : " + missingNodes);
        }
    }

    private void logPropertyDifference(String name, String expected, String actual) {
        if (!Objects.equals(expected, actual)) {
            LOG.info(String.format("Expected %s=(%s), but found %s=(%s)", name, expected, name, actual));
        }
    }

    @AfterClass
    public void shutDownLanguageServer() {
        TestUtil.shutdownLanguageServer(this.serviceEndpoint);
        this.languageServer = null;
        this.serviceEndpoint = null;
    }

    /**
     * Represents the test configuration.
     */
    private static class TestConfig {

        private LinePosition start;
        private LinePosition end;
        private String source;
        private String description;
        private Flow flow;

        public LinePosition getStart() {
            return start;
        }

        public LinePosition getEnd() {
            return end;
        }

        public String getSource() {
            return source;
        }

        public String getDescription() {
            return description == null ? "" : description;
        }

        public Flow getFlow() {
            return flow;
        }

        public void setStart(LinePosition start) {
            this.start = start;
        }

        public void setEnd(LinePosition end) {
            this.end = end;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setFlow(Flow flow) {
            this.flow = flow;
        }
    }
}
