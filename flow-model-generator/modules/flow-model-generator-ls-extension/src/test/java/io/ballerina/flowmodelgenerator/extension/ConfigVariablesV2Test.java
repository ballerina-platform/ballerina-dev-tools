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

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariablesGetRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test class for configurable variables V2 API.
 *
 * @since 2.7.0
 */
public class ConfigVariablesV2Test extends AbstractLSTest {

    private static final Type flowNodes = new TypeToken<Map<String, List<FlowNode>>>() {
    }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        ConfigVariablesTestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath),
                ConfigVariablesTestConfig.class);

        ConfigVariablesGetRequest request =
                new ConfigVariablesGetRequest(sourceDir.resolve(testConfig.project()).toAbsolutePath().toString());
        JsonObject configVariables = getResponse(request);

        Map<String, List<FlowNode>> m = gson.fromJson(configVariables, flowNodes);
        List<FlowNode> actualFlowNodes = m.get("configVariables");
        boolean assertFalse = false;
        for (FlowNode actualFlowNode : actualFlowNodes) {
            Optional<Property> actualVar = actualFlowNode.getProperty(Property.VARIABLE_KEY);
            if (actualVar.isEmpty()) {
                assertFalse = true;
                break;
            }
            String actualVarName = actualVar.get().toSourceCode();
            for (FlowNode expectedFlowNode : testConfig.configVariables()) {
                Optional<Property> expectedVar = expectedFlowNode.getProperty(Property.VARIABLE_KEY);
                if (expectedVar.isEmpty()) {
                    assertFalse = true;
                    break;
                }
                String expectedVarName = expectedVar.get().toSourceCode();
                if (expectedVarName.equals(actualVarName)) {
                    if (!actualFlowNode.equals(expectedFlowNode)) {
                        assertFalse = true;
                    }
                }
            }
        }

        if (assertFalse) {
            ConfigVariablesTestConfig updatedConfig = new ConfigVariablesTestConfig(testConfig.project(),
                    actualFlowNodes);
            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s'", configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "configurable_variables_v2";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ConfigVariablesV2Test.class;
    }

    @Override
    protected String getApiName() {
        return "getConfigVariables";
    }

    @Override
    protected String getServiceName() {
        return "configEditorV2";
    }

    /**
     * Represents the test configuration for the model generator test.
     *
     * @param project         Path to config file
     * @param configVariables Config variables
     * @since 1.4.0
     */
    private record ConfigVariablesTestConfig(String project, List<FlowNode> configVariables) {

    }
}
