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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.testmanagerservice.extension.model.Annotation;
import io.ballerina.testmanagerservice.extension.model.Codedata;
import io.ballerina.testmanagerservice.extension.model.FunctionParameter;
import io.ballerina.testmanagerservice.extension.model.Metadata;
import io.ballerina.testmanagerservice.extension.model.Property;
import io.ballerina.testmanagerservice.extension.model.TestFunction;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utils class for the test manager service.
 *
 * @since 2.0.0
 */
public class Utils {

    private Utils() {
    }

    /**
     * Generates the URI for the given source path.
     *
     * @param sourcePath the source path
     * @return the generated URI as a string
     */
    public static String getExprUri(String sourcePath) {
        String exprUriString = "expr" + Paths.get(sourcePath).toUri().toString().substring(4);
        return URI.create(exprUriString).toString();
    }

    public static TestFunction getTestFunctionModel(FunctionDefinitionNode functionDefinitionNode,
                                                    SemanticModel semanticModel) {
        TestFunction.FunctionBuilder functionBuilder = new TestFunction.FunctionBuilder();

        functionBuilder.metadata(new Metadata("Test Function", "Test Function"))
                .codedata(new Codedata(functionDefinitionNode.lineRange()))
                .functionName(TestFunction.functionName(functionDefinitionNode.functionName().text()))
                .parameters(TestFunction.parameters(functionDefinitionNode.functionSignature().parameters()))
                .returnType(TestFunction.returnType(functionDefinitionNode.functionSignature().returnTypeDesc()));

        // annotations
        functionDefinitionNode.metadata().ifPresent(metadata -> {
            List<Annotation> annotations = new ArrayList<>();
            for (AnnotationNode annotationNode: metadata.annotations()) {
                annotations.add(getAnnotationModel(annotationNode, semanticModel));
            }
            functionBuilder.annotations(annotations);

        });

        functionBuilder.editable(true);

        return functionBuilder.build();
    }

    public static Annotation getAnnotationModel(AnnotationNode annotationNode, SemanticModel semanticModel) {
        AnnotationSymbol annotationSymbol = (AnnotationSymbol) semanticModel.symbol(annotationNode).get();
        String annotName = annotationSymbol.getName().orElse("");
        if (annotName.isEmpty()) {
            return null;
        }
        Optional<MappingConstructorExpressionNode> annotValue = annotationNode.annotValue();
        MappingConstructorExpressionNode mappingConstructor = annotValue.orElse(null);
        if (annotName.equals("Config")) {
            return buildConfigAnnotation(mappingConstructor);
        }
        return null;
    }

    private static Annotation buildConfigAnnotation(MappingConstructorExpressionNode mappingConstructor) {
        Annotation.ConfigAnnotationBuilder builder = new Annotation.ConfigAnnotationBuilder();
        builder.metadata(new Metadata("Config", "Test Function Configurations"));
        if (mappingConstructor == null) {
            return builder.build();
        }
        SeparatedNodeList<MappingFieldNode> fields = mappingConstructor.fields();
        for (MappingFieldNode field: fields) {
            if (field instanceof SpecificFieldNode specificFieldNode) {
                String fieldName = specificFieldNode.fieldName().toSourceCode().trim();
                Optional<ExpressionNode> expressionNode = specificFieldNode.valueExpr();

                switch (fieldName) {
                    case "enabled" -> {
                        if (expressionNode.isPresent()) {
                            String value = expressionNode.get().toSourceCode().trim();
                            if (value.equals("false")) {
                                builder.enabled(false);
                            } else {
                                builder.enabled(true);
                            }
                        }
                    }
                    case "groups" -> {
                        if (expressionNode.isPresent() &&
                                expressionNode.get() instanceof ListConstructorExpressionNode expr) {
                            List<String> groupList = new ArrayList<>();
                            for (Node groupExpr: expr.expressions()) {
                                groupList.add(groupExpr.toSourceCode().trim());
                            }
                            builder.groups(groupList);
                        }
                    }
                    default -> { }
                }
            }
        }
        return builder.build();
    }

    public static String getTestFunctionTemplate(TestFunction function) {
        StringBuilder builder = new StringBuilder();

        // build annotations
        builder.append(buildAnnotation(function.annotations()))
                .append(Constants.LINE_SEPARATOR);

        builder.append(Constants.KEYWORD_FUNCTION)
                .append(Constants.SPACE)
                .append(function.functionName().value())
                .append(buildFunctionSignature(function));

        builder.append(Constants.SPACE)
                .append(Constants.OPEN_CURLY_BRACE)
                .append(Constants.LINE_SEPARATOR)
                .append(Constants.TAB_SEPARATOR)
                .append(Constants.KEYWORD_DO)
                .append(Constants.SPACE)
                .append(Constants.OPEN_CURLY_BRACE)
                .append(Constants.LINE_SEPARATOR)
                .append(Constants.TAB_SEPARATOR)
                .append(Constants.CLOSE_CURLY_BRACE)
                .append(Constants.SPACE)
                .append(Constants.ON_FAIL_ERROR_STMT)
                .append(Constants.SPACE)
                .append(Constants.OPEN_CURLY_BRACE)
                .append(Constants.LINE_SEPARATOR)
                .append(Constants.TAB_SEPARATOR)
                .append(Constants.CLOSE_CURLY_BRACE)
                .append(Constants.LINE_SEPARATOR)
                .append(Constants.CLOSE_CURLY_BRACE);
        return builder.toString();
    }

