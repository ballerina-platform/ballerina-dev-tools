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

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.flowmodelgenerator.core.model.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the Central API using a local index.
 *
 * @since 2.0.0
 */
public class LocalIndexCentral {

    private final Gson gson;
    private Map<String, FlowNode> templateCache;
    private Map<String, List<Item>> connectionMap;
    private static final String NODE_TEMPLATES_JSON = "node_templates.json";
    private static final String CONNECTORS_JSON = "connectors.json";
    private static final String CONNECTIONS_JSON = "connections.json";
    private static final String FUNCTIONS_JSON = "functions.json";

    private static final class CentralProxyHolder {

        private static final LocalIndexCentral instance = new LocalIndexCentral();
    }

    public static LocalIndexCentral getInstance() {
        return CentralProxyHolder.instance;
    }

    public LocalIndexCentral() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Item.class, new ItemDeserializer())
                .registerTypeAdapter(Category.class, new CategoryDeserializer())
                .create();
    }

    public FlowNode getNodeTemplate(Codedata codedata) {
        if (templateCache == null) {
            initializeTemplateCache();
        }
        return templateCache.get(codedata.toString());
    }

    public List<Item> getConnectors() {
        Category connectors = readJsonResource(CONNECTORS_JSON, Category.class);
        return connectors.items();
    }

    public List<Item> getFunctions() {
        Category functions = readJsonResource(FUNCTIONS_JSON, Category.class);
        return functions.items();
    }

    public List<Item> getConnectorActions(Codedata codedata) {
        if (connectionMap == null) {
            initializeConnectionMap();
        }
        return connectionMap.get(codedata.toString());
    }

    public List<AvailableNode> getConnectors(Map<String, String> queryMap) {
        List<Item> connectors = getConnectors();
        String query = queryMap.getOrDefault("q", "");
        int limit = Integer.parseInt(queryMap.getOrDefault("limit", "10"));
        int offset = Integer.parseInt(queryMap.getOrDefault("offset", "0"));

        List<AvailableNode> availableNodes = new ArrayList<>();
        for (Item item : connectors) {
            if (item instanceof Category) {
                availableNodes.addAll(getAvailableNodesFromCategory((Category) item));
            } else if (item instanceof AvailableNode) {
                availableNodes.add((AvailableNode) item);
            }
        }
        return availableNodes.stream()
                .filter(node -> node.codedata().object().contains(query) || node.codedata().module().contains(query))
                .skip(offset)
                .limit(limit)
                .toList();
    }
    
    private List<AvailableNode> getAvailableNodesFromCategory(Category category) {
        List<AvailableNode> availableNodes = new ArrayList<>();
        for (Item item : category.items()) {
            if (item instanceof Category) {
                availableNodes.addAll(getAvailableNodesFromCategory((Category) item));
            } else if (item instanceof AvailableNode) {
                availableNodes.add((AvailableNode) item);
            }
        }
        return availableNodes;
    }

    private void initializeTemplateCache() {
        templateCache = readJsonResource(NODE_TEMPLATES_JSON, new FlowNodeTypeToken().getType());
    }

    private void initializeConnectionMap() {
        connectionMap = readJsonResource(CONNECTIONS_JSON, new ConnectionTypeToken().getType());
    }

    public  <T> T readJsonResource(String resourcePath, Type type) {
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class FlowNodeTypeToken extends TypeToken<Map<String, FlowNode>> {

    }

    private static class ConnectionTypeToken extends TypeToken<Map<String, List<Item>>> {

    }

    private static class ItemDeserializer implements JsonDeserializer<Item> {

        public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (jsonObject.has("items")) {
                return context.deserialize(jsonObject, Category.class);
            } else if (jsonObject.has("enabled")) {
                return context.deserialize(jsonObject, AvailableNode.class);
            } else {
                throw new JsonParseException("Unknown type of Item");
            }
        }
    }

    private static class CategoryDeserializer implements JsonDeserializer<Category> {

        public Category deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            Metadata metadata = context.deserialize(jsonObject.get("metadata"), Metadata.class);

            JsonArray itemsArray = jsonObject.getAsJsonArray("items");
            List<Item> items = new ArrayList<>();
            for (JsonElement itemElement : itemsArray) {
                items.add(context.deserialize(itemElement, Item.class));
            }

            return new Category(metadata, items);
        }
    }
}
