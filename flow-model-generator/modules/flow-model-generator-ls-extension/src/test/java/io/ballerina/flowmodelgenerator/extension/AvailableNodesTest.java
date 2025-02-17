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

import com.google.gson.JsonArray;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelAvailableNodesRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test for the getting available nodes service.
 *
 * @since 2.0.0
 */
public class AvailableNodesTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        FlowModelAvailableNodesRequest request =
                new FlowModelAvailableNodesRequest(sourceDir.resolve(testConfig.source()).toAbsolutePath().toString(),
                        testConfig.position());
        JsonArray availableNodes = getResponse(request).getAsJsonArray("categories");

        JsonArray categories = availableNodes.getAsJsonArray();
        if (!categories.equals(testConfig.categories())) {
            TestConfig updateConfig =
                    new TestConfig(testConfig.description(), testConfig.position(), testConfig.source(), categories);
//            updateConfig(configJsonPath, updateConfig);
            compareJsonElements(categories, testConfig.categories());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String[] skipList() {
        //TODO: Need a better approach on how we can mock the central data
        return new String[]{
                "remote_connector.json"
        };
    }

    @Override
    protected String getResourceDir() {
        return "available_nodes";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return AvailableNodesTest.class;
    }

    @Override
    protected String getApiName() {
        return "getAvailableNodes";
    }

    /**
     * Represents the test configuration for the available nodes test.
     *
     * @param description The description of the test
     * @param position    The position of the node to be added
     * @param source      The source file path
     * @param categories  The available categories for the given input
     */
    private record TestConfig(String description, LinePosition position, String source, JsonArray categories) {

    }
}
