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

package io.ballerina.servicemodelgenerator.extension.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the return type of function.
 *
 * @since 2.0.0
 */
public class FunctionReturnType extends Value {
    private List<HttpResponse> responses;
    private Map<String, HttpResponse> schema;
    private boolean hasError;

    public FunctionReturnType(Value value) {
        super(value);
    }

    public FunctionReturnType(MetaData metaData) {
        super(new Value.ValueBuilder().setMetadata(metaData).enabled(true).editable(true).build());
    }

    public List<HttpResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<HttpResponse> responses) {
        this.responses = responses;
    }

    public Map<String, HttpResponse> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, HttpResponse> schema) {
        this.schema = schema;
    }

    public int hashCode() {
        return Objects.hash(super.hashCode(), responses, schema);
    }

    public boolean hasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }
}
