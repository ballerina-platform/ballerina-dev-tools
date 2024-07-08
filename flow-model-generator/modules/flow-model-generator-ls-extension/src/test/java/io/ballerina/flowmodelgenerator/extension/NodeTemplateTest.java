package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelNodeTemplateRequest;
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
        Path configJsonPath = resDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        FlowModelNodeTemplateRequest request = new FlowModelNodeTemplateRequest(testConfig.kind().name());
        JsonElement nodeTemplate = getResponse(request).get("flowNode");

        if (!nodeTemplate.equals(testConfig.output())) {
            TestConfig updateConfig = new TestConfig(testConfig.description(), testConfig.kind(), nodeTemplate);
//            updateConfig(config, updateConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
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
     * @param description The description of the test
     * @param kind        The kind of the node
     * @param output      The expected output
     */
    private record TestConfig(String description, FlowNode.Kind kind, JsonElement output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
