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

package io.ballerina.flowmodelgenerator.extension.agentsmanager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelNodeTemplateRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for get template of agent.
 *
 * @since 2.0.0
 */
public class NodeTemplateTest extends AbstractLSTest {

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("agent_template.json")},
                {Path.of("agent_call_template.json")},
                {Path.of("model_template.json")}
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        String filePath =
                testConfig.source() == null ? "" : sourceDir.resolve(testConfig.source()).toAbsolutePath().toString();
        FlowModelNodeTemplateRequest request =
                new FlowModelNodeTemplateRequest(filePath, testConfig.position(), testConfig.codedata());
        JsonElement nodeTemplate = getResponse(request).get("flowNode");

        if (!nodeTemplate.equals(testConfig.output())) {
            TestConfig updateConfig =
                    new TestConfig(testConfig.source(), testConfig.position(), testConfig.description(),
                            testConfig.codedata(), nodeTemplate);
            updateConfig(configJsonPath, updateConfig);
            compareJsonElements(nodeTemplate, testConfig.output());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "agents_manager";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return NodeTemplateTest.class;
    }

    @Override
    protected String getApiName() {
        return "getNodeTemplate";
    }

    /**
     * Represents the test configuration for the flow model getNodeTemplate API.
     *
     * @param source      The source file path
     * @param position    The position of the node to be added
     * @param description The description of the test
     * @param codedata    The codedata of the node
     * @param output      The expected output
     */
    private record TestConfig(String source, LinePosition position, String description, JsonObject codedata,
                              JsonElement output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
