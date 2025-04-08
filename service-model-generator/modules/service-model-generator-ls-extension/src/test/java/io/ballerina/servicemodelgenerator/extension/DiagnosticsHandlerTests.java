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

import io.ballerina.servicemodelgenerator.extension.diagnostics.DiagnosticsHandler;
import io.ballerina.servicemodelgenerator.extension.model.Diagnostics;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test class to validate util methods in the DiagnosticsHandler.
 *
 * @since 2.3.0
 */
public class DiagnosticsHandlerTests {

    @Test
    public void testEmptyResourcePaths() {
        String resourcePath = "";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = DiagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "path cannot be empty");
    }

    @Test
    public void testResourcePathsWithTwoSlashes() {
        String resourcePath = "foo//bar";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = DiagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "Resource path contains invalid characters");
    }

    @Test
    public void testResourcePathsWithInvalidCharacters() {
        String resourcePath = "path/to/resource@";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = DiagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "Invalid character: @");
    }

    @Test
    public void testResourcePathsWithHyphen() {
        String resourcePath = "user-name";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = DiagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "Invalid character: -");
    }

    @Test
    public void testResourcePathsWithSpaces() {
        String resourcePath = "user name";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = DiagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "Invalid segment: user name");
    }

    @Test
    public void testResourcePathsStartWithSlash() {
        String resourcePath = "/user";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = DiagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "Resource path contains invalid characters");
    }

    @Test
    public void testResourcePathsWithValidPath() {
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = DiagnosticsHandler.validateResourcePath("user/path", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = DiagnosticsHandler.validateResourcePath("[string name]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = DiagnosticsHandler.validateResourcePath("[string|int id]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = DiagnosticsHandler.validateResourcePath("[string... name]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = DiagnosticsHandler.validateResourcePath("[string...]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = DiagnosticsHandler.validateResourcePath("[123]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = DiagnosticsHandler.validateResourcePath("[12.12]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = DiagnosticsHandler.validateResourcePath("[true]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = DiagnosticsHandler.validateResourcePath("[\"true\"]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);
    }
}
