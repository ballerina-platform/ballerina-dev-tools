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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.flowmodelgenerator.core.central.*;
import io.ballerina.flowmodelgenerator.core.model.*;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.PositionUtil;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.*;

/**
 * Generates functions based on a given keyword.
 *
 * @since 1.4.0
 */
public class ConfigurableVariablesGenerator {

    private final Gson gson;
    private final String filePath;
    private final String variable;
    private final String type;
    private final String value;
    public static final String CONFIG_BAL = "Config.bal";
    public static final String CONFIG_TOML = "Config.toml";

    public ConfigurableVariablesGenerator(String filePath, String variable, String type, String value) {
        this.gson = new Gson();
        this.filePath = filePath;
        this.variable = variable;
        this.type = type;
        this.value = value;
    }

    public JsonElement textEditsToAddConfigurableVariables() {

//        TextEdit textEdit = new TextEdit(CommonUtils.toRange(lineRange), "");
//        textEdits.add(textEdit);
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
//        textEditsMap.put(filePath, textEdits);
        return gson.toJsonTree(textEditsMap);
    }
}
