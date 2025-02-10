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

package io.ballerina.testmanagerservice.extension;

import com.google.gson.JsonObject;
import io.ballerina.testmanagerservice.extension.request.TestsDiscoveryRequest;
import io.ballerina.testmanagerservice.extension.response.TestsDiscoveryResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Assert the response returned by the discoverInFile.
 *
 * @since 2.0.0
 */
public class TestFileTestDiscovery extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestFileTestDiscovery.TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath),
                TestFileTestDiscovery.TestConfig.class);

        String testSourcePath = sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString();
        TestsDiscoveryRequest request = new TestsDiscoveryRequest(testSourcePath);
        JsonObject jsonMap = getResponse(request);

        TestsDiscoveryResponse testsDiscoveryResponse = gson.fromJson(jsonMap, TestsDiscoveryResponse.class);
        boolean assertTrue = testsDiscoveryResponse.equals(testConfig.response());

        if (!assertTrue) {
            TestFileTestDiscovery.TestConfig updatedConfig =
                    new TestFileTestDiscovery.TestConfig(testConfig.filePath(), testConfig.description(),
                            testsDiscoveryResponse);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "discover_in_file";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return TestFileTestDiscovery.class;
    }

    @Override
    protected String getApiName() {
        return "discoverInFile";
    }

    /**
     * Represents the test configuration.
     *
     * @param filePath    The path of the file
     * @param description The description of the test
     * @param response    The expected response
     */
    private record TestConfig(String filePath, String description, TestsDiscoveryResponse response) {
        public String description() {
            return description == null ? "" : description;
        }
    }
}
