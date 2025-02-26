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

import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FunctionReturnType extends Value {
    private List<HttpResponse> responses;
    private Map<String, HttpResponse> schema;

    public FunctionReturnType(Value value) {
        super(value.getMetadata(), value.isEnabled(), value.isEditable(), value.getValue(), value.getValueType(),
                value.getValueTypeConstraint(), value.isType(), value.getPlaceholder(), value.isOptional(),
                value.isAdvanced(), value.getProperties(), value.getItems(), value.getCodedata());
    }

    public FunctionReturnType() {
        this(ServiceModelGeneratorConstants.FUNCTION_RETURN_TYPE_METADATA, false, false, null,
                null, null, false, null, false, false,
                null, null, null, null, null);
    }

    public FunctionReturnType(MetaData metadata, boolean enabled, boolean editable, String value, String valueType,
                              String valueTypeConstraint, boolean isType, String placeholder, boolean optional,
                              boolean advanced, Map<String, Value> properties, List<String> items, Codedata codedata,
                              List<HttpResponse> responses, Map<String, HttpResponse> schema) {
        super(metadata, enabled, editable, value, valueType, valueTypeConstraint, isType, placeholder, optional,
                advanced, properties, items, codedata);
        this.responses = responses;
        this.schema = schema;
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

    @Override
    public boolean equals(Object obj) {
        if (Objects.isNull(obj) || !(obj instanceof FunctionReturnType functionReturnType)) {
            return false;
        }
        return super.equals(functionReturnType);
    }
}
