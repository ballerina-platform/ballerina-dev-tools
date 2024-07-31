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

package io.ballerina.flowmodelgenerator.core.central;

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
 * The proxy implementation of the central interface to obtain information about the connectors.
 *
 * @since 1.4.0
 */
public class CentralProxy implements Central {

    private final Gson gson;
    private Map<String, FlowNode> templateCache;
    private static final String NODE_TEMPLATES_JSON = "node_templates.json";
    private static final String CONNECTIONS_JSON = "connections.json";
    private static final String FUNCTIONS_JSON = "functions.json";

    public CentralProxy() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Item.class, new ItemDeserializer())
                .registerTypeAdapter(Category.class, new CategoryDeserializer())
                .create();
    }

    @Override
    public FlowNode getNodeTemplate(Codedata codedata) {
        if (templateCache == null) {
            initializeTemplateCache();
        }
        return templateCache.get(codedata.toString());
    }

    @Override
    public List<Item> getAvailableConnections() {
        Category connections = readJsonResource(CONNECTIONS_JSON, Category.class);
        return connections.items();
    }

    @Override
    public List<Item> getFunctions() {
        Category functions = readJsonResource(FUNCTIONS_JSON, Category.class);
        return functions.items();
    }

    private void initializeTemplateCache() {
        templateCache = readJsonResource(NODE_TEMPLATES_JSON, new FlowNodeTypeToken().getType());
    }

    private <T> T readJsonResource(String resourcePath, Type type) {
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

    private static class ItemDeserializer implements JsonDeserializer<Item> {

        @Override
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

        @Override
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
