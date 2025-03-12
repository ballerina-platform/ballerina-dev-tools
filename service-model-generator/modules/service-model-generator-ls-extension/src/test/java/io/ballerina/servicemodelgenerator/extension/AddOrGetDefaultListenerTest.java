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

package io.ballerina.servicemodelgenerator.extension;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.request.FunctionSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerDiscoveryRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerSourceRequest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the service model source generator addFunction service.
 *
 * @since 2.0.0
 */
public class AddOrGetDefaultListenerTest extends AbstractLSTest {

    private static final Type TEXT_EDIT_LIST_TYPE = new TypeToken<Map<String, List<TextEdit>>>() {
    }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        ListenerDiscoveryRequest request = new ListenerDiscoveryRequest(sourceDir.resolve(testConfig.filePath())
                .toAbsolutePath().toString(), "ballerina", "http");

        JsonObject jsonMap = getResponse(request);

        // check if the jsonMap has textEdits
        if (jsonMap.has("textEdits")) {
            Map<String, List<TextEdit>> actualTextEdits = gson.fromJson(jsonMap, TEXT_EDIT_LIST_TYPE);

            boolean assertFailure = false;

            if (actualTextEdits.size() != testConfig.textEdits().size()) {
                log.info("The number of text edits does not match the expected output.");
                assertFailure = true;
            }

            Map<String, List<TextEdit>> newMap = new HashMap<>();
            for (Map.Entry<String, List<TextEdit>> entry : actualTextEdits.entrySet()) {
                Path fullPath = Paths.get(entry.getKey());
                String relativePath = sourceDir.relativize(fullPath).toString();

                List<TextEdit> textEdits = testConfig.textEdits().get(relativePath.replace("\\", "/"));
                if (textEdits == null) {
                    log.info("No text edits found for the file: " + relativePath);
                    assertFailure = true;
                } else if (!assertArray("text edits", entry.getValue(), textEdits)) {
                    assertFailure = true;
                }

                newMap.put(relativePath, entry.getValue());
            }

            if (assertFailure) {
                TestConfig updatedConfig =
                        new TestConfig(testConfig.filePath(), testConfig.description(), null,
                                newMap);
                updateConfig(configJsonPath, updatedConfig);
                Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
            }
        } else {
            String defaultListenerRef = getResponse(request).get("defaultListenerRef").getAsString();
            if (!defaultListenerRef.equals(testConfig.defaultListenerRef())) {
                TestConfig updatedConfig =
                        new TestConfig(testConfig.filePath(), testConfig.description(), defaultListenerRef,
                                null);
                updateConfig(configJsonPath, updatedConfig);
                Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
            }
        }

    }

    @Override
    protected String getResourceDir() {
        return "add_or_get_default_listener";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return AddOrGetDefaultListenerTest.class;
    }

    @Override
    protected String getServiceName() {
        return "serviceDesign";
    }
    
    @Override
    protected String getApiName() {
        return "addOrGetDefaultListener";
    }

    /**
     * Represents the test configuration for the source generator test.
     *
     * @param filePath    The path to the source file.
     * @param description The description of the test.
     * @param defaultListenerRef The default listener reference.
     * @param textEdits   The expected text edits.
     */
    private record TestConfig(String filePath, String description, String defaultListenerRef,
                              Map<String, List<TextEdit>> textEdits) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
