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

package io.ballerina.testmanagerservice.extension;

/**
 * Constants for the test manager service.
 *
 * @since 2.0.0
 */
public class Constants {

    public static final String CAPABILITY_NAME = "testManagerService";

    public static final String ORG_BALLERINA = "ballerina";
    public static final String MODULE_TEST = "test";
    public static final String IMPORT_TEST_STMT = "import ballerina/test;";
    public static final String FILED_TEMPLATE = "%s: %s";

    public static final String TEST_ANNOTATION = "@test:";
    public static final String CONFIG_GROUPS = "groups";
    public static final String CONFIG_ENABLED = "enabled";

    public static final String OPEN_CURLY_BRACE = "{";
    public static final String CLOSE_CURLY_BRACE = "}";
    public static final String OPEN_BRACKET = "[";
    public static final String CLOSE_BRACKET = "]";
    public static final String OPEN_PARAM = "(";
    public static final String CLOSED_PARAM = ")";
    public static final String COMMA = ",";
    public static final String SPACE = " ";
    public static final String EQUAL = "=";
    public static final String COLON = ":";

    public static final String KEYWORD_FUNCTION = "function";
    public static final String KEYWORD_RETURNS = "returns";
    public static final String KEYWORD_DO = "do";
    public static final String FALSE = "false";
    public static final String TRUE = "true";

    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final String TAB_SEPARATOR = "\t";

    public static final String ON_FAIL_ERROR_STMT = "on fail error err";

    private Constants() {
    }
}
