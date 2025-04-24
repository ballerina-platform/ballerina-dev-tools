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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceDesignerDiagnosticRequest;
import io.ballerina.servicemodelgenerator.extension.response.ServiceDesignerDiagnosticResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Assert the response returned by the getFunctionModel.
 *
 * @since 2.0.0
 */
public class ServiceDesignerDiagnosticsTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        BufferedReader bufferedReader = Files.newBufferedReader(configJsonPath);
        ServiceDesignerDiagnosticsTest.TestConfig testConfig = gson.fromJson(bufferedReader,
                ServiceDesignerDiagnosticsTest.TestConfig.class);
        bufferedReader.close();

        ServiceDesignerDiagnosticRequest request = new ServiceDesignerDiagnosticRequest(composeRequest(testConfig),
                testConfig.operation());
        JsonObject jsonMap = getResponse(request);
        ServiceDesignerDiagnosticResponse response = gson.fromJson(jsonMap, ServiceDesignerDiagnosticResponse.class);
        JsonElement actualResponse = gson.toJsonTree(response.response());

        if (!testConfig.response().equals(actualResponse)) {
            ServiceDesignerDiagnosticsTest.TestConfig updatedConfig = new TestConfig(
                    testConfig.filePath(), testConfig.description(), testConfig.operation(),
                    testConfig.requestModel(), testConfig.codedata(), actualResponse);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "diagnostics";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ServiceDesignerDiagnosticsTest.class;
    }

    @Override
    protected String getServiceName() {
        return "serviceDesign";
    }

    @Override
    protected String getApiName() {
        return "diagnostics";
    }

    private JsonElement composeRequest(TestConfig config) {
        switch (config.operation()) {
            case "addFunction", "addResource" -> {
                return composeFunctionSourceRequest(config);
            }
            case "updateFunction" -> {
                return composeFunctionModifierRequest(config);
            }
            default -> {
                return new JsonObject();
            }

        }
    }

    private JsonElement composeFunctionModifierRequest(TestConfig testConfig) {
        JsonObject jsonMap = new JsonObject();
        Path filePath = sourceDir.resolve(testConfig.filePath());
        jsonMap.addProperty("filePath", filePath.toAbsolutePath().toString());
        jsonMap.add("function", testConfig.requestModel());
        return jsonMap;
    }

    private JsonElement composeFunctionSourceRequest(TestConfig config) {
        JsonObject jsonMap = new JsonObject();
        Path filePath = sourceDir.resolve(config.filePath());
        jsonMap.addProperty("filePath", filePath.toAbsolutePath().toString());
        jsonMap.add("function", config.requestModel());
        jsonMap.add("codedata", config.codedata());
        return jsonMap;
    }

    /**
     * Represents the test configuration.
     *
     * @param filePath     The path to the test configuration file.
     * @param description  The description of the test.
     * @param operation   The operation to be performed.
     * @param requestModel The request model for the test.
     * @param codedata     The codedata for the test.
     * @param response     The expected response for the test.
     */
    private record TestConfig(String filePath, String description, String operation, JsonElement requestModel,
                              JsonElement codedata, JsonElement response) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
