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


package io.ballerina.flowmodelgenerator.core.type;

import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.RecordType;
import org.ballerinalang.diagramutil.connector.models.connector.types.UnionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generates the record value for a type configuration.
 *
 * @since 2.0.0
 */
public class RecordValueGenerator {

    public static String generate(Type type) {
        StringBuilder builder = new StringBuilder();
        if (type instanceof RecordType recordType) {
            generateRecordValue(recordType, builder);
        } else if (type instanceof UnionType unionType) {
            generateUnionValue(unionType, builder);
        }
        return builder.toString();
    }

    public static void generateUnionValue(UnionType unionType, StringBuilder builder) {
        for (Type type : unionType.members) {
            if (type.selected && type instanceof RecordType recordType) {
                generateRecordValue(recordType, builder);
                break;
            }
        }
    }

    public static void generateRecordValue(RecordType recordType, StringBuilder builder) {
        if (recordType.selected) {
            return;
        }
        builder.append("{ ");
        List<String> fieldValues = new ArrayList<>();
        for (Type field: recordType.fields) {
            if (!field.selected) {
               continue;
            }
            if (field instanceof RecordType rt) {
                generateRecordValue(rt, builder);
            } else if (field instanceof UnionType ut) {
                generateUnionValue(ut, builder);
            } else {
                if (Objects.nonNull(field.value) && !field.value.isEmpty()) {
                    fieldValues.add(field.name + ": " + field.value);
                } else if (Objects.nonNull(field.defaultValue) && !field.defaultValue.isEmpty()) {
                    fieldValues.add(field.name + ": " + field.defaultValue);
                } else {
                    fieldValues.add(field.name + ": " + generateDefaultValue(field.typeName));
                }
            }
        }
        builder.append(String.join(", ", fieldValues));
        builder.append("}");
    }

    private static String generateDefaultValue(String type) {
        return "\"\"";
    }
}
