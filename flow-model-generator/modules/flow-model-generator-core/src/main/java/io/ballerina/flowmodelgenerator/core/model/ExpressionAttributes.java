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

import java.util.HashMap;
import java.util.Map;

/**
 * Expression attributes used in the Ballerina libraries.
 *
 * @since 1.4.0
 */
public class ExpressionAttributes {

    private static final Map<String, Info> parameterInfoMap = new HashMap<>();

    public record Info(String key, String label, String documentation, String type, boolean dynamicType) {

    }

    public static Info get(String key) {
        return parameterInfoMap.get(key);
    }

    private static void addMapEntry(Info info) {
        parameterInfoMap.put(info.key(), info);
    }

    public static final Info HTTP_PATH = new Info("path", "Path", "HTTP Path", "string", false);
    public static final Info HTTP_HEADERS =
            new Info("headers", "Headers", "HTTP Headers", "map<string|string[]>?", false);
    public static final Info HTTP_CLIENT = new Info("client", "Client", "HTTP Client Connection", "http:Client", false);
    public static final Info HTTP_MESSAGE = new Info("message", "Message", "HTTP Post Message", "string", true);
    public static final Info HTTP_MEDIA_TYPE =
            new Info("mediaType", "Media Type", "HTTP Post Media Type", "string?", false);

    public static final Info REDIS_CLIENT =
            new Info("client", "Client", "Redis Client Connection", "redis:Client", false);
    public static final Info REDIS_KEY = new Info("key", "Key", "Redis Key", "string", false);
    public static final Info REDIS_VALUE = new Info("value", "Value", "Redis Value", "string", false);

    static {
        addMapEntry(HTTP_PATH);
        addMapEntry(HTTP_HEADERS);
        addMapEntry(HTTP_CLIENT);
        addMapEntry(HTTP_MESSAGE);
        addMapEntry(HTTP_MEDIA_TYPE);
    }
}