    public static String buildFunctionSignature(TestFunction function) {
        return buildFunctionParams(function.parameters()) +
                buildReturnType(function.returnType());
    }

    public static String buildFunctionParams(List<FunctionParameter> parameters) {
        if (parameters.isEmpty()) {
            return "()";
        }
        List<String> params = new ArrayList<>();
        for (FunctionParameter param : parameters) {
            String type = param.type().value().toString().trim();
            String name = param.variable().value().toString().trim();
            String defaultValue = "";
            if (param.defaultValue() != null) {
                Object value = param.defaultValue().value();
                defaultValue = value != null ? value.toString().trim() : "";
            }
            if (defaultValue.isEmpty()) {
                params.add(type + Constants.SPACE + name);
            } else {
                params.add(type + Constants.SPACE + name + Constants.SPACE + Constants.EQUAL + Constants.SPACE
                        + defaultValue);
            }
        }
        return Constants.OPEN_PARAM + String.join(Constants.COMMA + Constants.SPACE, params)
                + Constants.CLOSED_PARAM;
    }

    public static String buildReturnType(Property returnType) {
        if (returnType == null || returnType.value() == null || returnType.value().toString().trim().isEmpty()) {
            return "";
        }
        return Constants.SPACE + Constants.KEYWORD_RETURNS + Constants.SPACE + returnType.value().toString().trim();
    }

    public static String buildAnnotation(List<Annotation> annotations) {
        List<String> annotationStrings = new ArrayList<>();
        for (Annotation annotation : annotations) {
            StringBuilder builder = new StringBuilder();
            builder.append(Constants.TEST_ANNOTATION)
                    .append(annotation.name());
            String annotValue;
            if (annotation.name().equals("Config")) {
                annotValue = buildTestConfigAnnotation(annotation);
            } else {
                annotValue = "";
            }

            if (!annotValue.isEmpty()) {
                builder.append(Constants.OPEN_CURLY_BRACE)
                        .append(Constants.LINE_SEPARATOR)
                        .append(annotValue)
                        .append(Constants.LINE_SEPARATOR)
                        .append(Constants.CLOSE_CURLY_BRACE);
            }
            annotationStrings.add(builder.toString());
        }

        if (annotationStrings.isEmpty()) {
            return "";
        }

        return String.join(Constants.LINE_SEPARATOR, annotationStrings);
    }

    public static String buildTestConfigAnnotation(Annotation annotation) {
        List<String> fieldStrings = new ArrayList<>();
        for (Property field : annotation.fields()) {
            String fieldName = field.originalName();
            Object value = field.value();
            switch (fieldName) {
                case "enabled" -> {
                    if (value instanceof String valueStr && valueStr.equals(Constants.FALSE)) {
                        fieldStrings.add(Constants.FILED_TEMPLATE.formatted(fieldName, Constants.FALSE));
                    }
                }
                case "groups" -> {
                    if (value instanceof List<?> valueList && !valueList.isEmpty()
                            && valueList.getFirst() instanceof String) {
                        List<String> groupList = valueList.stream().map(Object::toString).toList();
                        String groupStr = Constants.OPEN_BRACKET + String.join(Constants.COMMA + Constants.SPACE,
                                groupList) + Constants.CLOSE_BRACKET;
                        fieldStrings.add(Constants.FILED_TEMPLATE.formatted(fieldName, groupStr));
                    }
                }
                default -> { }
            }
        }
        if (fieldStrings.isEmpty()) {
            return "";
        }

        return String.join(Constants.COMMA + Constants.LINE_SEPARATOR, fieldStrings);
    }

    /**
     * Check whether the test import exists in the module.
     *
     * @param node module part node
     * @return true if the import exists, false otherwise
     */
    public static boolean isTestModuleImportExists(ModulePartNode node) {
        return node.imports().stream().anyMatch(importDeclarationNode -> {
            String moduleName = importDeclarationNode.moduleName().stream()
                    .map(IdentifierToken::text)
                    .collect(Collectors.joining("."));
            return importDeclarationNode.orgName().isPresent() &&
                    Constants.ORG_BALLERINA.equals(importDeclarationNode.orgName().get().orgName().text()) &&
                    Constants.MODULE_TEST.equals(moduleName);
        });
    }

    /**
     * Convert the syntax-node line range into a lsp4j range.
     *
     * @param lineRange line range
     * @return {@link Range} converted range
     */
    public static Range toRange(LineRange lineRange) {
        return new Range(toPosition(lineRange.startLine()), toPosition(lineRange.endLine()));
    }

    /**
     * Converts syntax-node line position into a lsp4j position.
     *
     * @param position line position
     * @return {@link Range} converted range
     */
    public static Range toRange(LinePosition position) {
        return new Range(toPosition(position), toPosition(position));
    }

    /**
     * Converts syntax-node line position into a lsp4j position.
     *
     * @param linePosition - line position
     * @return {@link Position} converted position
     */
    public static Position toPosition(LinePosition linePosition) {
        return new Position(linePosition.line(), linePosition.offset());
    }

}
