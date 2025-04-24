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

import io.ballerina.servicemodelgenerator.extension.diagnostics.ServiceValidator;
import io.ballerina.servicemodelgenerator.extension.model.Diagnostics;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ServiceValidatorTest {

    @Test
    public void testHttpServiceBasePath() {
        String path = "/hello";
        Value value = new Value.ValueBuilder().value(path).build();
        boolean isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertTrue(isValid);

        path = "/api/hello";
        value = new Value.ValueBuilder().value(path).build();
        isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertTrue(isValid);

        path = "/api/v2\\.1";
        value = new Value.ValueBuilder().value(path).build();
        isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertTrue(isValid);

        path = "/'from";
        value = new Value.ValueBuilder().value(path).build();
        isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertTrue(isValid);

        path = "/api/hello\\-world";
        value = new Value.ValueBuilder().value(path).build();
        isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertTrue(isValid);

        path = "/api/hello\\ world";
        value = new Value.ValueBuilder().value(path).build();
        isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertTrue(isValid);

        path = "\"\"";
        value = new Value.ValueBuilder().value(path).build();
        isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertTrue(isValid);
    }

    @Test
    public void testUsageOfReservedKeyword() {
        String path = "/from";
        Value value = new Value.ValueBuilder().value(path).build();
        boolean isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertFalse(isValid);
        Diagnostics diagnostics = value.getDiagnostics();
        Assert.assertEquals(diagnostics.diagnostics().size(), 1);
        Assert.assertEquals(diagnostics.diagnostics().getFirst().message(), "usage of reserved keyword: 'from'");
    }

    @Test
    public void testEmptyBasePath() {
        String path = "";
        Value value = new Value.ValueBuilder().value(path).build();
        boolean isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertFalse(isValid);
        Diagnostics diagnostics = value.getDiagnostics();
        Assert.assertEquals(diagnostics.diagnostics().size(), 1);
        Assert.assertEquals(diagnostics.diagnostics().getFirst().message(), "base path cannot be empty");
    }

    @Test
    public void testBasePathStartWithoutSlash() {
        String path = "foo";
        Value value = new Value.ValueBuilder().value(path).build();
        boolean isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertFalse(isValid);
        Diagnostics diagnostics = value.getDiagnostics();
        Assert.assertEquals(diagnostics.diagnostics().size(), 1);
        Assert.assertEquals(diagnostics.diagnostics().getFirst().message(), "base path should start with '/'");
    }

    @Test
    public void testInvalidStringLiteral() {
        String path = "\"";
        Value value = new Value.ValueBuilder().value(path).build();
        boolean isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertFalse(isValid);
        Diagnostics diagnostics = value.getDiagnostics();
        Assert.assertEquals(diagnostics.diagnostics().size(), 1);
        Assert.assertEquals(diagnostics.diagnostics().getFirst().message(), "base path should end with '\"'");

        path = "\"hello";
        value = new Value.ValueBuilder().value(path).build();
        isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertFalse(isValid);
        diagnostics = value.getDiagnostics();
        Assert.assertEquals(diagnostics.diagnostics().size(), 1);
        Assert.assertEquals(diagnostics.diagnostics().getFirst().message(), "base path should end with '\"'");

        path = "\"he\"llo\"";
        value = new Value.ValueBuilder().value(path).build();
        isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertFalse(isValid);
        diagnostics = value.getDiagnostics();
        Assert.assertEquals(diagnostics.diagnostics().size(), 1);
        Assert.assertEquals(diagnostics.diagnostics().getFirst().message(),
                "base path should not contain unescaped double quotes");

        path = "\"he\\ llo\"";
        value = new Value.ValueBuilder().value(path).build();
        isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertFalse(isValid);
        diagnostics = value.getDiagnostics();
        Assert.assertEquals(diagnostics.diagnostics().size(), 1);
        Assert.assertEquals(diagnostics.diagnostics().getFirst().message(),
                "double quote should be followed by an escape character");

        path = "\"he\\\"";
        value = new Value.ValueBuilder().value(path).build();
        isValid = ServiceValidator.validHttpBasePath(value, "http");
        Assert.assertFalse(isValid);
        diagnostics = value.getDiagnostics();
        Assert.assertEquals(diagnostics.diagnostics().size(), 1);
        Assert.assertEquals(diagnostics.diagnostics().getFirst().message(),
                "double quote should be followed by an escape character");
    }
}
