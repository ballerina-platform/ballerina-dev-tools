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
import io.ballerina.flowmodelgenerator.extension.request.SuggestedComponentRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the package component service.
 *
 * @since 2.0.0
 */
public class SuggestedComponentTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String fileContent = Files.readString(sourceDir.resolve(testConfig.source()).toAbsolutePath());
        SuggestedComponentRequest request = new SuggestedComponentRequest(fileContent);
        JsonArray jsonModel = getResponse(request).getAsJsonArray("modules");

        JsonArray output = testConfig.output();
        if (!assertArray("packages", jsonModel.asList(), output.asList())) {
//            updateConfig(configJsonPath, new TestConfig(testConfig.source(), jsonModel));
            compareJsonElements(jsonModel, output);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.source(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "package_components";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return SuggestedComponentTest.class;
    }

    @Override
    protected String getApiName() {
        return "getSuggestedComponents";
    }

    /**
     * Represents the test configuration for the get suggested components test.
     *
     * @param source The source file
     * @param output The expected output
     * @since 2.0.0
     */
    private record TestConfig(String source, JsonArray output) {

    }
}
