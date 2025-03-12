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

/**
 * Represents a HTTP response.
 *
 * @since 2.0.0
 */
public class HttpResponse {
    private Value statusCode;
    private Value body;
    private Value name;
    private Value type;
    private Value headers;
    private boolean enabled = true;
    private boolean editable = false;
    private boolean advanced = false;
    private boolean isHttpResponseType = false;

    public HttpResponse() {
    }

    public HttpResponse(Value statusCode, Value body, Value name, Value type, Value headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.name = name;
        this.type = type;
        this.headers = headers;
    }

    public HttpResponse(String type) {
        this.type = new Value(type, "EXPRESSION", true);
    }

    public HttpResponse(String statusCode, String body, String name) {
        this.statusCode = new Value(statusCode, "SINGLE_SELECT", true);
        this.body = new Value(body, "EXPRESSION", true);
        this.name = new Value(name, "EXPRESSION", true);
    }

    public HttpResponse(String statusCode, String type) {
        this.statusCode = new Value(statusCode, "SINGLE_SELECT", true);
        this.body = new Value(type, "TYPE", true);
        this.name = new Value("", "IDENTIFIER", true);
        this.type = new Value(type, "TYPE", true);
        this.headers = new Value("", "MULTIPLE_SELECT", true);
    }

    public HttpResponse(String statusCode, String type, boolean editable) {
        this.statusCode = new Value(statusCode, "SINGLE_SELECT", true);
        this.body = new Value("", "TYPE", true);
        this.name = new Value("", "IDENTIFIER", true);
        this.type = new Value(type, "TYPE", true);
        this.headers = new Value("", "MULTIPLE_SELECT", true);
        this.editable = editable;
    }

    public static HttpResponse getAnonResponse(String code, String typeStr) {
        Value statusCode = new Value(code, "SINGLE_SELECT", true);
        Value body = new Value("", "EXPRESSION", true);
        Value name = new Value("", "EXPRESSION", true);
        Value type = new Value(typeStr, "EXPRESSION", true);
        Value headers = new Value("", "MULTIPLE_SELECT", true);
        return new HttpResponse(statusCode, body, name, type, headers);
    }

    public Value getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Value statusCode) {
        this.statusCode = statusCode;
    }

    public Value getBody() {
        return body;
    }

    public void setBody(Value body) {
        this.body = body;
    }

    public Value getName() {
        return name;
    }

    public void setName(Value name) {
        this.name = name;
    }

    public Value getType() {
        return type;
    }

    public void setType(Value type) {
        this.type = type;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setAdvanced(boolean advanced) {
        this.advanced = advanced;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Value getHeaders() {
        return headers;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isHttpResponseType() {
        return isHttpResponseType;
    }

    public void setHttpResponseType(boolean httpResponseType) {
        isHttpResponseType = httpResponseType;
    }
}
