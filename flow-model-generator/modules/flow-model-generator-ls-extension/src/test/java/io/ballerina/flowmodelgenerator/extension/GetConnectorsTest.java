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
import io.ballerina.flowmodelgenerator.extension.request.FlowModelGetConnectorsRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Tests for getting the connectors.
 *
 * @since 1.4.0
 */
public class GetConnectorsTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = resDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        Map<String, String> queryMap = Map.of("q", testConfig.keyword());
        FlowModelGetConnectorsRequest request = new FlowModelGetConnectorsRequest(queryMap);
        JsonArray availableNodes = getResponse(request).getAsJsonArray("categories");

        JsonArray categories = availableNodes.getAsJsonArray();
        if (!categories.equals(testConfig.categories())) {
            TestConfig updateConfig = new TestConfig(testConfig.description(), testConfig.keyword(), categories);
//            updateConfig(config, updateConfig);
            compareJsonElements(categories, testConfig.categories());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "get_connectors";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetConnectorsTest.class;
    }

    @Override
    protected String getApiName() {
        return "getConnectors";
    }

    /**
     * Represent the test configurations for the get connectors test.
     *
     * @param description The description of the test
     * @param keyword     The keyword to search for connectors
     * @param categories  The categories of connectors
     */
    private record TestConfig(String description, String keyword, JsonArray categories) {

    }
}
