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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelSourceGeneratorServiceRequest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the flow model source generator service.
 *
 * @since 2201.9.0
 */
public class SourceGeneratorTest extends AbstractLSTest {

    private static final Type textEditListType = new TypeToken<List<TextEdit>>() {}.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = RES_DIR.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String response = getResponse(new FlowModelSourceGeneratorServiceRequest(testConfig.diagram()));
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonArray jsonArray = json.getAsJsonObject("result").getAsJsonArray("textEdits");

        List<TextEdit> actualTextEdits = gson.fromJson(jsonArray, textEditListType);
        List<TextEdit> expectedTextEdits = testConfig.output();
        List<TextEdit> mismatchedTextEdits = new ArrayList<>();

        int actualTextEditsSize = actualTextEdits.size();
        int expectedTextEditsSize = expectedTextEdits.size();
        boolean hasCountMatch = actualTextEditsSize == expectedTextEditsSize;
        if (!hasCountMatch) {
            LOG.error(String.format("Mismatched text edits count. Expected: %d, Found: %d",
                    expectedTextEditsSize, actualTextEditsSize));
        }

        for (TextEdit actualTextEdit : actualTextEdits) {
            if (expectedTextEdits.contains(actualTextEdit)) {
                expectedTextEdits.remove(actualTextEdit);
            } else {
                mismatchedTextEdits.add(actualTextEdit);
            }
        }

        boolean hasAllExpectedTextEdits = expectedTextEdits.isEmpty();
        if (!hasAllExpectedTextEdits) {
            LOG.error("Found in expected text edits but not in actual text edits: " + expectedTextEdits);
        }

        boolean hasRelevantTextEdits = mismatchedTextEdits.isEmpty();
        if (!hasRelevantTextEdits) {
            LOG.error("Found in actual text edits but not in expected text edits: " + mismatchedTextEdits);
        }

        if (!hasCountMatch || !hasAllExpectedTextEdits || !hasRelevantTextEdits) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), actualTextEdits, testConfig.diagram());
//            updateConfig(configJsonPath, updatedConfig);
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

    private record TestConfig(String description, List<TextEdit> output, JsonElement diagram) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
