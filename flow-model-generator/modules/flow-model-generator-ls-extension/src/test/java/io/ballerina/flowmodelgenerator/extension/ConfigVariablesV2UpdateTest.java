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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariableUpdateRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Test class for 'updateConfigVariable()' API in config API V2.
 *
 * @since 2.0.0
 */
public class ConfigVariablesV2UpdateTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        ConfigVariablesTestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath),
                ConfigVariablesTestConfig.class);

        String projectPath = sourceDir.resolve(testConfig.project()).toAbsolutePath().toString();

        ConfigVariableUpdateRequest request = new ConfigVariableUpdateRequest(
                testConfig.request().packageName(),
                testConfig.request().moduleName(),
                testConfig.request().configFilePath(),
                testConfig.request().configVariable()
        );
        ConfigVariableUpdateResponse actualResponse = gson.fromJson(getResponse(request), ConfigVariableUpdateResponse.class);

        if (!actualResponse.configVariables().equals(testConfig.response())) {
//            updateConfig(configJsonPath, new ConfigVariablesTestConfig(testConfig.project(),
//                    actualResponse.configVariables()));
            Assert.fail(String.format("Failed test: '%s'", configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "configurable_variables_v2_update";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ConfigVariablesV2UpdateTest.class;
    }

    @Override
    protected String getApiName() {
        return "updateConfigVariable";
    }

    @Override
    protected String getServiceName() {
        return "configEditorV2";
    }

    /**
     * Represents the response of the `updateConfigVariable()` API.
     */
    private record ConfigVariableUpdateResponse(Map<String, Map<String, List<FlowNode>>> configVariables) {

    }

    /**
     * Represents the test configuration for the model generator test.
     */
    private record ConfigVariablesTestConfig(String project, Request request, Response response) {

    }

    private record Request(String packageName, String moduleName, String configFilePath, JsonElement configVariable) {

    }

    private record Response(TextEdit[] textEdits) {

    }
}
