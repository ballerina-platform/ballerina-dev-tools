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

package io.ballerina.servicemodelgenerator.extension.util;

import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Field;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.FunctionReturnType;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.ServiceClass;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Util class for service class related operations.
 *
 * @since 2.0.0
 */
public class ServiceClassUtil {

    public static String buildObjectFiledString(Field field) {
        StringBuilder builder = new StringBuilder();
        if (field.isPrivate()) {
            builder.append("private ");
        }
        if (field.isFinal()) {
            builder.append("final ");
        }
        builder.append(field.getType().getValue()).append(" ").append(field.getName().getValue());
        if (Objects.nonNull(field.getDefaultValue().getValue()) && !field.getDefaultValue().getValue().isEmpty()) {
            builder.append(" = ").append(field.getDefaultValue().getValue());
        }
        builder.append(";");
        return builder.toString();
    }

    public static ServiceClass getServiceClass(ClassDefinitionNode classDef) {
        ServiceClass.ServiceClassBuilder builder = new ServiceClass.ServiceClassBuilder();

        List<Function> functions = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        populateFunctionsAndFields(classDef, functions, fields);

        builder.name(classDef.className().text().trim())
                .type(getClassType(classDef))
                .properties(Map.of("name", buildClassNameProperty(classDef.className().text().trim(),
                        classDef.className().lineRange())))
                .codedata(new Codedata(classDef.lineRange()))
                .functions(functions)
                .fields(fields);

        return builder.build();
    }

    private static String getClassType(ClassDefinitionNode classDef) {
        if (classDef.classTypeQualifiers().isEmpty()) {
            return ServiceModelGeneratorConstants.CLASS_TYPE_DEFAULT;
        }
        return classDef.classTypeQualifiers().get(0).text().trim();
    }

