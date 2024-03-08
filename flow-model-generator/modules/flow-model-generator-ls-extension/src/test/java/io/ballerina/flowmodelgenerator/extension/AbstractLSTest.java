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
 * Represents the abstract test class for the flow model generator service.
 *
 * @since 2201.9.0
 */
abstract class AbstractLSTest {

    protected static Logger LOG;
    protected static Path RES_DIR, SOURCE_DIR, CONFIG_DIR;
    protected final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private Endpoint serviceEndpoint;
    private BallerinaLanguageServer languageServer;

    @BeforeClass
    public final void init() {
        RES_DIR = Paths.get("src/test/resources").resolve(getResourceDir()).toAbsolutePath();
        CONFIG_DIR = RES_DIR.resolve("config");
        SOURCE_DIR = RES_DIR.resolve("source");
        LOG = LoggerFactory.getLogger(clazz());
        this.languageServer = new BallerinaLanguageServer();
        TestUtil.LanguageServerBuilder builder = TestUtil.newLanguageServer().withLanguageServer(languageServer);
        this.serviceEndpoint = builder.build();
    }

    /**
     * Positive tests for the flow model generator service.
     *
     * @param config The path to the test config
     * @throws IOException If an error occurs while reading the config
     */
    @Test(dataProvider = "data-provider")
    public abstract void test(Path config) throws IOException;

    /**
     * Provides the list of test configs.
     *
     * @return The list of test configs
     */
    @DataProvider(name = "data-provider")
    protected Object[] getConfigsList() {
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

    /**
     * Provides the list of tests to be skipped.
     *
     * @return The list of tests to be skipped
     */
    protected String[] skipList() {
        return new String[]{};
    }

    /**
     * Updates the test config with the result generated from the test case.
     *
     * @param configJsonPath The path to the test config
     * @param updatedConfig  The updated config
     * @throws IOException If an error occurs while writing the config
     */
    protected void updateConfig(Path configJsonPath, Object updatedConfig) throws IOException {
        String objStr = gson.toJson(updatedConfig).concat(System.lineSeparator());
        Files.writeString(configJsonPath, objStr);
    }

    protected JsonObject getResponse(Object request) throws IOException {
        CompletableFuture<?> result = this.serviceEndpoint.request("flowDesignService/" + getApiName(), request);
        String response = TestUtil.getResponseString(result);
        return JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");
    }

    /**
     * Asserts the equality of the actual and expected arrays.
     *
     * @param property      The property name
     * @param actualNodes   The actual nodes
     * @param expectedNodes The expected nodes
     * @return True if the arrays are equal, false otherwise
     */
    protected final boolean assertArray(String property, List<?> actualNodes, List<?> expectedNodes) {
        List<Object> unmatchedExpectedNodes = new java.util.ArrayList<>(expectedNodes);
        List<Object> mismatchedAvailableNodes = new java.util.ArrayList<>();

        int actualTextEditsSize = actualNodes.size();
        int expectedTextEditsSize = expectedNodes.size();
        boolean hasCountMatch = actualTextEditsSize == expectedTextEditsSize;
        if (!hasCountMatch) {
            LOG.error(String.format("Mismatched %s count. Expected: %d, Found: %d", property, expectedTextEditsSize,
                    actualTextEditsSize));
        }

        for (Object actualNode : actualNodes) {
            if (expectedNodes.contains(actualNode)) {
                unmatchedExpectedNodes.remove(actualNode);
            } else {
                mismatchedAvailableNodes.add(actualNode);
            }
        }

        boolean hasAllExpectedTextEdits = unmatchedExpectedNodes.isEmpty();
        if (!hasAllExpectedTextEdits) {
            LOG.error(String.format("Found in expected %s but not in actual %s: ", property, property) +
                    unmatchedExpectedNodes);
        }

        boolean hasRelevantTextEdits = mismatchedAvailableNodes.isEmpty();
        if (!hasRelevantTextEdits) {
            LOG.error(String.format("Found in actual %s but not in expected %s: ", property, property) +
                    mismatchedAvailableNodes);
        }

        return hasCountMatch && hasAllExpectedTextEdits && hasRelevantTextEdits;
    }

    /**
     * Returns the resource directory of the API test.
     *
     * @return The resource directory of the API test
     */
    protected abstract String getResourceDir();

    /**
     * Returns the class of the API test.
     *
     * @return The class of the API test
     */
    protected abstract Class<? extends AbstractLSTest> clazz();

    /**
     * Returns the name of the API.
     *
     * @return The name of the API
     */
    protected abstract String getApiName();

    @AfterClass
    public void shutDownLanguageServer() {
        TestUtil.shutdownLanguageServer(this.serviceEndpoint);
        this.languageServer = null;
        this.serviceEndpoint = null;
    }
}
