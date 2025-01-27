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

package io.ballerina.servicemodelgenerator.extension.response;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public record ListenerDiscoveryResponse(boolean hasListeners, Set<String> listeners, String errorMsg,
                                        String stacktrace) {

    public ListenerDiscoveryResponse() {
        this(false, new HashSet<>(), null, null);
    }

    public ListenerDiscoveryResponse(Set<String> listeners) {
        this(!listeners.isEmpty(), listeners, null, null);
    }

    public ListenerDiscoveryResponse(Throwable e) {
        this(false, new HashSet<>(), e.toString(), Arrays.toString(e.getStackTrace()));
    }
}
