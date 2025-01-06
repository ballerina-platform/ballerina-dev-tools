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

package io.ballerina.sequencemodelgenerator.ls.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
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
 * Test class for sequence model generator service.
 *
 * @since 2.0.0
 */
public class ModelGeneratorTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModelGeneratorTest.class);
    private static final Path RES_DIR = Paths.get("src/test/resources/").toAbsolutePath();
    private static final Path CONFIG_DIR = RES_DIR.resolve("config");
    private static final Path SOURCE_DIR = RES_DIR.resolve("source");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private Endpoint serviceEndpoint;
    private BallerinaLanguageServer languageServer;
    private static final String SEQUENCE_DESIGN_SERVICE = "sequenceModelGeneratorService/getSequenceDiagramModel";

    @BeforeClass
    public void init() {
        this.languageServer = new BallerinaLanguageServer();
        TestUtil.LanguageServerBuilder builder = TestUtil.newLanguageServer()
                .withLanguageServer(languageServer);
        this.serviceEndpoint = builder.build();
    }

    @Test(dataProvider = "model-data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = CONFIG_DIR.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        JsonObject responseJson = getResponse(testConfig.source(), testConfig.start(), testConfig.end());
        Assert.assertNotNull(responseJson);
        Diagram jsonModel = gson.fromJson(responseJson.getAsJsonObject("sequenceDiagram"), Diagram.class);

        // Assert only the file name since the absolute path may vary depending on the machine
        LineRange location = jsonModel.location();
        String balFileName = Path.of(location.fileName()).getFileName().toString();
        LineRange testConfigLocation = testConfig.diagram().location();
        boolean fileNameEquality = testConfigLocation != null && balFileName.equals(testConfigLocation.fileName());
        Diagram modifiedDiagram = new Diagram(
                LineRange.from(balFileName, location.startLine(), location.endLine()), jsonModel.participants(),
                jsonModel.others());

        boolean flowEquality = modifiedDiagram.equals(testConfig.diagram());
        if (!fileNameEquality || !flowEquality) {
            TestConfig updatedTestConfig = new TestConfig(testConfig.source(), testConfig.description(),
                    testConfig.start(), testConfig.end(), modifiedDiagram);
//            updateConfig(configJsonPath, updatedTestConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @DataProvider(name = "model-data-provider")
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

    private JsonObject getResponse(String source, LinePosition start, LinePosition end) throws IOException {
        CompletableFuture<?> result = this.serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE,
                new SequenceDiagramServiceRequest(SOURCE_DIR.resolve(source).toAbsolutePath().toString(), start, end));
        String response = TestUtil.getResponseString(result);
        return JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");
    }

    @AfterClass
    public void shutDownLanguageServer() {
        TestUtil.shutdownLanguageServer(this.serviceEndpoint);
        this.languageServer = null;
        this.serviceEndpoint = null;
    }

    public String[] skipList() {
        return new String[]{ };
    }

    private void updateConfig(Path configJsonPath, Object updatedConfig) throws IOException {
        String objStr = gson.toJson(updatedConfig).concat(System.lineSeparator());
        Files.writeString(configJsonPath, objStr);
    }

    private record TestConfig(String source, String description, LinePosition start, LinePosition end,
                              Diagram diagram) {

    }

    private record Diagram(LineRange location, JsonArray participants, JsonArray others) {

    }
}
