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

/**
 * Represents a response containing a list of symbols.
 *
 * @param symbols the list of symbols in the response
 * @param count   the total number of symbols
 * @param offset  the offset for pagination
 * @param limit   the limit for pagination
 * @since 2.0.0
 */
public record SymbolResponse(
        List<Symbol> symbols,
        int count,
        int offset,
        int limit
) {

    public record Symbol(
            String id,
            String packageID,
            String name,
            String organization,
            String version,
            long createdDate,
            String icon,
            String symbolType,
            String symbolParent,
            String symbolName,
            String description,
            String symbolSignature,
            boolean isIsolated,
            boolean isRemote,
            boolean isResource,
            boolean isClosed,
            boolean isDistinct,
            boolean isReadOnly
    ) { }
}

