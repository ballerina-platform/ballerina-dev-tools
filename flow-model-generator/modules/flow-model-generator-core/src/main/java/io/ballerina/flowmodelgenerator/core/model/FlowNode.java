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

package io.ballerina.flowmodelgenerator.core.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.flowmodelgenerator.core.model.properties.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.properties.HttpApiEvent;
import io.ballerina.flowmodelgenerator.core.model.properties.HttpGet;
import io.ballerina.flowmodelgenerator.core.model.properties.HttpPost;
import io.ballerina.flowmodelgenerator.core.model.properties.IfNode;
import io.ballerina.flowmodelgenerator.core.model.properties.NodeProperties;
import io.ballerina.flowmodelgenerator.core.model.properties.Return;
import io.ballerina.tools.text.LineRange;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a node in the flow model.
 *
 * @param id             unique identifier of the node
 * @param label          label of the node
 * @param lineRange      line range of the node
 * @param kind           kind of the node
 * @param returning      whether the node is returning
 * @param fixed          whether the node is fixed
 * @param branches       branches of the node
 * @param nodeProperties properties that are specific to the node kind
 * @param flags          flags of the node
 * @since 2201.9.0
 */
public record FlowNode(String id, String label, LineRange lineRange, NodeKind kind, boolean returning, boolean fixed,
                       List<Branch> branches, NodeProperties nodeProperties, int flags) {

    public static int NODE_FLAG_CHECKED = 1 << 0;
    public static int NODE_FLAG_CHECKPANIC = 1 << 1;
    public static int NODE_FLAG_FINAL = 1 << 2;
    public static int NODE_FLAG_REMOTE = 1 << 10;
    public static int NODE_FLAG_RESOURCE = 1 << 11;

    public enum NodeKind {
        EVENT_HTTP_API,
        IF,
        HTTP_API_GET_CALL,
        HTTP_API_POST_CALL,
        RETURN,
        EXPRESSION
    }

    /**
     * Represents a builder for the flow node.
     *
     * @since 2201.9.0
     */
    public static class Builder {

        private String label;
        private LineRange lineRange;
        private NodeKind kind;
        private boolean returning;
        private boolean fixed;
        private NodeProperties nodeProperties;
        private final List<Branch> branches;
        private int flags;

        public Builder() {
            this.branches = new ArrayList<>();
            this.flags = 0;
        }

        public void label(String label) {
            this.label = label;
        }

        public void kind(NodeKind kind) {
            this.kind = kind;
        }

        public void returning(boolean returning) {
            this.returning = returning;
        }

        public void fixed(boolean fixed) {
            this.fixed = fixed;
        }

        public void nodeProperties(NodeProperties nodeProperties) {
            this.nodeProperties = nodeProperties;
        }

        public void setNode(Node node) {
            this.lineRange = node.lineRange();
        }

        public void addBranch(String label, Branch.BranchKind kind, List<FlowNode> children) {
            this.branches.add(new Branch(label, kind, children));
        }

        public void addFlag(int flag) {
            this.flags |= flag;
        }

        public boolean isDefault() {
            return this.kind == NodeKind.EXPRESSION || this.kind == null;
        }

        public FlowNode build() {
            String id = String.valueOf(Objects.hash(lineRange));
            List<Branch> outBranches = branches.isEmpty() ? null : branches;
            return new FlowNode(id, label, lineRange, kind, returning, fixed, outBranches, nodeProperties, flags);
        }
    }

    /**
     * Represents a deserializer for the flow node.
     *
     * @since 2201.9.0
     */
    public static class Deserializer implements JsonDeserializer<FlowNode> {

        @Override
        public FlowNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            FlowNode.NodeKind kind = context.deserialize(jsonObject.get("kind"), FlowNode.NodeKind.class);
            JsonElement propertiesJson = jsonObject.get("nodeProperties");

            NodeProperties properties = switch (kind) {
                case EXPRESSION -> context.deserialize(propertiesJson, DefaultExpression.class);
                case IF -> context.deserialize(propertiesJson, IfNode.class);
                case EVENT_HTTP_API -> context.deserialize(propertiesJson, HttpApiEvent.class);
                case RETURN -> context.deserialize(propertiesJson, Return.class);
                case HTTP_API_GET_CALL -> context.deserialize(propertiesJson, HttpGet.class);
                case HTTP_API_POST_CALL -> context.deserialize(propertiesJson, HttpPost.class);
            };

            return new FlowNode(
                    jsonObject.get("id").getAsString(),
                    jsonObject.get("label").getAsString(),
                    context.deserialize(jsonObject.get("lineRange"), LineRange.class),
                    kind,
                    jsonObject.get("returning").getAsBoolean(),
                    jsonObject.get("fixed").getAsBoolean(),
                    context.deserialize(jsonObject.get("branches"), new TypeToken<List<Branch>>() {}.getType()),
                    properties,
                    context.deserialize(jsonObject.get("flags"), Integer.class)
            );
        }
    }
}
