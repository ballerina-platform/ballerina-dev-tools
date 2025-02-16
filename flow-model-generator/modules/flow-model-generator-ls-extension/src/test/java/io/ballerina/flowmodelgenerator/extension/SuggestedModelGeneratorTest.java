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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelSuggestedGenerationRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test cases for the flow model generator service with the AI suggestions.
 *
 * @since 2.0.0
 */
public class SuggestedModelGeneratorTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        FlowModelSuggestedGenerationRequest request = new FlowModelSuggestedGenerationRequest(
                sourceDir.resolve(testConfig.source()).toAbsolutePath().toString(), testConfig.start(),
                testConfig.end(), testConfig.text(), testConfig.position());
        JsonObject jsonModel = getResponseAndCloseFile(request, testConfig.source()).getAsJsonObject("flowModel");

        // Assert only the file name since the absolute path may vary depending on the machine
        String balFileName = Path.of(jsonModel.getAsJsonPrimitive("fileName").getAsString()).getFileName().toString();
        JsonPrimitive testFileName = testConfig.diagram().getAsJsonPrimitive("fileName");
        boolean fileNameEquality = testFileName != null && balFileName.equals(testFileName.getAsString());
        JsonObject modifiedDiagram = jsonModel.deepCopy();
        modifiedDiagram.addProperty("fileName", balFileName);

        boolean flowEquality = modifiedDiagram.equals(testConfig.diagram());
        if (!fileNameEquality || !flowEquality) {
            TestConfig updatedConfig = new TestConfig(testConfig.start(), testConfig.end(), testConfig.source(),
                    testConfig.text(), testConfig.position(), testConfig.description(), testConfig.forceAssign(),
                    modifiedDiagram);
//            updateConfig(configJsonPath, updatedConfig);
            compareJsonElements(modifiedDiagram, testConfig.diagram());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "suggested_flow_model";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ModelGeneratorTest.class;
    }

    @Override
    protected String getApiName() {
        return "getSuggestedFlowModel";
    }

    /**
     * Represents the test configuration for the model generator test.
     *
     * @param start       The start position of the diagram
     * @param end         The end position of the diagram
     * @param source      The source file
     * @param text        the AI generated text
     * @param position    the position of the AI generated text
     * @param description The description of the test
     * @param forceAssign whether to render the assign node wherever possible
     * @param diagram     The expected diagram for the given inputs
     * @since 2.0.0
     */
    private record TestConfig(LinePosition start, LinePosition end, String source, String text, LinePosition position,
                              String description, boolean forceAssign, JsonObject diagram) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
