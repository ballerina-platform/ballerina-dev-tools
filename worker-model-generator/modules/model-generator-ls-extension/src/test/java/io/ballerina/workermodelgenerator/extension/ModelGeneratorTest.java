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
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Test cases for the worker model generator service.
 *
 * @since 2201.9.0
 */
public class ModelGeneratorTest {

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

        boolean result = flow.equals(testConfig.getFlow());
        if (!result) {
//            updateConfig(configJsonPath, testConfig, flow);
            //TODO: Add a logger to display the difference
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.getDescription(), configJsonPath));
        }
    }

    public List<String> skipList() {
        return new ArrayList<>();
    }

    @DataProvider(name = "worker-model-data-provider")
    public Object[] getConfigsList() {
        List<String> skippedTests = this.skipList();
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
