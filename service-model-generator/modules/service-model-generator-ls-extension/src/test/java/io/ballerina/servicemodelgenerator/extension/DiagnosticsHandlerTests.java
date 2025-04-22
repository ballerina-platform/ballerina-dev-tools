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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.projects.Document;
import io.ballerina.servicemodelgenerator.extension.diagnostics.DiagnosticsHandler;
import io.ballerina.servicemodelgenerator.extension.model.Diagnostics;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Test class to validate util methods in the DiagnosticsHandler.
 *
 * @since 2.3.0
 */
public class DiagnosticsHandlerTests {

    private DiagnosticsHandler diagnosticsHandler;

    @BeforeClass
    public final void init() throws Exception {
        Path sourceDir = Paths.get("src/test/resources").resolve("diagnostics")
                .resolve("source").toAbsolutePath();
        WorkspaceManager workspaceManager = new BallerinaLanguageServer().getWorkspaceManager();
        Path projectPath = sourceDir.resolve("sample1");
        workspaceManager.loadProject(projectPath);
        this.diagnosticsHandler = new DiagnosticsHandler(workspaceManager);
        Optional<SemanticModel> semanticModel = workspaceManager.semanticModel(projectPath);

        Path mainBal = projectPath.resolve("main.bal");
        Optional<Document> document = workspaceManager.document(mainBal);
        if (semanticModel.isEmpty() || document.isEmpty()) {
            throw new Exception("Unable to get the semantic model or document");
        }
        Field semanticModelField = DiagnosticsHandler.class.getDeclaredField("semanticModel");
        semanticModelField.setAccessible(true);
        semanticModelField.set(diagnosticsHandler, semanticModel.get());

        Field documentField = DiagnosticsHandler.class.getDeclaredField("document");
        documentField.setAccessible(true);
        documentField.set(diagnosticsHandler, document.get());

        Method initBasicTypes = DiagnosticsHandler.class.getDeclaredMethod("initBasicTypes");
        initBasicTypes.setAccessible(true);
        initBasicTypes.invoke(diagnosticsHandler);
    }

    @Test
    public void testEmptyResourcePaths() {
        String resourcePath = "";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();
        boolean result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "path cannot be empty");
    }

    @Test
    public void testEmptyResourcePathSegments() {
        String resourcePath = "foo\\//bar";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();
        boolean result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "empty resource path segment");
    }

    @Test
    public void testDotResourcePaths() {
        String resourcePath = "..";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();
        boolean result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "invalid character: '.'");
    }

    @Test
    public void testResourcePathsWithTwoSlashes() {
        String resourcePath = "foo//bar";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "empty resource path segment");
    }

    @Test
    public void testResourcePathsStartWithSlash() {
        String resourcePath = "/user";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "path cannot start with slash");

        resourcePath = "\\/user";
        diagnostics = new ArrayList<>();
        paramNames = new HashSet<>();

        result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "path cannot start with slash");
    }

    @Test
    public void testResourcePathsWithValidPath() {
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = diagnosticsHandler.validateResourcePath("user/path", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[string name]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[string|int id]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[string... name]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[string ... name]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[string ...name]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[string...]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[123]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[12.12]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[true]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[\"true\"]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[STRING]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);
    }

    @Test
    public void testResourcePathsWithConstRefs() {
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = diagnosticsHandler.validateResourcePath("[CONST c]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);
    }

    @Test
    public void testResourcePathsWithEnumRefs() {
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = diagnosticsHandler.validateResourcePath("[Value c]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = diagnosticsHandler.validateResourcePath("[VALUE2]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);
    }

    @Test
    public void testRestParamCtx() {
        String resourcePath = "foo/[string ... name]/bar";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "cannot have path segments after rest parameter");
    }

    @Test
    public void testParamNameRepetition() {
        String resourcePath = "foo/[string name]/bar/[string name]";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "duplicate parameter name: 'name'");
    }


    @Test
    public void testResourcePathsWithInvalidCharacters() {
        String resourcePath = "path/to/resource@";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "invalid character: '@'");
    }

    @Test
    public void testResourcePathsWithHyphen() {
        String resourcePath = "user-name";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "invalid character: '-'");
    }

    @Test
    public void testResourcePathsWithSpaces() {
        String resourcePath = "user name";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "invalid character: ' '");
    }

    @Test
    public void testResourcePathsWithInvalidIdentifiers() {
        String resourcePath = "bar/from";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = diagnosticsHandler.validateResourcePath(resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "usage of reserved keyword: 'from'");
    }

    @AfterClass
    public void tearDown() {
        this.diagnosticsHandler = null;
    }
}
