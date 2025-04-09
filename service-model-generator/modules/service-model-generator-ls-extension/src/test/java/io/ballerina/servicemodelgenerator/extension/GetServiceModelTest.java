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
import io.ballerina.servicemodelgenerator.extension.request.ServiceModelRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Assert the response returned by the getListenerModel.
 *
 * @since 2.3.0
 */
public class GetServiceModelTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        BufferedReader bufferedReader = Files.newBufferedReader(configJsonPath);
        GetServiceModelTest.TestConfig testConfig = gson.fromJson(bufferedReader, GetServiceModelTest.TestConfig.class);
        bufferedReader.close();

        String filePath = sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString();
        ServiceModelRequest request = new ServiceModelRequest(filePath, testConfig.orgName(), testConfig.pkgName(),
                testConfig.moduleName());
        JsonObject jsonMap = getResponse(request);

        boolean assertTrue = testConfig.response().getAsJsonObject().equals(jsonMap);
        if (!assertTrue) {
            GetServiceModelTest.TestConfig updatedConfig = new GetServiceModelTest.TestConfig(testConfig.description(),
                    testConfig.filePath(), testConfig.orgName(), testConfig.pkgName(), testConfig.moduleName(),
                    jsonMap);
//            updateConfig(configJsonPath, updatedConfig);
            compareJsonElements(jsonMap, testConfig.response());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }


    @Override
    protected String getResourceDir() {
        return "get_service_model";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetServiceModelTest.class;
    }

    @Override
    protected String getServiceName() {
        return "serviceDesign";
    }

    @Override
    protected String getApiName() {
        return "getServiceModel";
    }

    /**
     * Represents the test configuration.
     *
     * @param description description of the test
     * @param filePath   file path
     * @param orgName   organization name
     * @param pkgName   package name
     * @param moduleName module name
     * @param response  expected response
     * @since 2.3.0
     */
    private record TestConfig(String description,  String filePath, String orgName, String pkgName, String moduleName,
                              JsonElement response) {
        public String description() {
            return description == null ? "" : description;
        }
    }
}
