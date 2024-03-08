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
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelAvailableNodesRequest;
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Test for the getting available nodes service.
 *
 * @since 2201.9.0
 */
public class AvailableNodesTest extends AbstractLSTest {

    private static final Type availableNodesType = new TypeToken<List<String>>() {
    }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = RES_DIR.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        FlowModelAvailableNodesRequest request = new FlowModelAvailableNodesRequest(testConfig.parentNodeLineRange(),
                testConfig.parentNodeKind(), testConfig.branchLabel());
        JsonArray availableNodes = getResponse(request).getAsJsonArray("availableNodes");

        List<String> actualAvailableNodes = gson.fromJson(availableNodes, availableNodesType);
        if (!assertArray("available nodes", actualAvailableNodes, testConfig.availableNodes())) {
            TestConfig updateConfig = new TestConfig(testConfig.description(), testConfig.parentNodeLineRange(),
                    testConfig.parentNodeKind(), testConfig.branchLabel(), actualAvailableNodes);
//            updateConfig(config, updateConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }

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
     * @param description         The description of the test
     * @param parentNodeLineRange The line range of the parent node
     * @param parentNodeKind      The kind of the parent node
     * @param branchLabel         The branch label
     * @param availableNodes      The available nodes for the given input
     */
    private record TestConfig(String description, LineRange parentNodeLineRange, String parentNodeKind,
                              String branchLabel, List<String> availableNodes) {

    }
}
