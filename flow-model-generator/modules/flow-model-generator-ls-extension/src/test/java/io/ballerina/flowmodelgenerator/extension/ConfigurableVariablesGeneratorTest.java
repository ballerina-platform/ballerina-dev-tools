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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.ballerina.flowmodelgenerator.extension.request.ConfigurableVariablesGeneratorRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelGeneratorRequest;
import io.ballerina.tools.text.LinePosition;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Test cases for the flow model generator service.
 *
 * @since 1.4.0
 */
public class ConfigurableVariablesGeneratorTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Endpoint endpoint = TestUtil.newLanguageServer().withLanguageServer(new BallerinaLanguageServer()).build();
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        ConfigurableVariablesGeneratorRequest request =
                new ConfigurableVariablesGeneratorRequest(testConfig.projectPath(), testConfig.variable(),
                        testConfig.type(), testConfig.value());
//        FlowModelGeneratorRequest request = new FlowModelGeneratorRequest(
//                sourceDir.resolve(testConfig.source()).toAbsolutePath().toString(), testConfig.start(),
//                testConfig.end());
        JsonObject configurableVariablesEdits =
                getResponse(endpoint, request);

        if (!configurableVariablesEdits.getAsJsonObject("textEdits").equals(testConfig.textEdits())) {
            TestConfig updatedConfig = new TestConfig(testConfig.projectPath(), testConfig.variable(),
                    testConfig.type(), testConfig.value(), configurableVariablesEdits);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s'", configJsonPath));
        }
        TestUtil.shutdownLanguageServer(endpoint);
    }

    @Override
    protected String getResourceDir() {
        return "generateConfigurableVariables";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ConfigurableVariablesGeneratorTest.class;
    }

    @Override
    protected String getApiName() {
        return "getFlowModel";
    }

    /**
     * Represents the test configuration for the model generator test.
     *
     * @param projectPath Path to project
     * @param variable    Configurable variable name
     * @param type        Configurable variable type
     * @param value       Configurable variable value
     *
     * @since 1.4.0
     */
    private record TestConfig(String projectPath, String variable, String type, String value, JsonObject textEdits) {

    }
}
