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
import io.ballerina.flowmodelgenerator.extension.request.FunctionDefinitionRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test cases for the function definition request.
 *
 * @since 2.0.0
 */
public class FunctionDefinitionTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        FunctionDefinitionRequest request = new FunctionDefinitionRequest(
                getSourcePath(testConfig.filePath()),
                testConfig.fileName(),
                testConfig.functionName());
        JsonObject functionDefinition = getResponseAndCloseFile(request, testConfig.filePath())
                .getAsJsonObject("functionDefinition");

        if (!functionDefinition.equals(testConfig.output())) {
            TestConfig updatedConfig = new TestConfig(
                    testConfig.filePath(),
                    testConfig.fileName(),
                    testConfig.functionName(),
                    testConfig.description(),
                    functionDefinition);
//            updateConfig(configJsonPath, updatedConfig);
            compareJsonElements(functionDefinition, testConfig.output());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "function_definition";
    }


    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return FunctionDefinitionTest.class;
    }

    @Override
    protected String getApiName() {
        return "functionDefinition";
    }

    private record TestConfig(String filePath, String fileName, String functionName, String description,
                              JsonObject output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
