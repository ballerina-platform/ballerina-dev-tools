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

/**
 * ServiceClass class to hold the service class model.
 *
 * @param id service class id
 * @param name service class name
 * @param type service class type
 * @param properties service class properties
 * @param codedata service class codedata
 * @param functions service class functions
 * @param fields service class fields
 *
 * @since 2.0.0
 */
public record ServiceClass(String id, String name, String type, Map<String, Value> properties,
                          Codedata codedata, List<Function> functions, List<Field> fields) {

    public static class ServiceClassBuilder {
        private String name;
        private String type;
        private Map<String, Value> properties;
        private Codedata codedata;
        private List<Function> functions;
        private List<Field> fields;

        public ServiceClassBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ServiceClassBuilder type(String type) {
            this.type = type;
            return this;
        }

        public ServiceClassBuilder properties(Map<String, Value> properties) {
            this.properties = properties;
            return this;
        }

        public ServiceClassBuilder codedata(Codedata codedata) {
            this.codedata = codedata;
            return this;
        }

        public ServiceClassBuilder functions(List<Function> functions) {
            this.functions = functions;
            return this;
        }

        public ServiceClassBuilder fields(List<Field> fields) {
            this.fields = fields;
            return this;
        }

        public ServiceClass build() {
            String id = "0";
            return new ServiceClass(id, name, type, properties, codedata, functions, fields);
        }
    }
}
