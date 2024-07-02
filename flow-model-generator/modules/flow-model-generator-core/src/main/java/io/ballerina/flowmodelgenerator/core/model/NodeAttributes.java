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

    public record Info(String key, String label, FlowNode.Kind kind, ExpressionAttributes.Info callExpression,
                       List<ExpressionAttributes.Info> parameterExpressions) {

    }

    public static Info get(String key) {
        return nodeInfoMap.stream()
                .filter(info -> info.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    public static Info get(FlowNode.Kind kind) {
        return nodeInfoMap.stream()
                .filter(info -> info.kind().equals(kind))
                .findFirst()
                .orElse(null);
    }

    public static final Info HTTP_GET =
            new Info("get", "HTTP GET", FlowNode.Kind.HTTP_API_GET_CALL, ExpressionAttributes.HTTP_CLIENT,
                    List.of(ExpressionAttributes.HTTP_PATH, ExpressionAttributes.HTTP_HEADERS));
    public static final Info HTTP_POST =
            new Info("post", "HTTP POST", FlowNode.Kind.HTTP_API_POST_CALL, ExpressionAttributes.HTTP_CLIENT,
                    List.of(ExpressionAttributes.HTTP_PATH, ExpressionAttributes.HTTP_MESSAGE,
                            ExpressionAttributes.HTTP_HEADERS, ExpressionAttributes.HTTP_MEDIA_TYPE));

    static {
        nodeInfoMap.add(HTTP_GET);
        nodeInfoMap.add(HTTP_POST);
    }
}
