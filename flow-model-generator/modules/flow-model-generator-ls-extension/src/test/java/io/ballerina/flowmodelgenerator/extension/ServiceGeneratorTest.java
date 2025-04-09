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

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.extension.request.OpenAPIServiceGenerationRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test cases for the OpenAPI service generation.
 *
 * @since 1.4.0
 */
public class ServiceGeneratorTest extends AbstractLSTest {

    private static final Type textEditListType = new TypeToken<Map<String, List<TextEdit>>>() {
    }.getType();

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
//                {Path.of("config1.json")},
//                {Path.of("config2.json")},
//                {Path.of("config3.json")},
//                {Path.of("config4.json")},
                {Path.of("config5.json")}
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        Path contractPath = resDir.resolve("contracts").resolve(testConfig.contractFile());

        Path project = configDir.resolve(config.getFileName().toString().split(".json")[0]);
        Files.createDirectories(project);
        Files.createFile(project.resolve("main.bal"));
        String projectPath = project.toAbsolutePath().toString();
        OpenAPIServiceGenerationRequest request =
                new OpenAPIServiceGenerationRequest(contractPath.toAbsolutePath().toString(), projectPath,
                        testConfig.name(), testConfig.listeners());

        boolean assertFailure = false;
        Map<String, List<TextEdit>> newMap = new HashMap<>();
        try {
            JsonObject jsonMap = getResponse(request).getAsJsonObject("textEdits");
            Map<String, List<TextEdit>> actualTextEdits = gson.fromJson(jsonMap, textEditListType);

            if (actualTextEdits.size() != testConfig.textEdits().size()) {
                log.info("The number of text edits does not match the expected output.");
                assertFailure = true;
            }

            for (Map.Entry<String, List<TextEdit>> entry : actualTextEdits.entrySet()) {
                Path fullPath = Paths.get(entry.getKey());
                String relativePath = configDir.relativize(fullPath).toString();

                List<TextEdit> textEdits = testConfig.textEdits().get(relativePath.replace("\\", "/"));
                if (textEdits == null) {
                    log.info("No text edits found for the file: " + relativePath);
                    assertFailure = true;
                } else if (!assertArray("text edits", entry.getValue(), textEdits)) {
                    assertFailure = true;
                }

                newMap.put(relativePath, entry.getValue());
            }
        } catch (Throwable e) {
            assertFailure = !testConfig.textEdits().isEmpty();
        }

        deleteFolder(project.toFile());

        if (assertFailure) {
            TestConfig updatedConfig = new TestConfig(testConfig.contractFile(), newMap, testConfig.name(),
                    testConfig.listeners());
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s'", configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "openapi_service_gen";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ServiceGeneratorTest.class;
    }

    @Override
    protected String getApiName() {
        return "generateServiceFromOpenApiContract";
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
    }

    /**
     * Represents the test configuration for the service generation.
     *
     * @param contractFile OpenAPI contract file
     * @param textEdits    line range of service declaration
     * @param name         Service type name
     * @param listeners    Listener names
     * @since 1.4.0
     */
    private record TestConfig(String contractFile, Map<String, List<TextEdit>> textEdits, String name,
                              List<String> listeners) {

    }
}
