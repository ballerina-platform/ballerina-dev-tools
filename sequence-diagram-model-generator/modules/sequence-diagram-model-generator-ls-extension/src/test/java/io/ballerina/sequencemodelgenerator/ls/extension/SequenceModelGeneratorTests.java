/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.sequencemodelgenerator.ls.extension;

import io.ballerina.tools.text.LinePosition;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SequenceModelGeneratorTests {
    private static final Path RES_DIR = Paths.get("src", "test", "resources").toAbsolutePath();
    private static final String BALLERINA = "ballerina";
    private static final String RESPONSES = "responses";
    private static final String SEQUENCE_DESIGN_SERVICE = "sequenceModelGeneratorService/getSequenceDiagramModel";

    private Endpoint serviceEndpoint;

    @BeforeClass
    public void startLanguageServer() {
        this.serviceEndpoint = TestUtil.initializeLanguageSever();
    }


    @Test(description = "test sequence model with conditional statements")
    public void testSequenceModel1() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "test1.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(5, 0), LinePosition.from(15, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("test1Response.json"));
    }

    @Test(description = "test sequence model with comments")
    public void testSequenceModel2() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "test2.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(12, 0), LinePosition.from(24, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("test2Response.json"));
    }


    @Test(description = "test sequence model connectors with different resource accessing formats")
    public void testSequenceModel3() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "connectorTest.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(50, 4), LinePosition.from(63, 5));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("connectorTest1.json"));
    }


    @Test(description = "test connectors with resource level initialization")
    public void testSequenceModel4() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "connectorTest.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(21, 4), LinePosition.from(30, 5));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("connectorTest2.json"));
    }

    @Test(description = "test connectors different access modifiers")
    public void testSequenceModel5() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "connectorTest2.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(25, 4), LinePosition.from(30, 5));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("connectorTest3.json"));
    }

    @Test(description = "test endpoints initialized in module level and function level")
    public void testSequenceModel6() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "connectorTest3.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(4, 0), LinePosition.from(10, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("connectorTest4.json"));
    }

    @Test(description = "test sequence model for module level endpoints without actions")
    public void testSequenceModel8() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "connectorTest4.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(5, 0), LinePosition.from(8, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("connectorTest5.json"));
    }


    private String getExpectedResponse(String fileName) throws IOException {
        return Files.readString(RES_DIR.resolve(RESPONSES).resolve(Path.of(fileName)))
                .replaceAll("\\s+", "")
                .replaceAll("\\{srcPath}", RES_DIR.toString().replaceAll("\\\\", "/"));

    }

    private String getFormattedResponse(SequenceDiagramServiceRequest request, Endpoint serviceEndpoint)
            throws ExecutionException, InterruptedException {

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
        
        return formatStdLibVersion(response.getSequenceDiagramModel().toString().replaceAll("\\s+", "")
                .replaceAll("\\\\", "/"));
    }

    private String formatStdLibVersion(String source) {
        return source
                .replaceAll("ballerina/http_[0-9].[0-9].[0-9]", "ballerina/http_2.9.0");
    }
}
