package io.ballerina.sequencemodelgenerator.ls.extension;

import io.ballerina.tools.text.LinePosition;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
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

    @Test(description = "test seqeunce")
    public void testSequenceModel() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "sequence_service.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(19, 4), LinePosition.from(26, 5));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();


    }

    @Test(description = "test seqeunce")
    public void testSequenceModel2() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "sequence_service.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(39, 0), LinePosition.from(42, 0));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();


    }

    @Test(description = "test seqeunce")
    public void testSequenceModel3() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "sequence_2.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(2, 0), LinePosition.from(6, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();


    }

    @Test(description = "test sequence flow control")
    public void testSequenceModelFlowControl() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "flow_control.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(9, 0), LinePosition.from(26, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test sequence flow control elseif")
    public void testSequenceModelFlowControlWithElse() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "elseElseIfbody.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(6, 0), LinePosition.from(18, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }
}
