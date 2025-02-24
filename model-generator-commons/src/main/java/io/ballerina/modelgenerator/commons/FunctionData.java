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

package io.ballerina.modelgenerator.commons;

import java.util.Map;

/**
 * Represents the result of a function.
 *
 * @since 2.0.0
 */
public class FunctionData {

    private final int functionId;
    private final String name;
    private final String description;
    private final String returnType;
    private final String packageName;
    private final String org;
    private final String version;
    private final String resourcePath;
    private final Kind kind;
    private final boolean returnError;
    private final boolean inferredReturnType;
    private Map<String, ParameterResult> parameters;

    public FunctionData(int functionId, String name, String description, String returnType,
                        String packageName, String org, String version, String resourcePath,
                        Kind kind, boolean returnError, boolean inferredReturnType) {
        this.functionId = functionId;
        this.name = name;
        this.description = description;
        this.returnType = returnType;
        this.packageName = packageName;
        this.org = org;
        this.version = version;
        this.resourcePath = resourcePath;
        this.kind = kind;
        this.returnError = returnError;
        this.inferredReturnType = inferredReturnType;
    }

    public void setParameters(Map<String, ParameterResult> parameters) {
        this.parameters = parameters;
    }

    // Getters
    public int functionId() {
        return functionId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String returnType() {
        return returnType;
    }

    public String packageName() {
        return packageName;
    }

    public String org() {
        return org;
    }

    public String version() {
        return version;
    }

    public String resourcePath() {
        return resourcePath;
    }

    public Kind kind() {
        return kind;
    }

    public boolean returnError() {
        return returnError;
    }

    public boolean inferredReturnType() {
        return inferredReturnType;
    }

    public Map<String, ParameterResult> parameters() {
        return parameters;
    }

    public enum Kind {
        FUNCTION,
        CONNECTOR,
        REMOTE,
        RESOURCE
    }

}
