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

package io.ballerina.servicemodelgenerator.extension;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.servicemodelgenerator.extension.request.FunctionModelRequest;
import io.ballerina.servicemodelgenerator.extension.response.FunctionModelResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Assert the response returned by the getFunctionModel.
 *
 * @since 2.0.0
 */
public class GetFunctionModelTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        BufferedReader bufferedReader = Files.newBufferedReader(configJsonPath);
        GetFunctionModelTest.TestConfig testConfig = gson.fromJson(bufferedReader,
                GetFunctionModelTest.TestConfig.class);
        bufferedReader.close();

        FunctionModelRequest request = new FunctionModelRequest(testConfig.type(), testConfig.functionName());
        JsonObject jsonMap = getResponse(request);
        FunctionModelResponse functionModelResponse = gson.fromJson(jsonMap, FunctionModelResponse.class);
        JsonElement actualResourceModel = gson.toJsonTree(functionModelResponse.function());

        if (!testConfig.response().equals(actualResourceModel)) {
            GetFunctionModelTest.TestConfig updatedConfig =
                    new GetFunctionModelTest.TestConfig(testConfig.description(), testConfig.type(),
                            testConfig.functionName(), actualResourceModel);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "get_function_model";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetFunctionModelTest.class;
    }

    @Override
    protected String getServiceName() {
        return "serviceDesign";
    }

    @Override
    protected String getApiName() {
        return "getFunctionModel";
    }

    /**
     * Represents the test configuration.
     *
     * @param functionName The name of the function
     * @param type         The type of the function
     * @param description  The description of the test
     * @param response     The expected response
     */
    private record TestConfig(String description, String type, String functionName,
                              JsonElement response) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
