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
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelSourceGeneratorRequest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests for the flow model source generator service.
 *
 * @since 1.4.0
 */
public class SourceGeneratorTest extends AbstractLSTest {

    private static final Type textEditListType = new TypeToken<List<TextEdit>>() {
    }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = resDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        FlowModelSourceGeneratorRequest request = new FlowModelSourceGeneratorRequest(testConfig.diagram());
        JsonArray jsonArray = getResponse(request).getAsJsonArray("textEdits");

        List<TextEdit> actualTextEdits = gson.fromJson(jsonArray, textEditListType);
        if (!assertArray("text edits", actualTextEdits, testConfig.output())) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.diagram(), actualTextEdits);
            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "to_source";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return SourceGeneratorTest.class;
    }

    @Override
    protected String getApiName() {
        return "getSourceCode";
    }

    @Override
    protected String[] skipList() {
        //TODO: The tests are failing in Windows: https://github.com/ballerina-platform/ballerina-lang/issues/42932
        return new String[]{
                "http_get_node2.json",
                "http_post_node2.json"
        };
    }

    /**
     * Represents the test configuration for the source generator test.
     *
     * @param description The description of the test
     * @param diagram     The diagram to generate the source code
     * @param output      The expected output source code
     */
    private record TestConfig(String description, JsonElement diagram, List<TextEdit> output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
