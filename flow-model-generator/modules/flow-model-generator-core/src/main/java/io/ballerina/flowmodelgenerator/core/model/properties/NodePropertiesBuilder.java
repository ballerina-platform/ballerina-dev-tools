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

package io.ballerina.flowmodelgenerator.core.model.properties;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.model.Expression;

/**
 * Represents builder for the node properties.
 *
 * @since 2201.9.0
 */
public abstract class NodePropertiesBuilder {

    protected final SemanticModel semanticModel;
    protected Expression.Builder expressionBuilder;

    public NodePropertiesBuilder(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.expressionBuilder = new Expression.Builder();
    }

    /**
     * Builds the node properties.
     *
     * @return node properties
     */
    public abstract NodeProperties build();
}
