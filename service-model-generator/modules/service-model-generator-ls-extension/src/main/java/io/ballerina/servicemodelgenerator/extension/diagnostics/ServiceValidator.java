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

package io.ballerina.servicemodelgenerator.extension.diagnostics;

import io.ballerina.servicemodelgenerator.extension.model.Diagnostics;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Validate the service adding and updating.
 *
 * @since 2.5.0
 */
public class ServiceValidator {

    public static boolean validHttpBasePath(@NonNull Value basePath, String type) {
        String basePathValue = basePath.getValue();
        List<Diagnostics.Info> diagnostics = new ArrayList<>();

        if (basePathValue.isEmpty()) {
            diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR, "base path cannot be empty"));
            basePath.setDiagnostics(new Diagnostics(true, diagnostics));
            return false;
        }

        if (basePathValue.startsWith("\"")) {
            if (!basePathValue.endsWith("\"")) {
                diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR, "base path should end with '\"'"));
                basePath.setDiagnostics(new Diagnostics(true, diagnostics));
                return false;
            }
            String content = basePathValue.substring(1, basePathValue.length() - 1);
            // I need to iterate over the string and if there is double quote without escape, then it is invalid
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '\\') {
                    // get the next char if exist and report is its not a double quote
                    if (i + 1 < content.length()) {
                        char nextChar = content.charAt(i + 1);
                        if (nextChar != '\"') {
                            diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR,
                                    "double quote should be followed by an escape character"));
                            basePath.setDiagnostics(new Diagnostics(true, diagnostics));
                            return false;
                        }
                        i++;
                    } else {
                        diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR,
                                "double quote should be followed by an escape character"));
                        basePath.setDiagnostics(new Diagnostics(true, diagnostics));
                        return false;
                    }
                    continue;
                }
                if (c == '\"') {
                    diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR,
                            "base path should not contain unescaped double quotes"));
                    basePath.setDiagnostics(new Diagnostics(true, diagnostics));
                    return false;
                }
            }
            return true;
        }

        if (!basePathValue.startsWith("/")) {
            diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR, "base path should start with '/'"));
            basePath.setDiagnostics(new Diagnostics(true, diagnostics));
            return false;
        }

        IdentifierValidator validator = new IdentifierValidator();
        List<ResourcePathParser.SegmentPart> segments = ResourcePathParser.splitSegments(
                basePathValue.substring(1));
        for (ResourcePathParser.SegmentPart segment : segments) {
            String value = segment.value();
            if (!ResourceFunctionFormValidator.validIdentifier(validator, value, diagnostics)) {
                basePath.setDiagnostics(new Diagnostics(true, diagnostics));
                return false;
            }
        }
        return true;
    }
}