    private static Value buildClassNameProperty(String className, LineRange lineRange) {
        Value value = new Value();
        value.setMetadata(new MetaData("Class Name", "The name of the class definition"));
        value.setCodedata(new Codedata(lineRange));
        value.setEnabled(true);
        value.setEditable(false);
        value.setValue(className);
        value.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER);
        value.setValueTypeConstraint("string");
        value.setPlaceholder("");
        return value;
    }

    private static void populateFunctionsAndFields(ClassDefinitionNode classDef, List<Function> functions,
                                                   List<Field> fields) {
        classDef.members().forEach(member -> {
            if (member instanceof FunctionDefinitionNode functionDefinitionNode) {
                functions.add(buildMemberFunction(functionDefinitionNode));
            } else if (member instanceof ObjectFieldNode objectFieldNode) {
                fields.add(buildClassField(objectFieldNode));
            }
        });
    }

    private static Function buildMemberFunction(FunctionDefinitionNode functionDef) {
        Function functionModel = Function.getNewFunction();
        FunctionKind kind = getFunctionKind(functionDef);
        updateMetadata(functionModel, kind);
        functionModel.setKind(kind.name());

        if (kind.equals(FunctionKind.RESOURCE)) {
            Value accessor = functionModel.getAccessor();
            accessor.setValue(functionDef.functionName().text().trim());
            accessor.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER);
            accessor.setEnabled(true);
            accessor.setEditable(true);
            updateFunctionNameProperty(functionModel.getName(), Utils.getPath(functionDef.relativeResourcePath()),
                    functionDef.functionName().lineRange());
        } else {
            updateFunctionNameProperty(functionModel.getName(), functionDef.functionName().text().trim(),
                    functionDef.functionName().lineRange());
        }

        FunctionSignatureNode functionSignatureNode = functionDef.functionSignature();
        Optional<ReturnTypeDescriptorNode> returnTypeDesc = functionSignatureNode.returnTypeDesc();
        if (returnTypeDesc.isPresent()) {
            FunctionReturnType returnType = functionModel.getReturnType();
            if (Objects.nonNull(returnType)) {
                returnType.setValue(returnTypeDesc.get().type().toString().trim());
                returnType.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_TYPE);
                returnType.setEnabled(true);
                returnType.setEditable(true);
                returnType.setOptional(true);
            }
        }
        SeparatedNodeList<ParameterNode> parameters = functionSignatureNode.parameters();
        List<Parameter> parameterModels = new ArrayList<>();
        parameters.forEach(parameterNode -> {
            Optional<Parameter> parameterModel = Utils.getParameterModel(parameterNode, false);
            parameterModel.ifPresent(parameterModels::add);
        });
        functionModel.setParameters(parameterModels);
        functionModel.setEnabled(true);
        functionModel.setEditable(true);
        functionModel.setCodedata(new Codedata(functionDef.lineRange()));
        functionModel.setSchema(Map.of(ServiceModelGeneratorConstants.PARAMETER, Parameter.parameterSchema()));
        return functionModel;
    }

    private static Field buildClassField(ObjectFieldNode objectField) {
        Parameter parameterModel = Parameter.getNewField();
        Value type = parameterModel.getType();
        type.setValue(objectField.typeName().toSourceCode().trim());
        type.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_TYPE);
        type.setType(true);
        type.setEnabled(true);
        Value name = parameterModel.getName();
        name.setValue(objectField.fieldName().text().trim());
        name.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER);
        name.setEnabled(true);
        name.setEditable(false);
        name.setCodedata(new Codedata(objectField.fieldName().lineRange()));
        parameterModel.setEnabled(true);
        if (objectField.expression().isPresent()) {
            Value defaultValue = parameterModel.getDefaultValue();
            defaultValue.setValue(objectField.expression().get().toString().trim());
            defaultValue.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_EXPRESSION);
            defaultValue.setEnabled(true);
        }

        boolean isPrivate = objectField.visibilityQualifier().isPresent()
                && objectField.visibilityQualifier().get().text().trim().equals("private");
        boolean isFinal = objectField.qualifierList().stream()
                .anyMatch(qualifier -> qualifier.text().trim().equals("final"));

        return new Field(parameterModel, isPrivate, isFinal, new Codedata(objectField.lineRange()));
    }

    private static FunctionKind getFunctionKind(FunctionDefinitionNode functionDefinitionNode) {
        for (Token qualifier : functionDefinitionNode.qualifierList()) {
            if (qualifier.text().trim().matches(ServiceModelGeneratorConstants.REMOTE)) {
                return FunctionKind.REMOTE;
            } else if (qualifier.text().trim().matches(ServiceModelGeneratorConstants.RESOURCE)) {
                return FunctionKind.RESOURCE;
            }
        }
        if (functionDefinitionNode.functionName().text().trim().equals(ServiceModelGeneratorConstants.INIT)) {
            return FunctionKind.INIT;
        }
        return FunctionKind.DEFAULT;
    }

    private static void updateFunctionNameProperty(Value value, String functionName, LineRange lineRange) {
        value.setMetadata(new MetaData("Function Name", "The name of the function"));
        value.setEnabled(true);
        value.setEditable(false);
        value.setValue(functionName);
        value.setCodedata(new Codedata(lineRange));
        value.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER);
        value.setValueTypeConstraint("string");
        value.setPlaceholder("");
    }

    private static void updateMetadata(Function function, FunctionKind kind) {
        switch (kind) {
            case INIT -> function.setMetadata(new MetaData("Init Method", "Init Method"));
            case REMOTE -> function.setMetadata(new MetaData("Remote Method", "Remote Method"));
            case RESOURCE -> function.setMetadata(new MetaData("Resource Method", "Resource Method"));
            case DEFAULT -> function.setMetadata(new MetaData("Object Method", "Object Method"));
        }
    }

    public static String getTcpConnectionServiceTemplate() {
        return "service class %s {%n" +
                "    *tcp:ConnectionService;%n" +
                "%n" +
                "    function init() {%n" +
                "        do {%n" +
                "        } on fail error err {%n" +
                "            // handle error%n" +
                "            panic error(\"Unhandled error\", err);%n" +
                "        }%n" +
                "    }%n" +
                "%n" +
                "    remote function onBytes(tcp:Caller caller, readonly & byte[] data) returns tcp:Error? {%n" +
                "        do {%n" +
                "%n" +
                "        } on fail error err {%n" +
                "            // handle error%n" +
                "            panic error(\"Unhandled error\", err);%n" +
                "        }%n" +
                "    }%n" +
                "%n" +
                "    remote function onError(tcp:Error tcpError) {%n" +
                "        do {%n" +
                "%n" +
                "        } on fail error err {%n" +
                "            // handle error%n" +
                "            panic error(\"Unhandled error\", err);%n" +
                "        }%n" +
                "    }%n" +
                "%n" +
                "    remote function onClose() {%n" +
                "        do {%n" +
                "%n" +
                "        } on fail error err {%n" +
                "            // handle error%n" +
                "            panic error(\"Unhandled error\", err);%n" +
                "        }%n" +
                "    }%n" +
                "}%n";
    }

    public enum FunctionKind {
        INIT,
        REMOTE,
        RESOURCE,
        DEFAULT
    }
}
