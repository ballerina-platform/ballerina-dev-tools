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

import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.projects.Document;
import io.ballerina.testmanagerservice.extension.model.TestFunction;
import io.ballerina.testmanagerservice.extension.model.TestFunctionConfig;
import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Find test functions in a given Document.
 */
public class TestFunctionsFinder {

    private static final String TEST_CONFIG_ANNOTATION = "test:Config";
    private static final String FIELD_GROUPS = "groups";
    private static final String FIELD_ENABLED = "enabled";
    private static final String VALUE_TRUE = "true";

    private final Document document;
    private final ModuleTestDetailsHolder moduleTestDetailsHolder;

    public TestFunctionsFinder(Document document, ModuleTestDetailsHolder moduleTestDetailsHolder) {
        this.document = document;
        this.moduleTestDetailsHolder = moduleTestDetailsHolder;
    }

    public void find() {
        ModulePartNode modulePartNode = document.syntaxTree().rootNode();

        NodeList<ModuleMemberDeclarationNode> members = modulePartNode.members();

        // filter the function definition nodes from here
        for (ModuleMemberDeclarationNode member : members) {
            // filter the function definition nodes from here
            if (member instanceof FunctionDefinitionNode functionDefinitionNode) {
                Optional<MetadataNode> metadata = functionDefinitionNode.metadata();
                if (metadata.isEmpty()) {
                    continue;
                }
                NodeList<AnnotationNode> annotations = metadata.get().annotations();
                for (AnnotationNode annotation : annotations) {
                    if (annotation.annotReference().toSourceCode().trim().equals(TEST_CONFIG_ANNOTATION)) {
                        TestFunctionConfig config = processTestAnnotation(annotation);
                        String functionName = functionDefinitionNode.functionName().text().trim();
                        LineRange lineRange = functionDefinitionNode.lineRange();
                        TestFunction testFunction = new TestFunction(functionName, lineRange, config);
                        this.moduleTestDetailsHolder.addTestFunctions(config.groups(), testFunction);
                    }
                }
            }
        }
    }

    private TestFunctionConfig processTestAnnotation(AnnotationNode annotationNode) {
        if (annotationNode.annotValue().isEmpty()) {
            return new TestFunctionConfig(List.of(), true);
        }
        MappingConstructorExpressionNode annotValue = annotationNode.annotValue().get();

        SeparatedNodeList<MappingFieldNode> fields = annotValue.fields();
        List<String> groups = new ArrayList<>();
        boolean enabled = true;
        for (MappingFieldNode field : fields) {
            if (!(field instanceof SpecificFieldNode specificFieldNode)) {
                continue;
            }
            String fieldName = specificFieldNode.fieldName().toSourceCode().trim();
            switch (fieldName) {
                case FIELD_GROUPS:
                    if (specificFieldNode.valueExpr().isEmpty() || !(specificFieldNode.valueExpr().get() instanceof
                                    ListConstructorExpressionNode listConstructorExpressionNode)) {
                        break;
                    }
                    SeparatedNodeList<Node> expressions = listConstructorExpressionNode.expressions();
                    for (Node expression : expressions) {
                        if (expression instanceof BasicLiteralNode basicLiteralNode) {
                            groups.add(basicLiteralNode.toSourceCode().trim());
                        }
                    }
                    break;
                case FIELD_ENABLED:
                    // process the enabled field
                    if (specificFieldNode.valueExpr().isEmpty()) {
                        break;
                    }
                    enabled = specificFieldNode.valueExpr().get().toSourceCode().trim().equals(VALUE_TRUE);
                    break;
                default:
                    // ignore
            }
        }
        // process the test annotation

        return new TestFunctionConfig(Collections.unmodifiableList(groups), enabled);
    }
}
