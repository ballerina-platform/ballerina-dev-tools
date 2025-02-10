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

package io.ballerina.servicemodelgenerator.extension;

import io.ballerina.servicemodelgenerator.extension.model.MetaData;

/**
 * Represents constants for the trigger model generator service.
 *
 * @since 2.0.0
 */
public class ServiceModelGeneratorConstants {

    public static final String CAPABILITY_NAME = "serviceModel";
    public static final String SPACE = " ";
    public static final String OPEN_BRACE = "{";
    public static final String CLOSE_BRACE = "}";
    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final String IMPORT_STMT_TEMPLATE = "%nimport %s/%s;%n";

    public static final String SINGLE_SELECT_VALUE = "SINGLE_SELECT";
    public static final String MULTIPLE_SELECT_VALUE = "MULTIPLE_SELECT";

    public static final String HTTP_DEFAULT_LISTENER_STMT =
            "listener http:Listener httpDefaultListener = http:getDefaultListener();" + LINE_SEPARATOR;
    public static final String HTTP_DEFAULT_LISTENER = "Default Listener";
    public static final String HTTP_DEFAULT_LISTENER_VAR_NAME = "httpDefaultListener";

    public static final String KAFKA = "kafka";
    public static final String HTTP = "http";
    public static final String GRAPHQL = "graphql";

    public static final String PROPERTY_REQUIRED_FUNCTIONS = "requiredFunctions";
    public static final String PROPERTY_DESIGN_APPROACH = "designApproach";
    public static final String PROPERTY_NAME = "name";

    public static final String KIND_QUERY = "QUERY";
    public static final String KIND_MUTATION = "MUTATION";
    public static final String KIND_SUBSCRIPTION = "SUBSCRIPTION";
    public static final String KIND_RESOURCE = "RESOURCE";
    public static final String KIND_REMOTE = "REMOTE";
    public static final String KIND_DEFAULT = "DEFAULT";
    public static final String KIND_REQUIRED = "REQUIRED";
    public static final String KIND_DEFAULTABLE = "DEFAULTABLE";

    public static final String PARAMETER = "parameter";
    public static final String SERVICE = "service";
    public static final String RESOURCE = "resource";
    public static final String REMOTE = "remote";
    public static final String BASE_PATH = "basePath";
    public static final String ON = "on";
    public static final String SUBSCRIBE = "subscribe";
    public static final String GET = "get";

    public static final String VALUE_TYPE_EXPRESSION = "EXPRESSION";
    public static final String VALUE_TYPE_IDENTIFIER = "IDENTIFIER";
    public static final String VALUE_TYPE_TYPE = "TYPE";
    public static final String HTTP_PARAM_TYPE_QUERY = "QUERY";

    public static final String TYPE_HTTP_SERVICE_CONFIG = "http:ServiceConfig";

    public static final MetaData PARAMETER_TYPE_METADATA = new MetaData("Parameter Type",
            "The type of the parameter");
    public static final MetaData PARAMETER_NAME_METADATA = new MetaData("Parameter Name",
            "The name of the parameter");
    public static final MetaData PARAMETER_DEFAULT_VALUE_METADATA = new MetaData("Default Value",
            "The default value of the parameter");
    public static final MetaData FUNCTION_RETURN_TYPE_METADATA = new MetaData("Return Type",
            "The return type of the function");
    public static final MetaData FUNCTION_NAME_METADATA = new MetaData("Function Name",
            "The name of the function");
    public static final MetaData FUNCTION_ACCESSOR_METADATA = new MetaData("Accessor",
            "The accessor of the function");

    private ServiceModelGeneratorConstants() {
    }
}
