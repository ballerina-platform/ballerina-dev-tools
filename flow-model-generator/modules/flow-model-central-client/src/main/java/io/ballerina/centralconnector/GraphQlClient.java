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

package io.ballerina.centralconnector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import io.ballerina.centralconnector.response.ConnectorApiResponse;
import io.ballerina.centralconnector.response.Function;
import io.ballerina.centralconnector.response.FunctionResponse;
import io.ballerina.centralconnector.response.FunctionsResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class provides a client to interact with the GraphQL API of Ballerina Central.
 *
 * @since 2.0.0
 */
class GraphQlClient {

    private final Map<String, String> queryMap;
    private final Gson gson;

    private static final String GRAPHQL_API = "https://api.central.ballerina.io/2.0/graphql";
    private static final String QUERY_DIRECTORY = "graphql_queries";
    private static final String GET_FUNCTIONS_QUERY = "GetFunctions.graphql";
    private static final String GET_FUNCTION_QUERY = "GetFunction.graphql";
    private static final String GET_CONNECTION_QUERY = "GetConnector.graphql";

    public GraphQlClient() {
        queryMap = new HashMap<>();

        gson = new GsonBuilder()
                .registerTypeAdapter(FunctionsResponse.Module.class, new FunctionsModuleDeserializer())
                .registerTypeAdapter(FunctionResponse.Module.class, new FunctionModuleDeserializer())
                .registerTypeAdapter(ConnectorApiResponse.Module.class, new ConnectorApiModuleDeserializer())
                .create();
    }

    public FunctionsResponse getFunctions(String org, String module, String version) {
        String queryTemplate = getQueryTemplate(GET_FUNCTIONS_QUERY);
        String queryBody = String.format(queryTemplate, org, module, version);
        String response = query(queryBody);
        return gson.fromJson(response, FunctionsResponse.class);
    }

    public FunctionResponse getFunction(String organization, String name, String version, String functionName) {
        String queryTemplate = getQueryTemplate(GET_FUNCTION_QUERY);
        String queryBody = String.format(queryTemplate, organization, name, version, functionName);
        String response = query(queryBody);
        return gson.fromJson(response, FunctionResponse.class);
    }

    @Deprecated
    public ConnectorApiResponse getConnector(String organization, String name, String version, String clientName) {
        String queryTemplate = getQueryTemplate(GET_CONNECTION_QUERY);
        String queryBody = String.format(queryTemplate, organization, name, version, clientName);
        String response = query(queryBody);
        return gson.fromJson(response, ConnectorApiResponse.class);
    }

    private String query(String queryBody) {
        String query = String.format("{\"query\": \"%s\"}", queryBody);
        HttpURLConnection conn = null;
        try {
            URL url = new URL(GRAPHQL_API);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Write the request body
            try (var os = conn.getOutputStream()) {
                byte[] input = query.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read the response
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String getQueryTemplate(String queryName) {
        String queryTemplate = queryMap.get(queryName);
        if (queryTemplate == null) {
            queryTemplate = readResourceFile(Path.of(QUERY_DIRECTORY, queryName).toString());
            queryMap.put(queryName, queryTemplate);
        }
        return queryTemplate;
    }

    private String readResourceFile(String resourcePath) {
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new RuntimeException("Error reading resource: " + resourcePath, e);
        }
    }

    private static boolean isJsonString(JsonElement modulesElement) {
        return modulesElement != null && modulesElement.isJsonPrimitive() &&
                modulesElement.getAsJsonPrimitive().isString();
    }

    private static class FunctionsModuleDeserializer implements JsonDeserializer<FunctionsResponse.Module> {

        @Override
        public FunctionsResponse.Module deserialize(JsonElement jsonElement, Type type,
                                                    JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement modulesElement = jsonObject.get("functions");

            if (isJsonString(modulesElement)) {
                String modulesString = modulesElement.getAsString();
                List<Function> functions = new Gson().fromJson(modulesString, new FunctionListTypeToken().getType());
                return new FunctionsResponse.Module(functions);
            }
            return new Gson().fromJson(jsonObject, FunctionsResponse.Module.class);
        }
    }

    private static class FunctionModuleDeserializer implements JsonDeserializer<FunctionResponse.Module> {

        @Override
        public FunctionResponse.Module deserialize(JsonElement jsonElement, Type type,
                                                   JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement modulesElement = jsonObject.get("functions");

            if (isJsonString(modulesElement)) {
                String modulesString = modulesElement.getAsString();
                Function function = new Gson().fromJson(modulesString, Function.class);
                return new FunctionResponse.Module(function);
            }
            return new Gson().fromJson(jsonObject, FunctionResponse.Module.class);
        }
    }

    private static class FunctionListTypeToken extends TypeToken<List<Function>> { }

    private static class ConnectorApiModuleDeserializer implements JsonDeserializer<ConnectorApiResponse.Module> {

        @Override
        public ConnectorApiResponse.Module deserialize(JsonElement jsonElement, Type type,
                                                       JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement modulesElement = jsonObject.get("clients");

            if (isJsonString(modulesElement)) {
                String modulesString = modulesElement.getAsString();
                return new Gson().fromJson(modulesString, ConnectorApiResponse.Module.class);
            }
            return new Gson().fromJson(jsonObject, ConnectorApiResponse.Module.class);
        }
    }
}
