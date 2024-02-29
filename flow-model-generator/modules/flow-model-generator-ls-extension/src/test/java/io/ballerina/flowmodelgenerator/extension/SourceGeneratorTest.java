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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.concurrent.CompletableFuture;

/**
 * Tests for the flow model source generator service.
 *
 * @since 2201.9.0
 */
public class SourceGeneratorTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModelGeneratorTest.class);
    private static final Path RES_DIR = Paths.get("src/test/resources/to_source").toAbsolutePath();
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
    public void testSourceCodeGeneration(Path config) throws IOException {
        Path configJsonPath = RES_DIR.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String response = getResponse(testConfig.diagram());
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        String outputSource = json.getAsJsonObject("result").getAsJsonPrimitive("source").getAsString();

        if (!testConfig.output().equals(outputSource)) {
            LOG.error("Generated source code for " + testConfig.description() + " does not match the expected source");
            LOG.error("Expected: " + testConfig.output());
            LOG.error("Actual: " + outputSource);
        }
    }

    private String[] skipList() {
        return new String[]{};
    }

    @DataProvider(name = "flow-model-data-provider")
    private Object[] getConfigsList() {
//        return new Object[]{Path.of("http_post_node4.json")};
        List<String> skippedTests = Arrays.stream(this.skipList()).toList();
        try {
            return Files.walk(RES_DIR)
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

    private String getResponse(JsonElement diagramNode) {
        CompletableFuture<?> result = this.serviceEndpoint.request("flowDesignService/getSourceCode",
                new FlowModelSourceGeneratorServiceRequest(diagramNode));
        return TestUtil.getResponseString(result);
    }

    @AfterClass
    public void shutDownLanguageServer() {
        TestUtil.shutdownLanguageServer(this.serviceEndpoint);
        this.languageServer = null;
        this.serviceEndpoint = null;
    }

    private record TestConfig(String description, JsonElement diagram, String output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
