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

package io.ballerina.servicemodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Trigger;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.request.TriggerFunctionRequest;
import io.ballerina.servicemodelgenerator.extension.request.TriggerModelGenRequest;
import io.ballerina.servicemodelgenerator.extension.request.TriggerRequest;
import io.ballerina.servicemodelgenerator.extension.request.TriggerSourceRequest;
import io.ballerina.servicemodelgenerator.extension.response.TriggerCommonResponse;
import io.ballerina.servicemodelgenerator.extension.response.TriggerListResponse;
import io.ballerina.servicemodelgenerator.extension.response.TriggerModelGenResponse;
import io.ballerina.servicemodelgenerator.extension.response.TriggerResponse;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
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

public class TriggerServiceTest {
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
        TriggerSourceRequest request = new TriggerSourceRequest(resDir.resolve("sample1/triggers.bal")
                .toAbsolutePath().toString(), trigger);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getSourceCode", request);
        TriggerCommonResponse response = (TriggerCommonResponse) result.get();
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
        Value required = trigger.getProperties().get("requiredFunctions");
        required.setValue("onRequest");
        TriggerSourceRequest request = new TriggerSourceRequest(resDir.resolve("sample1/triggers.bal")
                .toAbsolutePath().toString(), trigger);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getSourceCode", request);
        TriggerCommonResponse response = (TriggerCommonResponse) result.get();
    }

    @Test
    public void testTriggerModelFromCodeGenerator() throws ExecutionException, InterruptedException {
        String filePath = resDir.resolve("sample4/triggers.bal").toAbsolutePath().toString();
        Codedata codedata = new Codedata(LineRange.from("triggers.bal", LinePosition.from(6, 0),
                LinePosition.from(13, 1)));
        TriggerModelGenRequest request = new TriggerModelGenRequest(filePath, codedata);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getTriggerModelFromCode",
                request);
        TriggerModelGenResponse response = (TriggerModelGenResponse) result.get();

        TriggerSourceRequest request1 = new TriggerSourceRequest(filePath, response.trigger());
        CompletableFuture<?> result1 = serviceEndpoint.request("triggerDesignService/getSourceCode", request1);
        TriggerCommonResponse response1 = (TriggerCommonResponse) result1.get();

        codedata = new Codedata(LineRange.from("triggers.bal", LinePosition.from(18, 0),
                LinePosition.from(32, 1)));
        request = new TriggerModelGenRequest(filePath, codedata);
        result = serviceEndpoint.request("triggerDesignService/getTriggerModelFromCode", request);
        response = (TriggerModelGenResponse) result.get();

        request1 = new TriggerSourceRequest(filePath, response.trigger());
        result1 = serviceEndpoint.request("triggerDesignService/getSourceCode", request1);
        response1 = (TriggerCommonResponse) result1.get();
    }

    @Test
    public void testTriggerFunctionGenerator() throws ExecutionException, InterruptedException {
        Path triggerPath = resDir.resolve("resources/kafka_model.json");
        Trigger trigger;
        try (InputStream inputStream = triggerPath.toUri().toURL().openStream()) {
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            trigger = new Gson().fromJson(reader, Trigger.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading trigger model from file: " + triggerPath, e);
        }
        Function function = trigger.getService().getFunctions().get(1);
        function.setEnabled(true);
        String filePath = resDir.resolve("sample2/triggers.bal").toAbsolutePath().toString();
        Codedata codedata = new Codedata(LineRange.from("triggers.bal", LinePosition.from(2, 0),
                LinePosition.from(9, 1)));
        TriggerFunctionRequest request = new TriggerFunctionRequest(filePath, function, codedata);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/addTriggerFunction", request);
        TriggerCommonResponse response = (TriggerCommonResponse) result.get();
    }

    @Test
    public void testTriggerFunctionModifier() throws ExecutionException, InterruptedException {
        String filePath = resDir.resolve("sample3/triggers.bal").toAbsolutePath().toString();
        Codedata codedata = new Codedata(LineRange.from("triggers.bal", LinePosition.from(2, 0),
                LinePosition.from(16, 1)));
        TriggerModelGenRequest modelRequest = new TriggerModelGenRequest(filePath, codedata);
        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getTriggerModelFromCode",
                modelRequest);
        TriggerModelGenResponse modelResponse = (TriggerModelGenResponse) result.get();

        Service service = modelResponse.trigger().getService();
        Function function = service.getFunctions().get(0);
        function.getParameters().forEach(param -> param.setEnabled(true));
        filePath = resDir.resolve("sample3/triggers.bal").toAbsolutePath().toString();
        TriggerFunctionRequest request = new TriggerFunctionRequest(filePath, function, codedata);
        result = serviceEndpoint.request("triggerDesignService/updateTriggerFunction", request);
        TriggerCommonResponse response = (TriggerCommonResponse) result.get();
    }

