package io.ballerina.modelgenerator.commons;/*
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

import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.ErrorTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.common.utils.RecordUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Util contains all the methods to generate default values for the types and function params.
 *
 * @since 2.0.0
 */
public class DefaultValueGeneratorUtil {

    private DefaultValueGeneratorUtil() {
    }

    public static String getDefaultValueForType(TypeSymbol bType) {
        return getDefaultValueForType(bType, new StringBuilder()).toString();
    }

    private static StringBuilder getDefaultValueForType(TypeSymbol bType, StringBuilder valueBuilder) {
        if (bType == null) {
            return valueBuilder;
        } else {
            TypeSymbol rawType = CommonUtil.getRawType(bType);
            TypeDescKind typeKind = rawType.typeKind();
            switch (typeKind) {
                case TYPE_REFERENCE:
                    return getDefaultValueForType(CommonUtil.getRawType(bType), valueBuilder);
                case FLOAT:
                    valueBuilder.append(0.0F);
                    break;
                case BOOLEAN:
                    valueBuilder.append("false");
                    break;
                case ANYDATA:
                case MAP:
                    valueBuilder.append("{}");
                    break;
                case STREAM:
                    valueBuilder.append("new ()");
                    break;
                case XML:
                    valueBuilder.append("xml ``");
                    break;
                case DECIMAL:
                    valueBuilder.append("0.0d");
                    break;
                case REGEXP:
                    valueBuilder.append("re ` `");
                    break;
                case TABLE:
                    valueBuilder.append("table []");
                    break;
                case JSON:
                case NIL:
                case ANY:
                    valueBuilder.append("()");
                    break;
                case SINGLETON:
                    valueBuilder.append(rawType.signature());
                    break;
                case RECORD:
                    RecordTypeSymbol recordTypeSymbol = (RecordTypeSymbol) rawType;
                    valueBuilder.append("{");
                    List<RecordFieldSymbol> mandatoryFieldSymbols = RecordUtil
                            .getMandatoryRecordFields(recordTypeSymbol)
                            .stream().filter((recordFieldSymbol) -> recordFieldSymbol.getName().isPresent()).toList();

                    List<String> fieldValueList = new ArrayList<>();
                    for (RecordFieldSymbol mandatoryField : mandatoryFieldSymbols) {
                        String fieldName = CommonUtil.escapeReservedKeyword(mandatoryField.getName().get());
                        String value = getDefaultValueForType(mandatoryField.typeDescriptor());
                        fieldValueList.add(fieldName + ": " + value);
                    }
                    valueBuilder.append(String.join(", ", fieldValueList));
                    valueBuilder.append("}");
                    break;
                case ARRAY:
                    valueBuilder.append("[]");
                    break;
                case TUPLE:
                    TupleTypeSymbol tupleType = (TupleTypeSymbol) rawType;
                    List<String> memberDefaultValues = tupleType.
                            memberTypeDescriptors().stream().map((member) ->
                                    getDefaultValueForType(member, new StringBuilder()).toString()).toList();
                    if (memberDefaultValues.isEmpty()) {
                        valueBuilder.append("[]");
                    } else {
                        valueBuilder.append("[");
                        valueBuilder.append(String.join(", ", memberDefaultValues));
                        valueBuilder.append("]");
                    }
                    break;
                case ERROR:
                    TypeSymbol errorType = CommonUtil.getRawType(((ErrorTypeSymbol) rawType).detailTypeDescriptor());
                    StringBuilder errorString = new StringBuilder("error(\"\"");
                    if (errorType.typeKind() == TypeDescKind.RECORD) {
                        List<RecordFieldSymbol> mandatoryFields = RecordUtil
                                .getMandatoryRecordFields((RecordTypeSymbol) errorType);
                        if (!mandatoryFields.isEmpty()) {
                            errorString.append(", ");
                            List<String> detailFieldSnippets = new ArrayList<>();
                            for (RecordFieldSymbol field : mandatoryFields) {
                                String defValue = getDefaultValueForType(field.typeDescriptor());
                                String key = field.getName().get();
                                detailFieldSnippets.add(key + ": " + defValue);
                            }
                            errorString.append(String.join(", ", detailFieldSnippets));
                        }
                    }
                    errorString.append(")");
                    valueBuilder.append(errorString);
                    break;
                case OBJECT:
                    ObjectTypeSymbol objectTypeSymbol = (ObjectTypeSymbol) rawType;
                    if (objectTypeSymbol.kind() == SymbolKind.CLASS) {
                        ClassSymbol classSymbol = (ClassSymbol) objectTypeSymbol;
                        if (classSymbol.initMethod().isPresent()) {
                            List<String> values = new ArrayList<>();
                            classSymbol.initMethod().get()
                                    .typeDescriptor()
                                    .params()
                                    .ifPresent((params) -> {
                                        params.forEach(param -> {
                                            values.add(getDefaultValueForType(param.typeDescriptor()));
                                        });
                                    });
                            valueBuilder.append("new (").append(String.join(", ", values)).append(")");
                        } else {
                            valueBuilder.append("new ()");
                        }
                    } else {
                        valueBuilder.append("object {}");
                    }
                    break;
                case UNION:
                    List<TypeSymbol> members = new ArrayList<>(((UnionTypeSymbol) rawType).memberTypeDescriptors());
                    boolean hasNilType = members.stream()
                            .anyMatch(mem -> CommonUtil.getRawType(mem).typeKind() == TypeDescKind.NIL);
                    if (!hasNilType && !members.isEmpty()) {
                        valueBuilder.append(getDefaultValueForType(members.get(0)));
                    } else {
                        valueBuilder.append("()");
                    }
                    break;
                case INTERSECTION:
                    TypeSymbol effectiveType = ((IntersectionTypeSymbol) rawType).effectiveTypeDescriptor();
                    effectiveType = CommonUtil.getRawType(effectiveType);
                    if (effectiveType.typeKind() == TypeDescKind.INTERSECTION) {
                        Optional<TypeSymbol> memberType = ((IntersectionTypeSymbol) effectiveType)
                                .memberTypeDescriptors().stream()
                                .filter((typeSymbol) -> typeSymbol.typeKind() != TypeDescKind.READONLY).findAny();
                        if (memberType.isPresent()) {
                            valueBuilder.append(getDefaultValueForType(memberType.get()));
                        } else {
                            valueBuilder.append("()");
                        }
                    } else {
                        valueBuilder.append(getDefaultValueForType(effectiveType));
                    }
                    break;
                default:
                    if (typeKind.isIntegerType()) {
                        valueBuilder.append("0");
                    } else if (typeKind.isStringType()) {
                        valueBuilder.append("\"\"");
                    }
            }
            return valueBuilder;
        }
    }

}
