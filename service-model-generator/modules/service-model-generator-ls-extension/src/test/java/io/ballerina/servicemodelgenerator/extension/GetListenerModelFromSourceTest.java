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
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.request.CommonModelFromSourceRequest;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Assert the response returned by the getListenerFromSource.
 *
 * @since 2.0.0
 */
public class GetListenerModelFromSourceTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        BufferedReader bufferedReader = Files.newBufferedReader(configJsonPath);
        GetListenerModelFromSourceTest.TestConfig testConfig = gson.fromJson(bufferedReader,
                GetListenerModelFromSourceTest.TestConfig.class);
        bufferedReader.close();

        String sourcePath = sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString();
        Codedata codedata = new Codedata(LineRange.from(sourcePath, testConfig.start(), testConfig.end()));
        CommonModelFromSourceRequest sourceRequest = new CommonModelFromSourceRequest(sourcePath, codedata);
        JsonObject jsonMap = getResponseAndCloseFile(sourceRequest, sourcePath);
        if (!jsonMap.equals(testConfig.response())) {
            GetListenerModelFromSourceTest.TestConfig updatedConfig =
                    new GetListenerModelFromSourceTest.TestConfig(testConfig.filePath(), testConfig.description(),
                            testConfig.start(), testConfig.end(), jsonMap);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s) %s", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "listener_from_source";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetListenerModelFromSourceTest.class;
    }

    @Override
    protected String getServiceName() {
        return "serviceDesign";
    }

    @Override
    protected String getApiName() {
        return "getListenerFromSource";
    }

    /**
     * Represents the test configuration for the source generator test.
     *
     * @param filePath    The path to the source file
     * @param description The description of the test
     * @param start       The start position of the service declaration node
     * @param end         The end position of the service declaration node
     * @param response    The expected response
     */
    private record TestConfig(String filePath, String description, LinePosition start, LinePosition end,
                              JsonElement response) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
