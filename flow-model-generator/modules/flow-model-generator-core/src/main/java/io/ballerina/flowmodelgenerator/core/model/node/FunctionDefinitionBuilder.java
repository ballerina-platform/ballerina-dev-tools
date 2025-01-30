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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.ballerinalang.model.types.TypeKind;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the properties of a function definition node.
 *
 * @since 2.0.0
 */
public class FunctionDefinitionBuilder extends NodeBuilder {

    public static final String LABEL = "Function Definition";
    public static final String DESCRIPTION = "Define a function";

    public static final String PARAMETERS_KEY = "parameters";
    public static final String PARAMETERS_LABEL = "Parameters";
    public static final String PARAMETERS_DOC = "Function parameters";

    public static final String PARAMETER_LABEL = "Parameter";
    public static final String PARAMETER_DOC = "Function parameter";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.FUNCTION_DEFINITION);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties()
                .functionName(null)
                .returnType(null)
                .nestedProperty()
                .endNestedProperty(Property.ValueType.REPEATABLE_PROPERTY, PARAMETERS_KEY,
                        PARAMETERS_LABEL, PARAMETERS_DOC, ParameterSchemaHolder.PARAMETER_SCHEMA);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        return null;
    }

    private static class ParameterSchemaHolder {

        private static final Property PARAMETER_SCHEMA = initParameterSchema();

        private static Property initParameterSchema() {
            Property.Builder<?> propertyBuilder = new Property.Builder<>(null);

            // Build the type property
            propertyBuilder
                    .metadata()
                        .label(Property.TYPE_LABEL)
                        .description(Property.TYPE_DOC)
                        .stepOut()
                    .type(Property.ValueType.TYPE)
                    .typeConstraint(TypeKind.ANYDATA.typeName())
                    .value("")
                    .editable();
            Property typeProperty = propertyBuilder.build();

            // Build the data property
            propertyBuilder = new Property.Builder<>(null);
            propertyBuilder
                    .metadata()
                        .label(Property.VARIABLE_NAME)
                        .description(Property.VARIABLE_DOC)
                        .stepOut()
                    .type(Property.ValueType.IDENTIFIER)
                    .value("")
                    .editable();
            Property dataProperty = propertyBuilder.build();

            // Build the node properties
            Map<String, Property> nodeProperties = new HashMap<>();
            nodeProperties.put(Property.TYPE_KEY, typeProperty);
            nodeProperties.put(Property.VARIABLE_KEY, dataProperty);

            // Build the property schema
            propertyBuilder = new Property.Builder<>(null);
            propertyBuilder
                    .metadata()
                        .label(PARAMETER_LABEL)
                        .description(PARAMETER_DOC)
                        .stepOut()
                    .type(Property.ValueType.FIXED_PROPERTY)
                    .editable()
                    .value(nodeProperties);

            return propertyBuilder.build();
        }
    }
}
