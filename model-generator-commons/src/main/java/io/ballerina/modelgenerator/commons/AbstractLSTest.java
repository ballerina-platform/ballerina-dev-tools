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

package io.ballerina.modelgenerator.commons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Represents the abstract test class for the flow model generator service.
 *
 * @since 1.4.0
 */
public abstract class AbstractLSTest {

    protected static Logger log;
    protected static Path resDir, sourceDir, configDir;
    protected final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    protected Endpoint serviceEndpoint;
    private BallerinaLanguageServer languageServer;
    protected static final List<String> UNDEFINED_DIAGNOSTICS_CODES = List.of("BCE2000", "BCE2011");

    @BeforeClass
    public final void init() {
        resDir = Paths.get("src/test/resources").resolve(getResourceDir()).toAbsolutePath();
        configDir = resDir.resolve("config");
        sourceDir = resDir.resolve("source");
        log = LoggerFactory.getLogger(clazz());
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
        try (Stream<Path> stream = Files.walk(resDir)) {
            return stream
                    .filter(path -> {
                        File file = path.toFile();
                        return file.isFile() && !file.getName().startsWith(".")
                                && file.getName().endsWith(".json")
                                && !skippedTests.contains(file.getName());
                    })
                    .toArray(Path[]::new);
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
        return new String[]{ };
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

    protected JsonObject getResponse(Endpoint endpoint, Object request) throws IOException {
        Endpoint tempEndPoint = serviceEndpoint;
        serviceEndpoint = endpoint;
        JsonObject response = getResponse(request);
        serviceEndpoint = tempEndPoint;
        return response;
    }

    protected JsonObject getResponse(Object request) throws IOException {
        return getResponse(request, getServiceName() + "/" + getApiName());
    }

    protected JsonObject getResponseAndCloseFile(Object request, String source) throws IOException {
        JsonObject response = getResponse(request);
        String fileUri = sourceDir.resolve(source).toAbsolutePath().toUri().toString();
        serviceEndpoint.notify("textDocument/didClose",
                new DidCloseTextDocumentParams(new TextDocumentIdentifier(fileUri)));
        return response;
    }

    protected JsonObject getResponse(Object request, String api) {
        CompletableFuture<?> result = serviceEndpoint.request(api, request);
        String response = TestUtil.getResponseString(result);
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");
        JsonPrimitive errorMsg = jsonObject.getAsJsonPrimitive("errorMsg");
        if (errorMsg != null) {
            log.error("Stacktrace: {}", jsonObject.getAsJsonPrimitive("stacktrace").getAsString());
            Assert.fail("Error occurred: " + errorMsg.getAsString());
        }
        return jsonObject;
    }

    protected void sendNotification(String api, Object request) {
        serviceEndpoint.notify(api, request);
    }

    protected void notifyDidOpen(String sourcePath) throws IOException {
        TextDocumentItem textDocumentItem = new TextDocumentItem();
        String text;
        try (FileInputStream fis = new FileInputStream(sourcePath)) {
            text = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
        }
        textDocumentItem.setUri(CommonUtils.getExprUri(sourcePath));
        textDocumentItem.setText(text);
        textDocumentItem.setLanguageId("ballerina");
        textDocumentItem.setVersion(1);
        sendNotification("textDocument/didOpen", new DidOpenTextDocumentParams(textDocumentItem));
    }

    protected void notifyDidClose(String sourcePath) {
        TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier();
        textDocumentIdentifier.setUri(CommonUtils.getExprUri(sourcePath));
        sendNotification("textDocument/didClose", new DidCloseTextDocumentParams(textDocumentIdentifier));
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
            log.error(String.format("Mismatched %s count. Expected: %d, Found: %d", property, expectedTextEditsSize,
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
            log.error(String.format("Found in expected %s but not in actual %s: ", property, property) +
                    unmatchedExpectedNodes);
        }

        boolean hasRelevantTextEdits = mismatchedAvailableNodes.isEmpty();
        if (!hasRelevantTextEdits) {
            log.error(String.format("Found in actual %s but not in expected %s: ", property, property) +
                    mismatchedAvailableNodes);
        }

        return hasCountMatch && hasAllExpectedTextEdits && hasRelevantTextEdits;
    }

    /**
     * Compare the actual JSON with the expected JSON.
     *
     * @param actualJson   the actual JSON produced by the LS extension
     * @param expectedJson the expected JSON
     */
    protected void compareJsonElements(JsonElement actualJson, JsonElement expectedJson) {
        log.info("Differences in JSON elements:");
        compareJsonElementsRecursive(actualJson, expectedJson, "");
    }

    private void compareJsonElementsRecursive(JsonElement actualJson, JsonElement expectedJson, String path) {
        if (actualJson.isJsonObject() && expectedJson.isJsonObject()) {
            compareJsonObjects(actualJson.getAsJsonObject(), expectedJson.getAsJsonObject(), path);
        } else if (actualJson.isJsonArray() && expectedJson.isJsonArray()) {
            compareJsonArrays(actualJson.getAsJsonArray(), expectedJson.getAsJsonArray(), path);
        } else if (!actualJson.equals(expectedJson)) {
            log.info("- Value mismatch at '" + path + "'\n  actual: " + actualJson + "\n  expected: " + expectedJson);
        }
    }

    private void compareJsonObjects(JsonObject actualJson, JsonObject expectedJson, String path) {
        Set<Map.Entry<String, JsonElement>> entrySet1 = actualJson.entrySet();
        Set<Map.Entry<String, JsonElement>> entrySet2 = expectedJson.entrySet();

        for (Map.Entry<String, JsonElement> entry : entrySet1) {
            String key = entry.getKey();
            String currentPath = path.isEmpty() ? key : path + "." + key;

            if (!expectedJson.has(key)) {
                log.info("- Key '" + currentPath + "' is missing in the expected JSON");
            } else {
                compareJsonElementsRecursive(entry.getValue(), expectedJson.get(key), currentPath);
            }
        }

        for (Map.Entry<String, JsonElement> entry : entrySet2) {
            String key = entry.getKey();
            String currentPath = path.isEmpty() ? key : path + "." + key;

            if (!actualJson.has(key)) {
                log.info("- Key '" + currentPath + "' is missing in the actual JSON");
            }
        }
    }

    private void compareJsonArrays(JsonArray actualArray, JsonArray expectedArray, String path) {
        int size1 = actualArray.size();
        int size2 = expectedArray.size();
        int minSize = Math.min(size1, size2);

        for (int i = 0; i < minSize; i++) {
            compareJsonElementsRecursive(actualArray.get(i), expectedArray.get(i), path + "[" + i + "]");
        }

        if (size1 > size2) {
            for (int i = size2; i < size1; i++) {
                log.info("- Extra element in actual JSON at '" + path + "[" + i + "]': " + actualArray.get(i));
            }
        } else if (size2 > size1) {
            for (int i = size1; i < size2; i++) {
                log.info("- Extra element in expected JSON at '" + path + "[" + i + "]': " + expectedArray.get(i));
            }
        }
    }

    protected String getSourcePath(String source) {
        return sourceDir.resolve(source).toAbsolutePath().toString();
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

    protected String getServiceName() {
        return "flowDesignService";
    }

    @AfterClass
    public void shutDownLanguageServer() {
        TestUtil.shutdownLanguageServer(this.serviceEndpoint);
        this.languageServer = null;
        this.serviceEndpoint = null;
    }
}
