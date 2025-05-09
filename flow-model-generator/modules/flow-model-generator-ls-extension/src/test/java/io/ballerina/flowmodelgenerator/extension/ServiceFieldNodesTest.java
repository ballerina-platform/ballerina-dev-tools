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
import com.google.gson.JsonPrimitive;
import io.ballerina.flowmodelgenerator.extension.request.ServiceFieldNodesRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test cases for the service field flow node retrieving.
 *
 * @since 2.0.0
 */
public class ServiceFieldNodesTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        String filePath =
                testConfig.source() == null ? "" : sourceDir.resolve(testConfig.source()).toAbsolutePath().toString();
        ServiceFieldNodesRequest request = new ServiceFieldNodesRequest(testConfig.linePosition(), filePath);
        JsonObject response = getResponse(request).get("flowModel").getAsJsonObject();

        JsonObject jsonModel = getResponse(request).getAsJsonObject("flowModel");
        String balFileName = Path.of(jsonModel.getAsJsonPrimitive("fileName").getAsString()).getFileName().toString();
        JsonPrimitive testFileName = testConfig.flowModel().getAsJsonPrimitive("fileName");
        boolean fileNameEquality = testFileName != null && balFileName.equals(testFileName.getAsString());
        JsonObject modifiedDiagram = jsonModel.deepCopy();
        modifiedDiagram.addProperty("fileName", balFileName);
        boolean flowEquality = modifiedDiagram.equals(testConfig.flowModel());

        if (!fileNameEquality || !flowEquality) {
            TestConfig updateConfig = new TestConfig(testConfig.source(), testConfig.linePosition(),
                    testConfig.description(), modifiedDiagram);
//            updateConfig(configJsonPath, updateConfig);
            compareJsonElements(response, testConfig.flowModel());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "service_field_nodes";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ServiceFieldNodesTest.class;
    }

    @Override
    protected String getApiName() {
        return "getServiceNodes";
    }

    /**
     * Represents the test configuration for the flow model getFlowDesignModel API.
     *
     * @param source       The source file path
     * @param description  The description of the test
     * @param linePosition Current position
     * @param flowModel    The expected output
     */
    private record TestConfig(String source, LinePosition linePosition, String description, JsonObject flowModel) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
