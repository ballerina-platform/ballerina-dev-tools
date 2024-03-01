/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.ballerina.tools.text.LinePosition;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Test cases for the flow model generator service.
 *
 * @since 2201.9.0
 */
public class ModelGeneratorTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModelGeneratorTest.class);
    private static final Path RES_DIR = Paths.get("src/test/resources/diagram_generator").toAbsolutePath();
    private static final Path CONFIG_DIR = RES_DIR.resolve("config");
    private static final Path SOURCE_DIR = RES_DIR.resolve("source");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private Endpoint serviceEndpoint;
    private BallerinaLanguageServer languageServer;

    @BeforeClass
    public void init() {
        this.languageServer = new BallerinaLanguageServer();
        TestUtil.LanguageServerBuilder builder = TestUtil.newLanguageServer().withLanguageServer(languageServer);
        this.serviceEndpoint = builder.build();
    }

    @Test(dataProvider = "flow-model-data-provider")
    public void testGeneratedModel(Path config) throws IOException {
        Path configJsonPath = CONFIG_DIR.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String response = getResponse(testConfig.source(), testConfig.start(), testConfig.end());
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonObject jsonModel = json.getAsJsonObject("result").getAsJsonObject("flowDesignModel");

        // Assert only the file name since the absolute path may vary depending on the machine
        String balFileName = Path.of(jsonModel.getAsJsonPrimitive("fileName").getAsString()).getFileName().toString();
        JsonPrimitive testFileName = testConfig.diagram().getAsJsonPrimitive("fileName");
        boolean fileNameEquality = testFileName != null && balFileName.equals(testFileName.getAsString());
        JsonObject modifiedDiagram = jsonModel.deepCopy();
        modifiedDiagram.addProperty("fileName", balFileName);

        boolean flowEquality = modifiedDiagram.equals(testConfig.diagram());
        if (!fileNameEquality || !flowEquality) {
            updateConfig(configJsonPath, testConfig, modifiedDiagram);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    private String[] skipList() {
        return new String[]{
                "flags2.json" // TODO: Need to set flags for remote functions
        };
    }

    @DataProvider(name = "flow-model-data-provider")
    private Object[] getConfigsList() {
//        return new Object[]{Path.of("if_node3.json")};
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
        CompletableFuture<?> result = this.serviceEndpoint.request("flowDesignService/getFlowDesignModel",
                new FlowModelGeneratorServiceRequest(
                        SOURCE_DIR.resolve(source).toAbsolutePath().toString(), start, end));
        return TestUtil.getResponseString(result);
    }

    private void updateConfig(Path configJsonPath, TestConfig testConfig, JsonObject responseDiagram)
            throws IOException {
        TestConfig updatedConfig = new TestConfig(testConfig.start(), testConfig.end(), testConfig.source(),
                testConfig.description(), responseDiagram);
        String objStr = gson.toJson(updatedConfig).concat(System.lineSeparator());
        Files.writeString(configJsonPath, objStr);
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
    private record TestConfig(LinePosition start, LinePosition end, String source, String description,
                              JsonObject diagram) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
