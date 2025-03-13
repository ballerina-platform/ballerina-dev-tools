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

package io.ballerina.flowmodelgenerator.core;

/**
 * Constants used in the flow model generator.
 *
 * @since 2.0.0
 */
public class Constants {

    // Check keywords
    public static final String CHECK = "check";
    public static final String CHECKPANIC = "checkpanic";

    public static final String MAIN_FUNCTION_NAME = "main";

    // Constants used for Natural functions
    public static final class NaturalFunctions {
        private NaturalFunctions() {}

        public static final String PROMPT = "prompt";
        public static final String PROMPT_TYPE = "np:Prompt";
        public static final String CONTEXT = "context";
        public static final String CONTEXT_TYPE = "np:Context";
        public static final String BALLERINAX_ORG = "ballerinax";
        public static final String NP_PACKAGE = "np";
        public static final String NP_PACKAGE_WITH_ORG = BALLERINAX_ORG + "/" + NP_PACKAGE;
        public static final String ICON =
                "https://gist.github.com/user-attachments/assets/903c5c16-7d67-4af8-8113-ce7c59ccdaab";
    }
}
