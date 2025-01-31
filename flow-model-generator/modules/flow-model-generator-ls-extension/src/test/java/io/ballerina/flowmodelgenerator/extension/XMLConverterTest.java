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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.core.model.TypeData;
import io.ballerina.flowmodelgenerator.extension.request.TypeUpdateRequest;
import io.ballerina.flowmodelgenerator.extension.request.XMLToRecordRequest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Test cases for the flow model generator service.
 *
 * @since 2.0.0
 */
public class XMLConverterTest extends AbstractLSTest {

    private static final Type textEditListType = new TypeToken<Map<String, List<TextEdit>>>() {
    }.getType();

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("config1.json")},
                {Path.of("config2.json")},
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        String sourceFile = sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString();
        XMLToRecordRequest request = new XMLToRecordRequest(testConfig.xmlString(), testConfig.isRecordTypeDesc(),
                testConfig.isClosed(), true, "text", false, false, false, sourceFile, testConfig.prefix());
        JsonArray records = getResponse(request).getAsJsonArray("types");

        StringBuilder sb = new StringBuilder();
        for (JsonElement record : records) {
            String typeStr = record.getAsJsonObject().get("type").toString();
            String[] splits = typeStr.split("properties");
            StringBuilder rest = new StringBuilder();
            for (int i = 1; i < splits.length; i++) {
                rest.append(splits[i]);
            }
            String newType = typeStr.split("codedata")[0] + "codedata\": {\"node\":\"RECORD\"}," +
                    "\"properties" + rest;
            TypeUpdateRequest updateRequest =
                    new TypeUpdateRequest(sourceFile, gson.toJsonTree(gson.fromJson(newType, TypeData.class)));
            JsonElement response = getResponse(updateRequest, "typesManager/updateType").getAsJsonObject("textEdits");
            Map<String, List<TextEdit>> actualTextEdits = gson.fromJson(response, textEditListType);
            for (Map.Entry<String, List<TextEdit>> entry : actualTextEdits.entrySet()) {
                for (TextEdit textEdit : entry.getValue()) {
                    sb.append(textEdit.getNewText()).append(System.lineSeparator());
                }
            }
        }

        String generatedRecords = sb.toString().replaceAll("\\s+", "");
        String expectedRecords = testConfig.records().replaceAll("\\s+", "");

        if (!generatedRecords.equals(expectedRecords)) {
            TestConfig updatedConfig = new TestConfig(testConfig.filePath(), testConfig.xmlString(),
                    testConfig.prefix(), testConfig.isClosed(), testConfig.isRecordTypeDesc(), sb.toString());
            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.filePath(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "xml_converter";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return XMLConverterTest.class;
    }

    @Override
    protected String getApiName() {
        return "convert";
    }

    @Override
    protected String getServiceName() {
        return "xmlToRecord";
    }

    private record TestConfig(String filePath, String xmlString, String prefix, boolean isClosed,
                              boolean isRecordTypeDesc, String records) {
    }
}
