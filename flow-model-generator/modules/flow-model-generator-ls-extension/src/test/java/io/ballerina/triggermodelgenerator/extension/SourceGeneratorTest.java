package io.ballerina.triggermodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.triggermodelgenerator.extension.model.Codedata;
import io.ballerina.triggermodelgenerator.extension.model.Function;
import io.ballerina.triggermodelgenerator.extension.model.Service;
import io.ballerina.triggermodelgenerator.extension.model.Trigger;
import io.ballerina.triggermodelgenerator.extension.model.Value;
import io.ballerina.triggermodelgenerator.extension.request.TriggerFunctionRequest;
import io.ballerina.triggermodelgenerator.extension.request.TriggerRequest;
import io.ballerina.triggermodelgenerator.extension.request.TriggerSourceGenRequest;
import io.ballerina.triggermodelgenerator.extension.request.TriggerSvcModelGenRequest;
import io.ballerina.triggermodelgenerator.extension.response.TriggerFunctionResponse;
import io.ballerina.triggermodelgenerator.extension.response.TriggerListResponse;
import io.ballerina.triggermodelgenerator.extension.response.TriggerResponse;
import io.ballerina.triggermodelgenerator.extension.response.TriggerSourceGenResponse;
import io.ballerina.triggermodelgenerator.extension.response.TriggerSvcModelGenResponse;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SourceGeneratorTest {
    private Endpoint serviceEndpoint;
    private BallerinaLanguageServer languageServer;
    private Path resDir;

    @BeforeClass
    public void init() {
        this.languageServer = new BallerinaLanguageServer();
        TestUtil.LanguageServerBuilder builder = TestUtil.newLanguageServer().withLanguageServer(languageServer);
        this.serviceEndpoint = builder.build();
        resDir = Paths.get("src/test/resources/triggers").toAbsolutePath();
    }

    @Test
    public void testGetTriggerList() throws ExecutionException, InterruptedException {
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getTriggerModels", null);
        TriggerListResponse response = (TriggerListResponse) result.get();
    }

    @Test
    public void testGetTriggerById() throws ExecutionException, InterruptedException {
        TriggerRequest request = new TriggerRequest("1", null, null, null, null, null, null);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getTriggerModel", request);
        TriggerResponse response = (TriggerResponse) result.get();
    }

    @Test
    public void testGetTriggerByName() throws ExecutionException, InterruptedException {
        TriggerRequest request = new TriggerRequest(null, "ballerinax", "rabbitmq", null, null, null, null);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getTriggerModel", request);
        TriggerResponse response = (TriggerResponse) result.get();
    }

    @Test
    public void testTriggersSourceGenerator() throws ExecutionException, InterruptedException {
        Path triggerPath = resDir.resolve("resources/kafka_model.json");
        Trigger trigger;
        try (InputStream inputStream = triggerPath.toUri().toURL().openStream()) {
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            trigger = new Gson().fromJson(reader, Trigger.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading trigger model from file: " + triggerPath, e);
        }
        TriggerSourceGenRequest request = new TriggerSourceGenRequest(resDir.resolve("sample1/triggers.bal").toAbsolutePath().toString(), trigger);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getSourceCode", request);
        TriggerSourceGenResponse response = (TriggerSourceGenResponse) result.get();
    }

    @Test
    public void testTriggersSourceGeneratorWithRequiredFunctions() throws ExecutionException, InterruptedException {
        Path triggerPath = resDir.resolve("resources/rabbitmq_model.json");
        Trigger trigger;
        try (InputStream inputStream = triggerPath.toUri().toURL().openStream()) {
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            trigger = new Gson().fromJson(reader, Trigger.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading trigger model from file: " + triggerPath, e);
        }
        Value required = trigger.getServices().get(0).getProperties().get("requiredFunctions");
        required.setValue("onRequest");
        TriggerSourceGenRequest request = new TriggerSourceGenRequest(resDir.resolve("sample1/triggers.bal").toAbsolutePath().toString(), trigger);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getSourceCode", request);
        TriggerSourceGenResponse response = (TriggerSourceGenResponse) result.get();
    }

    @Test
    public void testTriggerModelFromCodeGenerator() throws ExecutionException, InterruptedException {
        String filePath = resDir.resolve("sample4/triggers.bal").toAbsolutePath().toString();
        Codedata codedata = new Codedata(LineRange.from("triggers.bal", LinePosition.from(3, 0), LinePosition.from(10, 1)));
        TriggerSvcModelGenRequest request = new TriggerSvcModelGenRequest(filePath, codedata);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getTriggerModelFromCode", request);
        TriggerSvcModelGenResponse response = (TriggerSvcModelGenResponse) result.get();

        codedata = new Codedata(LineRange.from("triggers.bal", LinePosition.from(12, 0), LinePosition.from(26, 1)));
        request = new TriggerSvcModelGenRequest(filePath, codedata);
        result = serviceEndpoint.request("triggerDesignService/getTriggerModelFromCode", request);
        response = (TriggerSvcModelGenResponse) result.get();
    }

    @Test
    public void testTriggerFunctionGenerator() throws ExecutionException, InterruptedException {
        Path triggerPath = resDir.resolve("resources/kafka_model_1.json");
        Trigger trigger;
        try (InputStream inputStream = triggerPath.toUri().toURL().openStream()) {
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            trigger = new Gson().fromJson(reader, Trigger.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading trigger model from file: " + triggerPath, e);
        }
        Function function = trigger.getServices().get(0).getFunctions().get(1);
        function.setEnabled(true);
        String filePath = resDir.resolve("sample3/triggers.bal").toAbsolutePath().toString();
        TriggerFunctionRequest request = new TriggerFunctionRequest(filePath, function);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/addTriggerFunction", request);
        TriggerFunctionResponse response = (TriggerFunctionResponse) result.get();
    }

    @Test
    public void testTriggerFunctionModifier() throws ExecutionException, InterruptedException {
        String filePath = resDir.resolve("sample3/triggers.bal").toAbsolutePath().toString();
        Codedata codedata = new Codedata(LineRange.from("triggers.bal", LinePosition.from(2, 0), LinePosition.from(9, 1)));
        TriggerSvcModelGenRequest modelRequest = new TriggerSvcModelGenRequest(filePath, codedata);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getTriggerModelFromCode", modelRequest);
        TriggerSvcModelGenResponse modelResponse = (TriggerSvcModelGenResponse) result.get();

        Service service = modelResponse.service();
        Function function = service.getFunctions().get(0);
        function.getParameters().forEach(param -> param.setEnabled(true));
        filePath = resDir.resolve("sample3/triggers.bal").toAbsolutePath().toString();
        TriggerFunctionRequest request = new TriggerFunctionRequest(filePath, function);
        result = serviceEndpoint.request("triggerDesignService/updateTriggerFunction", request);
        TriggerFunctionResponse response = (TriggerFunctionResponse) result.get();
    }
}
