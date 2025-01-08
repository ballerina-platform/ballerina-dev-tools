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

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Represents the metadata of a diagram component.
 *
 * @param label        The label of the component
 * @param description  The description of the component
 * @param keywords     The keywords of the component
 * @param icon         The icon of the component
 * @param functionKind The kind of the function
 * @param data         The additional data
 * @since 2.0.0
 */
public record Metadata(String label, String description, List<String> keywords, String icon, String functionKind,
                       Map<String, Object> data) {

    public static class Builder<T> extends FacetedBuilder<T> {

        private String label;
        private String description;
        private List<String> keywords;
        private String icon;
        private String functionKind;
        private Map<String, Object> data;

        public Builder(T parentBuilder) {
            super(parentBuilder);
        }

        public Builder<T> label(String label) {
            this.label = label;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> description(String format, Object... args) {
            Object[] preprocessedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                preprocessedArgs[i] = (arg instanceof Node) ? CommonUtils.getVariableName((Node) arg) : arg;
            }
            this.description = String.format(format, preprocessedArgs);
            return this;
        }

        public Builder<T> keywords(List<String> keywords) {
            this.keywords = keywords;
            return this;
        }

        public Builder<T> icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder<T> functionKind(String functionKind) {
            this.functionKind = functionKind;
            return this;
        }

        public Builder<T> data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public Builder<T> addData(String key, Object value) {
            if (data == null) {
                data = Map.of(key, value);
            } else {
                data.put(key, value);
            }
            return this;
        }

        public Metadata build() {
            return new Metadata(label, description, keywords, icon, functionKind, data);
        }
    }
}
