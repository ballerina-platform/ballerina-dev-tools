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

import java.util.ArrayList;
import java.util.List;

/**
 * Node attributes used in the Ballerina libraries.
 *
 * @since 1.4.0
 */
public class NodeAttributes {

    private static final List<Info> nodeInfoMap = new ArrayList<>();

    public record Info(String key, String method, String label, FlowNode.Kind kind,
                       ExpressionAttributes.Info callExpression, List<ExpressionAttributes.Info> parameterExpressions) {

    }

    public static Info getByKey(String library, String method) {
        String key = library + "-" + method;
        return nodeInfoMap.stream()
                .filter(info -> info.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    public static Info getByLabel(String label) {
        return nodeInfoMap.stream()
                .filter(info -> info.label().equals(label))
                .findFirst()
                .orElse(null);
    }

    public static final Info HTTP_GET =
            new Info("http-get", "get", "HTTP GET", FlowNode.Kind.ACTION_CALL, ExpressionAttributes.HTTP_CLIENT,
                    List.of(ExpressionAttributes.HTTP_PATH, ExpressionAttributes.HTTP_HEADERS));
    public static final Info HTTP_POST =
            new Info("http-post", "post", "HTTP POST", FlowNode.Kind.ACTION_CALL, ExpressionAttributes.HTTP_CLIENT,
                    List.of(ExpressionAttributes.HTTP_PATH, ExpressionAttributes.HTTP_MESSAGE,
                            ExpressionAttributes.HTTP_HEADERS, ExpressionAttributes.HTTP_MEDIA_TYPE));

    public static final Info REDIS_GET =
            new Info("redis-get", "get", "Redis Get", FlowNode.Kind.ACTION_CALL, ExpressionAttributes.REDIS_CLIENT,
                    List.of(ExpressionAttributes.REDIS_KEY));
    public static final Info REDIS_SET =
            new Info("redis-set", "set", "Redis Set", FlowNode.Kind.ACTION_CALL, ExpressionAttributes.REDIS_CLIENT,
                    List.of(ExpressionAttributes.REDIS_KEY, ExpressionAttributes.REDIS_VALUE));

    static {
        nodeInfoMap.add(HTTP_GET);
        nodeInfoMap.add(HTTP_POST);
        nodeInfoMap.add(REDIS_GET);
        nodeInfoMap.add(REDIS_SET);
    }
}
