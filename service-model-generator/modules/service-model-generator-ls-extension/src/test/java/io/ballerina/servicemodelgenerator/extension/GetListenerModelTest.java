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
import io.ballerina.servicemodelgenerator.extension.request.ListenerModelRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Assert the response returned by the getListenerModel.
 *
 * @since 2.0.0
 */
public class GetListenerModelTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        BufferedReader bufferedReader = Files.newBufferedReader(configJsonPath);
        GetListenerModelTest.TestConfig testConfig = gson.fromJson(bufferedReader,
                GetListenerModelTest.TestConfig.class);
        bufferedReader.close();

        ListenerModelRequest request = new ListenerModelRequest(testConfig.orgName(), testConfig.pkgName(),
                testConfig.moduleName());
        JsonObject jsonMap = getResponse(request);

        boolean assertTrue = testConfig.response().getAsJsonObject().equals(jsonMap);
        if (!assertTrue) {
            GetListenerModelTest.TestConfig updatedConfig =
                    new GetListenerModelTest.TestConfig(testConfig.description(), testConfig.orgName(),
                            testConfig.pkgName(), testConfig.moduleName(), jsonMap);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }


    @Override
    protected String getResourceDir() {
        return "get_listener_model";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetListenerModelTest.class;
    }

    @Override
    protected String getServiceName() {
        return "serviceDesign";
    }

    @Override
    protected String getApiName() {
        return "getListenerModel";
    }

    /**
     * Represents the test configuration.
     *
     * @param description description of the test
     * @param orgName   organization name
     * @param pkgName   package name
     * @param moduleName module name
     * @param response  expected response
     * @since 2.0.0
     */
    private record TestConfig(String description,  String orgName, String pkgName, String moduleName,
                              JsonElement response) {
        public String description() {
            return description == null ? "" : description;
        }
    }
}
