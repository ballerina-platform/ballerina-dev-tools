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
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.Diagram;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelGeneratorRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowNodeDeleteRequest;
import io.ballerina.tools.text.LinePosition;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tests for the flow model source generator service.
 *
 * @since 1.4.0
 */
public class DeleteNodeTest extends AbstractLSTest {

    private static final Type textEditListType = new TypeToken<Map<String, List<TextEdit>>>() {
    }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        String sourceFile = sourceDir.resolve(testConfig.source()).toAbsolutePath().toString();
        FlowModelGeneratorRequest request = new FlowModelGeneratorRequest(sourceFile, testConfig.functionStart(),
                testConfig.functionEnd());
        JsonObject jsonMap = getResponse(request, "flowDesignService/getFlowModel").getAsJsonObject("flowModel");

        Diagram diagram = gson.fromJson(jsonMap, Diagram.class);
        FlowNode nodeToDelete = null;
        for (FlowNode node : diagram.nodes()) {
            Optional<FlowNode> optNodeToDelete = findNodeToDelete(node, testConfig.nodeStart(), testConfig.nodeEnd());
            if (optNodeToDelete.isPresent()) {
                nodeToDelete = optNodeToDelete.get();
            }
        }
        if (nodeToDelete == null) {
            Assert.fail(String.format("Failed test: '%s', cannot find the node to delete", configJsonPath));
        }
        FlowNodeDeleteRequest deleteRequest = new FlowNodeDeleteRequest(sourceFile, gson.toJsonTree(nodeToDelete));
        JsonObject deleteResponse = getResponse(deleteRequest).getAsJsonObject("textEdits");
        Map<String, List<TextEdit>> actualTextEdits = gson.fromJson(deleteResponse, textEditListType);

        assertTextEdits(actualTextEdits, testConfig, configJsonPath);
    }

    @Override
    protected String[] skipList() {
        // TODO: Remove after fixing the log symbol issue
        return new String[]{
                "delete_node8.json"
        };
    }

    private void assertTextEdits(Map<String, List<TextEdit>> actualTextEdits,
                                 TestConfig testConfig, Path configJsonPath) {
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
            TestConfig updatedConfig =
                    new TestConfig(testConfig.description(), testConfig.functionStart(), testConfig.functionEnd(),
                            testConfig.nodeStart(), testConfig.nodeEnd(), testConfig.source(), newMap);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    private Optional<FlowNode> findNodeToDelete(FlowNode node, LinePosition deleteNodeStart,
                                                LinePosition deleteNodeEnd) {
        LinePosition nodeStart = node.codedata().lineRange().startLine();
        LinePosition nodeEnd = node.codedata().lineRange().endLine();
        if (nodeStart.equals(deleteNodeStart) && nodeEnd.equals(deleteNodeEnd)) {
            return Optional.of(node);
        } else if (nodeStart.line() <= deleteNodeStart.line() && nodeEnd.line() >= deleteNodeEnd.line()) {
            List<Branch> branches = node.branches();
            if (branches == null) {
                return Optional.empty();
            }
            for (Branch branch : branches) {
                for (FlowNode child : branch.children()) {
                    Optional<FlowNode> nodeToDelete = findNodeToDelete(child, deleteNodeStart, deleteNodeEnd);
                    if (nodeToDelete.isPresent()) {
                        return nodeToDelete;
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    protected String getResourceDir() {
        return "delete_node";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return DeleteNodeTest.class;
    }

    @Override
    protected String getApiName() {
        return "deleteFlowNode";
    }

    /**
     * Represents the test configuration for the delete node test.
     *
     * @param description   The description of the test
     * @param functionStart The start position of the function that contains the node to be deleted
     * @param functionEnd   The end position of the function that contains the node to be deleted
     * @param nodeStart     The end position of the node to be deleted
     * @param nodeEnd       The start position of the node to be deleted
     * @param source        The source file that contains the nodes to be deleted
     * @param output        The expected output
     */
    private record TestConfig(String description, LinePosition functionStart, LinePosition functionEnd,
                              LinePosition nodeStart, LinePosition nodeEnd, String source,
                              Map<String, List<TextEdit>> output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
