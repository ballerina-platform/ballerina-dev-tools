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
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.request.VisibleVariableTypeRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the visible variable types service.
 *
 * @since 1.4.0
 */
public class VisibleVariableTypesTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        VisibleVariableTypeRequest request = new VisibleVariableTypeRequest(
                sourceDir.resolve(testConfig.source()).toAbsolutePath().toString(), testConfig.position());
        JsonObject response = getResponse(request);

        JsonArray actualVisibleTypes = response.get("categories").getAsJsonArray();
        if (!actualVisibleTypes.equals(testConfig.visibleTypes())) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.source(),
                    testConfig.position(), actualVisibleTypes);
            compareJsonElements(actualVisibleTypes, testConfig.visibleTypes());
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "visible_variable_types";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return VisibleVariableTypesTest.class;
    }

    @Override
    protected String getApiName() {
        return "visibleVariableTypes";
    }

    @Override
    protected String getServiceName() {
        return "expressionEditor";
    }

    private record TestConfig(String description, String source, LinePosition position, JsonArray visibleTypes) {
    }
}
