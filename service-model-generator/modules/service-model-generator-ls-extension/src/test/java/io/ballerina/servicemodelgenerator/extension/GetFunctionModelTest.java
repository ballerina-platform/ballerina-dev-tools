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

import com.google.gson.JsonObject;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.request.FunctionModelRequest;
import io.ballerina.servicemodelgenerator.extension.response.FunctionModelResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

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
        GetFunctionModelTest.TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath),
                GetFunctionModelTest.TestConfig.class);

        FunctionModelRequest request = new FunctionModelRequest(testConfig.type(), testConfig.functionName());
        JsonObject jsonMap = getResponse(request);

        FunctionModelResponse functionModelResponse = gson.fromJson(jsonMap, FunctionModelResponse.class);
        Function actualResourceModel = functionModelResponse.function();
        boolean assertTrue = isAssertTrue(testConfig, actualResourceModel);

        if (!assertTrue) {
            GetFunctionModelTest.TestConfig updatedConfig =
                    new GetFunctionModelTest.TestConfig(testConfig.description(), testConfig.type(),
                            testConfig.functionName(), functionModelResponse);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    private static boolean isAssertTrue(TestConfig testConfig, Function actual) {
        Function expectedResponse = testConfig.response().function();
        return expectedResponse.getMetadata().equals(actual.getMetadata()) &&
                expectedResponse.getKind().equals(actual.getKind()) &&
                expectedResponse.getAccessor().equals(actual.getAccessor()) &&
                expectedResponse.getReturnType().equals(actual.getReturnType()) &&
                expectedResponse.getParameters().size() == actual.getParameters().size() &&
                expectedResponse.getName().equals(actual.getName()) &&
                expectedResponse.getSchema().keySet().size() == actual.getSchema().keySet().size() &&
                expectedResponse.isEnabled() == actual.isEnabled() &&
                expectedResponse.isOptional() == actual.isOptional() &&
                expectedResponse.isEditable() == actual.isEditable();
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
                              FunctionModelResponse response) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
