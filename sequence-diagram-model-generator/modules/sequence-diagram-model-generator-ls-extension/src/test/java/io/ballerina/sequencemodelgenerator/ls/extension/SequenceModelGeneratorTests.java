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


    @Test(description = "test seqeunce")
    public void testSequenceModelNewModel() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "newSequence.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(2, 0), LinePosition.from(8, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();


    }

    @Test(description = "test seqeunce")
    public void testSequenceModelNewModel2() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "whileForEach.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(6, 0), LinePosition.from(19, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunce")
    public void testSequenceModelNewModel5() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "test23.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(42, 4), LinePosition.from(50, 5));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }


    @Test(description = "test seqeunce")
    public void testSequenceModelNewModel4() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "testComments.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(2, 0), LinePosition.from(13, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunce")
    public void testSequenceModelNewModelNested() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "testNested.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(2, 0), LinePosition.from(19, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunce")
    public void testSequenceModelNewModel6() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve("ballerina/ballerina-internal-hr-code-main/int-oxi-allotment-ballerina-subscriber/src/oxi_allotment_ballerina_subscriber/subscriber.bal");


        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(81, 0), LinePosition.from(126, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunce")
    public void testSequenceModelNewModel7() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve("ballerina/ballerina-internal-hr-code-main/int-oxi-allotment-ballerina-subscriber/src/oxi_allotment_ballerina_subscriber/subscriber_impl.bal");


        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(7, 0), LinePosition.from(121, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunc8e")
    public void testSequenceModelNewHotelReservation() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve("ballerina/ballerina-internal-hr-code-main/api-hotel-reservation-ballerina-svc/reservation-service/reservation_service.bal");


        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(24, 4), LinePosition.from(34, 5));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunc8e")
    public void testSequenceModelNewHotelReservation4() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve("ballerina/ballerina-internal-hr-code-main/api-azure-storage-ballerina-sys/src/azure_storage_sys/service.bal");


        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(45, 4), LinePosition.from(53, 5));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }




    @Test(description = "test seqeunce")
    public void testSequenceHealthSvc() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "hospitalSvc.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(291, 4), LinePosition.from(302, 5));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunce")
    public void testSequenceGSheetSvc() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "gsheet.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(16, 0), LinePosition.from(34, 5));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }


    @Test(description = "test sequence flow control")
    public void testSequenceModelFlowControl() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "flow_control.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(9, 0), LinePosition.from(17, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test sequence flow control elseif")
    public void testSequenceModelFlowControlWithElse() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "elseElseIfbody.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(6, 0), LinePosition.from(14, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }
}
