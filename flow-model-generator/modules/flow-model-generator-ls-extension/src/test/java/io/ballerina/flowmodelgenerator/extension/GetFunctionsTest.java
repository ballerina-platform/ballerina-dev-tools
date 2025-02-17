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
import io.ballerina.flowmodelgenerator.extension.request.FlowModelGetFunctionsRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Tests for getting the categories.
 *
 * @since 2.0.0
 */
public class GetFunctionsTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        FlowModelGetFunctionsRequest request =
                new FlowModelGetFunctionsRequest(getSourcePath(testConfig.source()), testConfig.position(),
                        testConfig.queryMap());
        JsonArray availableNodes = getResponse(request).getAsJsonArray("categories");

        JsonArray categories = availableNodes.getAsJsonArray();
        if (!categories.equals(testConfig.categories())) {
            TestConfig updateConfig =
                    new TestConfig(testConfig.description(), testConfig.source(), testConfig.position(),
                            testConfig.queryMap(), categories);
//            updateConfig(configJsonPath, updateConfig);
            compareJsonElements(categories, testConfig.categories());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "get_functions";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetFunctionsTest.class;
    }

    @Override
    protected String getApiName() {
        return "getFunctions";
    }

    /**
     * Represent the test configurations for the get categories test.
     *
     * @param description The description of the test
     * @param source      The source file path
     * @param position    The position in the source file
     * @param queryMap    The query parameters to filter the nodes
     * @param categories  The categories
     */
    private record TestConfig(String description, String source, LineRange position, Map<String, String> queryMap,
                              JsonArray categories) {

    }
}
