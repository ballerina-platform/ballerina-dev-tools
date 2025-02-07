package io.ballerina.flowmodelgenerator.extension.typesmanager;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.extension.AbstractLSTest;
import io.ballerina.flowmodelgenerator.extension.request.TypeUpdateRequest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test cases for retrieving types.
 *
 * @since 2.0.0
 */
public class CreateAndUpdateTypeTest extends AbstractLSTest {
    private static final Type textEditListType = new TypeToken<Map<String, List<TextEdit>>>() {
    }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        TypeUpdateRequest request = new TypeUpdateRequest(
                sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString(), testConfig.type());
        JsonElement response = getResponse(request).getAsJsonObject("textEdits");

        Map<String, List<TextEdit>> actualTextEdits = gson.fromJson(response, textEditListType);
        boolean assertFailure = false;
        Map<String, List<TextEdit>> newMap = new HashMap<>();
        for (Map.Entry<String, List<TextEdit>> entry : actualTextEdits.entrySet()) {
            Path fullPath = Paths.get(entry.getKey());
            String relativePath = sourceDir.relativize(fullPath).toString();

            List<TextEdit> textEdits = testConfig.output().get(relativePath.replace("\\", "/"));
            if (textEdits == null) {
                log.info("No text edits found for the file: " + relativePath);
                assertFailure = true;
            } else if (!assertArray("text edits", entry.getValue(), textEdits)) {
                assertFailure = true;
            }

            newMap.put(relativePath, entry.getValue());
        }

        if (assertFailure) {
            TestConfig updateConfig = new TestConfig(testConfig.filePath(), testConfig.description(),
                    testConfig.type(), newMap);
//            updateConfig(configJsonPath, updateConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("create_record_type1.json")},
                {Path.of("create_record_type2.json")},
                {Path.of("create_record_type3.json")},
                {Path.of("create_union_type.json")},
                {Path.of("create_enum_type.json")},
                {Path.of("create_table_type.json")},
                {Path.of("create_tuple_type.json")},
                {Path.of("create_intersection_type.json")},
                {Path.of("update_record_type1.json")},
                {Path.of("update_record_type2.json")},
                {Path.of("update_record_type3.json")},
                {Path.of("update_record_type4.json")},
        };
    }

    @Override
    protected String getResourceDir() {
        return "types_manager";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return CreateAndUpdateTypeTest.class;
    }

    @Override
    protected String getApiName() {
        return "updateType";
    }

    @Override
    protected String getServiceName() {
        return "typesManager";
    }

    private record TestConfig(String filePath, String description, JsonElement type,
                              Map<String, List<TextEdit>> output) {
    }
}