//    @Test
//    public void testTriggerModifier() throws ExecutionException, InterruptedException {
//        String filePath = resDir.resolve("sample3/triggers.bal").toAbsolutePath().toString();
//        Codedata codedata = new Codedata(LineRange.from("triggers.bal", LinePosition.from(2, 0),
//                LinePosition.from(16, 1)));
//        TriggerModelGenRequest modelRequest = new TriggerModelGenRequest(filePath, codedata);
//        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getTriggerModelFromCode",
//                modelRequest);
//        TriggerModelGenResponse modelResponse = (TriggerModelGenResponse) result.get();
//
//        Trigger trigger = modelResponse.trigger();
//        trigger.getProperty("bootstrapServers").setValue("\"localhost:9090\"");
//        trigger.getProperty("topics").setValue("[\"topic1\", \"topic2\"]");
//        trigger.getProperty("offsetReset").setValue("\"earliest\"");
//        trigger.getProperty("offsetReset").setEnabled(true);
//        filePath = resDir.resolve("sample3/triggers.bal").toAbsolutePath().toString();
//        TriggerServiceModifierRequest request = new TriggerServiceModifierRequest(filePath, trigger, codedata);
//        result = serviceEndpoint.request("triggerDesignService/updateTrigger", request);
//        TriggerCommonResponse response = (TriggerCommonResponse) result.get();
//    }

//    @Test
//    public void testTriggerModifierWithBasePath() throws ExecutionException, InterruptedException {
//        String filePath = resDir.resolve("sample4/triggers.bal").toAbsolutePath().toString();
//        Codedata codedata = new Codedata(LineRange.from("triggers.bal", LinePosition.from(3, 0),
//                LinePosition.from(10, 1)));
//        TriggerModelGenRequest modelRequest = new TriggerModelGenRequest(filePath, codedata);
//        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getTriggerModelFromCode",
//                modelRequest);
//        TriggerModelGenResponse modelResponse = (TriggerModelGenResponse) result.get();
//
//        Trigger trigger = modelResponse.trigger();
//        trigger.getProperty("port").setValue("9099");
//        trigger.getProperty("username").setValue("\"user\"");
//        Optional<Value> basePathProperty = trigger.getBasePathProperty();
//        basePathProperty.ifPresent(value -> value.setValue("\"/New-Queue\""));
//        filePath = resDir.resolve("sample4/triggers.bal").toAbsolutePath().toString();
//        TriggerServiceModifierRequest request = new TriggerServiceModifierRequest(filePath, trigger, codedata);
//        result = serviceEndpoint.request("triggerDesignService/updateTrigger", request);
//        TriggerCommonResponse response = (TriggerCommonResponse) result.get();
//    }

//    @Test
//    public void testTriggerModifierWithName() throws ExecutionException, InterruptedException {
//        String filePath = resDir.resolve("sample4/triggers.bal").toAbsolutePath().toString();
//        Codedata codedata = new Codedata(LineRange.from("triggers.bal", LinePosition.from(3, 0),
//                LinePosition.from(10, 1)));
//        TriggerModelGenRequest modelRequest = new TriggerModelGenRequest(filePath, codedata);
//        CompletableFuture<?> result = serviceEndpoint.request("triggerDesignService/getTriggerModelFromCode",
//                modelRequest);
//        TriggerModelGenResponse modelResponse = (TriggerModelGenResponse) result.get();
//
//        Trigger trigger = modelResponse.trigger();
//        trigger.getProperty("name").setValue("service-rabbitmq");
//        filePath = resDir.resolve("sample4/triggers.bal").toAbsolutePath().toString();
//        TriggerServiceModifierRequest request = new TriggerServiceModifierRequest(filePath, trigger, codedata);
//        result = serviceEndpoint.request("triggerDesignService/updateTrigger", request);
//        TriggerCommonResponse response = (TriggerCommonResponse) result.get();
//    }
}
