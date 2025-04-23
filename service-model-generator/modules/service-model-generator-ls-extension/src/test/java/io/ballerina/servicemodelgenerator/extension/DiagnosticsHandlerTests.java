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
import io.ballerina.servicemodelgenerator.extension.diagnostics.ResourceFunctionFormValidator;
import io.ballerina.servicemodelgenerator.extension.model.Diagnostics;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.servicemodelgenerator.extension.diagnostics.ResourceFunctionFormValidator.Context.ADD_RESOURCE;

/**
 * Test class to validate util methods in the DiagnosticsHandler.
 *
 * @since 2.3.0
 */
public class DiagnosticsHandlerTests {

    private ResourceFunctionFormValidator validator;
    private Method validateResourcePath;

    @BeforeClass
    public final void init() throws Exception {
        Path sourceDir = Paths.get("src/test/resources").resolve("diagnostics")
                .resolve("source").toAbsolutePath();
        WorkspaceManager workspaceManager = new BallerinaLanguageServer().getWorkspaceManager();
        Path projectPath = sourceDir.resolve("sample1");
        workspaceManager.loadProject(projectPath);
        this.validator = new ResourceFunctionFormValidator(workspaceManager, ADD_RESOURCE);
        Optional<SemanticModel> semanticModel = workspaceManager.semanticModel(projectPath);

        Path mainBal = projectPath.resolve("main.bal");
        Optional<Document> document = workspaceManager.document(mainBal);
        if (semanticModel.isEmpty() || document.isEmpty()) {
            throw new Exception("Unable to get the semantic model or document");
        }
        Field semanticModelField = ResourceFunctionFormValidator.class.getDeclaredField("semanticModel");
        semanticModelField.setAccessible(true);
        semanticModelField.set(validator, semanticModel.get());

        Field documentField = ResourceFunctionFormValidator.class.getDeclaredField("document");
        documentField.setAccessible(true);
        documentField.set(validator, document.get());

        Method initBasicTypes = ResourceFunctionFormValidator.class.getDeclaredMethod("initBasicTypes");
        initBasicTypes.setAccessible(true);
        initBasicTypes.invoke(validator);

        this.validateResourcePath = ResourceFunctionFormValidator.class.getDeclaredMethod("validateResourcePath",
                String.class, List.class, Set.class);
        validateResourcePath.setAccessible(true);
    }

    @Test
    public void testEmptyResourcePaths() throws Exception {
        String resourcePath = "";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();
        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "path cannot be empty");
    }

    @Test
    public void testEmptyResourcePathSegments() throws Exception {
        String resourcePath = "foo\\//bar";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();
        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "empty resource path segment");
    }

    @Test
    public void testDotResourcePaths() throws Exception {
        String resourcePath = "..";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();
        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "invalid character: '.'");
    }

    @Test
    public void testResourcePathsWithTwoSlashes() throws Exception {
        String resourcePath = "foo//bar";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "empty resource path segment");
    }

    @Test
    public void testResourcePathsStartWithSlash() throws Exception {
        String resourcePath = "/user";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "path cannot start with slash");

        resourcePath = "\\/user";
        diagnostics = new ArrayList<>();
        paramNames = new HashSet<>();

        result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "path cannot start with slash");
    }

    @Test
    public void testValidResourcePaths() throws Exception {
        String resourcePath = "user/path";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[string name]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[string|int id]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[string... name]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[string ... name]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[string ...name]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[string ...]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[string...]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[123]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[12.12]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[true]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[\"true\"]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        result = (boolean) validateResourcePath.invoke(validator, "[STRING]", diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);
    }

    @Test
    public void testResourcePathsWithConstRefs() throws Exception {
        String resourcePath = "[CONST c]";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);
    }

    @Test
    public void testResourcePathsWithEnumRefs() throws Exception {
        String resourcePath = "[Value c]";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);

        paramNames = new HashSet<>();
        resourcePath = "[VALUE2]";
        result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertTrue(result);
        Assert.assertEquals(diagnostics.size(), 0);
    }

    @Test
    public void testRestParamCtx() throws Exception {
        String resourcePath = "foo/[string ... name]/bar";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "cannot have path segments after rest parameter");
    }

    @Test
    public void testParamNameRepetition() throws Exception {
        String resourcePath = "foo/[string name]/bar/[string name]";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "duplicate parameter name: 'name'");
    }

    @Test
    public void testResourcePathsWithInvalidCharacters() throws Exception {
        String resourcePath = "path/to/resource@";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "invalid character: '@'");
    }

    @Test
    public void testResourcePathsWithHyphen() throws Exception {
        String resourcePath = "user-name";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "invalid character: '-'");
    }

    @Test
    public void testResourcePathsWithSpaces() throws Exception {
        String resourcePath = "user name";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "invalid character: ' '");
    }

    @Test
    public void testResourcePathsWithInvalidIdentifiers() throws Exception {
        String resourcePath = "bar/from";
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();

        boolean result = (boolean) validateResourcePath.invoke(validator, resourcePath, diagnostics, paramNames);
        Assert.assertFalse(result);
        Assert.assertEquals(diagnostics.size(), 1);
        Assert.assertEquals(diagnostics.getFirst().message(), "usage of reserved keyword: 'from'");
    }

    @AfterClass
    public void tearDown() {
        this.validator = null;
        this.validateResourcePath = null;
    }
}
