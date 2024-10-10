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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariablesGetRequest;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test cases for the flow model generator service.
 *
 * @since 1.4.0
 */
public class ConfigVariablesTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Endpoint endpoint = TestUtil.newLanguageServer().withLanguageServer(new BallerinaLanguageServer()).build();
        Path configJsonPath = configDir.resolve(config);
        ConfigVariablesTestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath),
                ConfigVariablesTestConfig.class);

        ConfigVariablesGetRequest request =
                new ConfigVariablesGetRequest(sourceDir.resolve(testConfig.configFile()).toAbsolutePath().toString());
        JsonObject configVariables = getResponse(endpoint, request);

        if (!configVariables.equals(testConfig.configVariables())) {
            ConfigVariablesTestConfig updatedConfig = new ConfigVariablesTestConfig(testConfig.configFile(),
                    configVariables);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s'", configJsonPath));
        }
        TestUtil.shutdownLanguageServer(endpoint);
    }

    @Override
    protected String getResourceDir() {
        return "configurable_variables";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ConfigVariablesTest.class;
    }

    @Override
    protected String getApiName() {
        return "getConfigVariables";
    }

    @Override
    protected String getServiceName() {
        return "configEditor";
    }

    /**
     * Represents the test configuration for the model generator test.
     *
     * @param configFile      Path to config file
     * @param configVariables Config variables
     * @since 1.4.0
     */
    private record ConfigVariablesTestConfig(String configFile, JsonElement configVariables) {

    }
}
