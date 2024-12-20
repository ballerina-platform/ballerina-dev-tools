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

package io.ballerina.servicemodelgenerator.extension.model;

public class HttpResponse {
    private Value statusCode;
    private Value body;
    private Value name;
    private Value createStatusCodeResponse;

    public HttpResponse() {
        this(null, null, null, null);
    }

    public HttpResponse(Value statusCode, Value body, Value name, Value createStatusCodeResponse) {
        this.statusCode = statusCode;
        this.body = body;
        this.name = name;
        this.createStatusCodeResponse = createStatusCodeResponse;
    }

    public HttpResponse(String statusCode, String body, String name) {
        this.statusCode = new Value(statusCode, "EXPRESSION", true);
        this.body = new Value(body, "EXPRESSION", true);
        this.name = new Value(name, "EXPRESSION", true);
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

    public Value isCreateStatusCodeResponse() {
        return createStatusCodeResponse;
    }

    public void setCreateStatusCodeResponse(Value createStatusCodeResponse) {
        this.createStatusCodeResponse = createStatusCodeResponse;
    }
}
