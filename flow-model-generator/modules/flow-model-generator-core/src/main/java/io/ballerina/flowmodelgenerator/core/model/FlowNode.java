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
import io.ballerina.flowmodelgenerator.core.model.node.ActionCall;
import io.ballerina.flowmodelgenerator.core.model.node.Break;
import io.ballerina.flowmodelgenerator.core.model.node.Continue;
import io.ballerina.flowmodelgenerator.core.model.node.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.node.ErrorHandler;
import io.ballerina.flowmodelgenerator.core.model.node.Fail;
import io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent;
import io.ballerina.flowmodelgenerator.core.model.node.If;
import io.ballerina.flowmodelgenerator.core.model.node.Lock;
import io.ballerina.flowmodelgenerator.core.model.node.Panic;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.Start;
import io.ballerina.flowmodelgenerator.core.model.node.Transaction;
import io.ballerina.flowmodelgenerator.core.model.node.While;
import io.ballerina.tools.text.LineRange;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Represents a node in the flow model.
 *
 * @since 1.4.0
 */
public record FlowNode(
        String id,
        Metadata metadata,
        Codedata codedata,
        LineRange lineRange,
        boolean returning,
        List<Branch> branches,
        Map<String, Property> properties,
        int flags
) {

    public Property getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }

    public Branch getBranch(String label) {
        return branches.stream().filter(branch -> branch.label().equals(label)).findFirst().orElse(null);
    }

    public LineRange lineRange() {
        return lineRange;
    }

    public boolean hasFlag(int flag) {
        return (flags & flag) == flag;
    }

    public boolean returning() {
        return returning;
    }

    public static final int NODE_FLAG_CHECKED = 1 << 0;
    public static final int NODE_FLAG_CHECKPANIC = 1 << 1;
    public static final int NODE_FLAG_FINAL = 1 << 2;
    public static final int NODE_FLAG_REMOTE = 1 << 10;
    public static final int NODE_FLAG_RESOURCE = 1 << 11;

    public enum Kind {
        EVENT_HTTP_API,
        IF,
        ACTION_CALL,
        RETURN,
        EXPRESSION,
        ERROR_HANDLER,
        WHILE,
        CONTINUE,
        BREAK,
        PANIC,
        START,
        TRANSACTION,
        LOCK,
        FAIL
    }

    /**
     * Represents a deserializer for the flow node.
     *
     * @since 1.4.0
     */
    public static class Deserializer implements JsonDeserializer<FlowNode> {

        @Override
        public FlowNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            FlowNode.Kind kind = context.deserialize(jsonObject.get("kind"), FlowNode.Kind.class);

            return switch (kind) {
                case EXPRESSION -> context.deserialize(jsonObject, DefaultExpression.class);
                case IF -> context.deserialize(jsonObject, If.class);
                case EVENT_HTTP_API -> context.deserialize(jsonObject, HttpApiEvent.class);
                case ACTION_CALL -> context.deserialize(jsonObject, ActionCall.class);
                case RETURN -> context.deserialize(jsonObject, Return.class);
                case ERROR_HANDLER -> context.deserialize(jsonObject, ErrorHandler.class);
                case WHILE -> context.deserialize(jsonObject, While.class);
                case CONTINUE -> context.deserialize(jsonObject, Continue.class);
                case BREAK -> context.deserialize(jsonObject, Break.class);
                case PANIC -> context.deserialize(jsonObject, Panic.class);
                case START -> context.deserialize(jsonObject, Start.class);
                case FAIL -> context.deserialize(jsonObject, Fail.class);
                case TRANSACTION -> context.deserialize(jsonObject, Transaction.class);
                case LOCK -> context.deserialize(jsonObject, Lock.class);
            };
        }
    }
}
