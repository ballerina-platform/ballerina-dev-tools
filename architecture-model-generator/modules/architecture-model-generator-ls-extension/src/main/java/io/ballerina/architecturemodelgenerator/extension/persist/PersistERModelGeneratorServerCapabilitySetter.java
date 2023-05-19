/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.architecturemodelgenerator.extension.persist;

import io.ballerina.architecturemodelgenerator.extension.Constants;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.registration.BallerinaServerCapabilitySetter;

import java.util.Optional;

/**
 * Capability setter for the {@link PersistERModelGeneratorService}.
 *
 * @since 2201.6.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.registration.BallerinaServerCapabilitySetter")
public class PersistERModelGeneratorServerCapabilitySetter extends
        BallerinaServerCapabilitySetter<PersistERModelGeneratorServerCapabilities> {

    @Override
    public Optional<PersistERModelGeneratorServerCapabilities> build() {

        PersistERModelGeneratorServerCapabilities capabilities = new PersistERModelGeneratorServerCapabilities();
        capabilities.setGetPersistERModel(true);
        return Optional.of(capabilities);
    }

    @Override
    public String getCapabilityName() {
        return Constants.PERSIST_MODEL_GEN_CAPABILITY_NAME;
    }

    @Override
    public Class<PersistERModelGeneratorServerCapabilities> getCapability() {
        return PersistERModelGeneratorServerCapabilities.class;
    }
}
