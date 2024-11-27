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

package io.ballerina.centralconnector.response;

import java.util.List;
import java.util.Map;

/**
 * Represents a response containing package information, suggestions, and highlighting details.
 *
 * @param packages     List of packages included in the response.
 * @param suggestions  List of suggestions related to the packages.
 * @param highlighting Map containing highlighting details for the packages.
 * @param count        Total number of packages.
 * @param offset       Offset for pagination.
 * @param limit        Limit for pagination.
 * @since 2.0.0
 */
public record PackageResponse(
        List<Package> packages,
        List<Object> suggestions,
        Map<String, Highlighting> highlighting,
        int count,
        int offset,
        int limit
) {

    public record Package(
            int id,
            String organization,
            String name,
            String version,
            String platform,
            String languageSpecificationVersion,
            boolean isDeprecated,
            String deprecateMessage,
            String URL,
            String balaVersion,
            String balaURL,
            String digest,
            String summary,
            String readme,
            boolean template,
            List<String> licenses,
            List<String> authors,
            String sourceCodeLocation,
            List<String> keywords,
            String ballerinaVersion,
            String icon,
            String ownerUUID,
            long createdDate,
            int pullCount,
            String visibility,
            List<String> modules,
            String balToolId,
            String graalvmCompatible
    ) { }

    public record Highlighting(
            List<String> summary,
            List<String> keywords
    ) { }
}
