/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core.type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the record value for a type configuration.
 *
 * @since 2.0.0
 */
public class RecordValueGenerator {

    public static String generate(JsonObject json) {
        StringBuilder builder = new StringBuilder();
        boolean hasTypeName = json.has("typeName");
        if (hasTypeName) {
            String typeName = json.get("typeName").getAsString();
            switch (typeName) {
                case "record" -> generateRecordValue(json, builder);
                case "union" -> generateUnionValue(json, builder);
                case "enum" -> generateEnumValue(json, builder);
                default -> {
                    if (json.has("value") && !json.get("value").getAsString().isEmpty()) {
                        builder.append(json.get("value").getAsString());
                    } else if (json.has("defaultValue") &&
                            !json.get("defaultValue").getAsString().isEmpty()) {
                        builder.append(json.get("defaultValue").getAsString());
                    } else {
                        StringBuilder sb = new StringBuilder();
                        generateDefaultValue(json, sb);
                        builder.append(sb);
                    }
                }
            }
        }
        return builder.toString();
    }

    public static void generateEnumValue(JsonObject jsonObject, StringBuilder builder) {
        if (jsonObject.has("members") && jsonObject.get("members").isJsonArray()) {
            JsonElement members = jsonObject.get("members");
            // iterate over each member until the array has a selected true value else get the default value
            for (JsonElement member : members.getAsJsonArray()) {
                JsonObject memberObj = member.getAsJsonObject();
                if (memberObj.has("selected") && memberObj.get("selected").getAsBoolean()) {
                    if (memberObj.has("value") && !memberObj.get("value").getAsString().isEmpty()) {
                        builder.append(memberObj.get("value").getAsString());
                    } else if (memberObj.has("defaultValue") &&
                            !memberObj.get("defaultValue").getAsString().isEmpty()) {
                        builder.append(memberObj.get("defaultValue").getAsString());
                    } else {
                        StringBuilder sb = new StringBuilder();
                        generateDefaultValue(memberObj, sb);
                        builder.append(sb);
                    }
                    break;
                }
            }
        }
    }

    public static void generateUnionValue(JsonObject union, StringBuilder builder) {
        if (union.has("members")) {
            JsonElement members = union.get("members");
            if (members.isJsonArray()) {
                for (JsonElement member : members.getAsJsonArray()) {
                    JsonObject memberObj = member.getAsJsonObject();
                    if (memberObj.has("selected") && memberObj.get("selected").getAsBoolean()) {
                        if (memberObj.has("typeName")) {
                            String typeName = memberObj.get("typeName").getAsString();
                            switch (typeName) {
                                case "record" -> generateRecordValue(memberObj, builder);
                                case "union" -> generateUnionValue(memberObj, builder);
                                case "enum" -> generateEnumValue(memberObj, builder);
                                default -> {
                                    if (memberObj.has("value") &&
                                            !memberObj.get("value").getAsString().isEmpty()) {
                                        builder.append(memberObj.get("value").getAsString());
                                    } else if (memberObj.has("defaultValue")
                                            && !memberObj.get("defaultValue").getAsString().isEmpty()) {
                                        builder.append(memberObj.get("defaultValue").getAsString());
                                    } else {
                                        StringBuilder sb = new StringBuilder();
                                        generateDefaultValue(memberObj, sb);
                                        builder.append(sb);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    public static void generateRecordValue(JsonObject jsonObject, StringBuilder builder) {
        if (jsonObject.has("selected") && !jsonObject.get("selected").getAsBoolean()) {
            return;
        }

        builder.append("{");
        List<String> fieldValues = new ArrayList<>();

        if (jsonObject.has("fields")) {
            JsonElement fields = jsonObject.get("fields");
            if (fields.isJsonArray()) {
                for (JsonElement field : fields.getAsJsonArray()) {
                    JsonObject fieldObj = field.getAsJsonObject();
                    if (fieldObj.has("selected") && fieldObj.get("selected").getAsBoolean()) {
                        if (fieldObj.has("typeName")) {
                            String typeName = fieldObj.get("typeName").getAsString();
                            switch (typeName) {
                                case "record" -> {
                                    StringBuilder sb = new StringBuilder();
                                    generateRecordValue(fieldObj, sb);
                                    fieldValues.add(fieldObj.get("name").getAsString() + ": " + sb);
                                }
                                case "union" -> {
                                    StringBuilder sb = new StringBuilder();
                                    generateUnionValue(fieldObj, sb);
                                    fieldValues.add(fieldObj.get("name").getAsString() + ": " + sb);
                                }
                                case "enum" -> {
                                    StringBuilder sb = new StringBuilder();
                                    generateEnumValue(fieldObj, sb);
                                    fieldValues.add(fieldObj.get("name").getAsString() + ": " + sb);
                                }
                                default -> {
                                    if (fieldObj.has("value") && !fieldObj.get("value")
                                            .getAsString().isEmpty()) {
                                        fieldValues.add(fieldObj.get("name").getAsString()
                                                + ": " + fieldObj.get("value").getAsString());
                                    } else if (fieldObj.has("defaultValue")
                                            && !fieldObj.get("defaultValue").getAsString().isEmpty()) {
                                        fieldValues.add(fieldObj.get("name").getAsString() +
                                                ": " + fieldObj.get("defaultValue").getAsString());
                                    } else {
                                        StringBuilder sb = new StringBuilder();
                                        generateDefaultValue(fieldObj, sb);
                                        fieldValues.add(fieldObj.get("name").getAsString() + ": " + sb);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        builder.append(String.join(", ", fieldValues));
        builder.append("}");
    }

    private static void generateDefaultValue(JsonObject jsonObject, StringBuilder builder) {
        if (!jsonObject.has("typeName")) {
            return;
        }
        String typeName = jsonObject.get("typeName").getAsString();
        switch (typeName) {
            case "record" -> generateRecordValue(jsonObject, builder);
            case "union" -> generateUnionValue(jsonObject, builder);
            case "array" -> builder.append("[]");
            case "enum" -> {
                if (jsonObject.has("members") && jsonObject.get("members").isJsonArray()) {
                    JsonElement members = jsonObject.get("members");
                    if (!members.getAsJsonArray().isEmpty()) {
                        generateDefaultValue(members.getAsJsonArray().get(0).getAsJsonObject(), builder);
                    } else {
                        builder.append("\"\"");
                    }
                }
            }
            case "error" -> builder.append("error(\"Custom Error\")");
            case "map" -> builder.append("{}");
            case "object" -> builder.append("object {}");
            case "stream" -> builder.append("new;");
            case "table" -> builder.append("table []");
            default -> {
                switch (typeName) {
                    case "any", "anydata", "json" -> builder.append("()");
                    case "xml" -> builder.append("xml ``");
                    case "string" -> builder.append("\"\"");
                    case "string:Char" -> builder.append("\"a\"");
                    case "int", "byte" -> builder.append("0");
                    case "float" -> builder.append("0.0");
                    case "decimal" -> builder.append("0.0d");
                    case "boolean" -> builder.append("false");
                    default -> builder.append("\"%s\"".formatted(typeName));
                }
            }
        }
    }
}
