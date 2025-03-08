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

package io.ballerina.flowmodelgenerator.extension.request;

import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.PropertyTypeMemberInfo;

import java.util.List;

/**
 * A request to find a matching type for the given expression.
 *
 * @param filePath Project path
 * @param typeMembers Type members
 * @param expr Expression to find the type
 *
 * @since 2.0.0
 */
public record FindTypeRequest(String filePath, List<PropertyTypeMemberInfo> typeMembers, String expr) {

    public record TypePackageInfo(String org, String module, String version) {

        public static TypePackageInfo from(String packageInfo) {
            if (packageInfo.isEmpty()) {
                return new TypePackageInfo(null, null, null);
            }
            String[] parts = packageInfo.split(":");
            return new TypePackageInfo(parts[0], parts[1], parts[2]);
        }

        public static TypePackageInfo from(Codedata codedata) {
            return new TypePackageInfo(codedata.org(), codedata.module(), codedata.version());
        }
    }
}
