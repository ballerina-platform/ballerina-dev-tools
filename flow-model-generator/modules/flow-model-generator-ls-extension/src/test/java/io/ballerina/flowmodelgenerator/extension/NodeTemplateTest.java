package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelNodeTemplateRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test cases for the flow model getNodeTemplate API.
 *
 * @since 1.4.0
 */
public class NodeTemplateTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        String filePath =
                testConfig.source() == null ? "" : sourceDir.resolve(testConfig.source()).toAbsolutePath().toString();
        FlowModelNodeTemplateRequest request =
                new FlowModelNodeTemplateRequest(filePath, testConfig.position(), testConfig.codedata());
        JsonElement nodeTemplate = getResponse(request).get("flowNode");

        if (!nodeTemplate.equals(testConfig.output())) {
            TestConfig updateConfig = new TestConfig(testConfig.source(), testConfig.position(),
                    testConfig.description(), testConfig.codedata(), nodeTemplate);
//            updateConfig(configJsonPath, updateConfig);
            compareJsonElements(nodeTemplate, testConfig.output());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String[] skipList() {
        // TODO: Re-enable once the ballerinax/np module is available
        return new String[]{
                "np_function_call_1.json",
                "np_function_call_2.json",
                "np_function_call_3.json",
        };
    }

    @Override
    protected String getResourceDir() {
        return "node_template";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return NodeTemplateTest.class;
    }

    @Override
    protected String getApiName() {
        return "getNodeTemplate";
    }

    /**
     * Represents the test configuration for the flow model getNodeTemplate API.
     *
     * @param source      The source file path
     * @param position    The position of the node to be added
     * @param description The description of the test
     * @param codedata    The codedata of the node
     * @param output      The expected output
     */
    private record TestConfig(String source, LinePosition position, String description, JsonObject codedata,
                              JsonElement output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
