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

public class HttpResponse {
    private Value statusCode = null;
    private Value body = null;
    private Value name = null;
    private Value type = null;
    private boolean enabled = false;
    private boolean editable = false;
    private boolean advanced = false;

    public HttpResponse() {
    }

    public HttpResponse(Value statusCode, Value body, Value name, Value type) {
        this.statusCode = statusCode;
        this.body = body;
        this.name = name;
        this.type = type;
    }

    public HttpResponse(String statusCode, String body) {
        this.statusCode = new Value(statusCode, "EXPRESSION", true);
        this.body = new Value(body, "EXPRESSION", true);
    }

    public HttpResponse(String type) {
        this.type = new Value(type, "EXPRESSION", true);
    }

    public HttpResponse(String statusCode, String body, String name) {
        this.statusCode = new Value(statusCode, "EXPRESSION", true);
        this.body = new Value(body, "EXPRESSION", true);
        this.name = new Value(name, "EXPRESSION", true);
    }

    public HttpResponse(String statusCode, String body, String name, String type) {
        this.statusCode = new Value(statusCode, "EXPRESSION", true);
        this.body = new Value(body, "EXPRESSION", true);
        this.name = new Value(name, "EXPRESSION", true);
        this.type = new Value(name, "EXPRESSION", true);
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
}
