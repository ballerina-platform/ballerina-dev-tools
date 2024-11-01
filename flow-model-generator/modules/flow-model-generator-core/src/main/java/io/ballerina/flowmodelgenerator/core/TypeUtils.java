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

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnection;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.ArrayType;
import org.ballerinalang.diagramutil.connector.models.connector.types.EnumType;
import org.ballerinalang.diagramutil.connector.models.connector.types.ErrorType;
import org.ballerinalang.diagramutil.connector.models.connector.types.InclusionType;
import org.ballerinalang.diagramutil.connector.models.connector.types.IntersectionType;
import org.ballerinalang.diagramutil.connector.models.connector.types.MapType;
import org.ballerinalang.diagramutil.connector.models.connector.types.ObjectType;
import org.ballerinalang.diagramutil.connector.models.connector.types.RecordType;
import org.ballerinalang.diagramutil.connector.models.connector.types.StreamType;
import org.ballerinalang.diagramutil.connector.models.connector.types.TableType;
import org.ballerinalang.diagramutil.connector.models.connector.types.UnionType;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class to manage and manipulate the {@link Type} class.
 *
 * @since 1.4.0
 */
public class TypeUtils {

    public static final String TARGET_TYPE = "targetType";
    private static final Gson gson = new Gson();

    private static final String ARRAY_TYPE = "array";
    private static final String ENUM_TYPE = "enum";
    private static final String ERROR_TYPE = "error";
    private static final String INCLUSION_TYPE = "inclusion";
    private static final String INTERSECTION_TYPE = "intersection";
    private static final String MAP_TYPE = "map";
    private static final String OBJECT_TYPE = "object";
    private static final String RECORD_TYPE = "record";
    private static final String STREAM_TYPE = "stream";
    private static final String TABLE_TYPE = "table";
    private static final String UNION_TYPE = "union";

    public static Type fromString(String s) {
        Type type = gson.fromJson(s, Type.class);
        Class<? extends Type> typeClass = switch (type.getTypeName()) {
            case ARRAY_TYPE -> ArrayType.class;
            case ENUM_TYPE -> EnumType.class;
            case ERROR_TYPE -> ErrorType.class;
            case INCLUSION_TYPE -> InclusionType.class;
            case INTERSECTION_TYPE -> IntersectionType.class;
            case MAP_TYPE -> MapType.class;
            case OBJECT_TYPE -> ObjectType.class;
            case RECORD_TYPE -> RecordType.class;
            case STREAM_TYPE -> StreamType.class;
            case TABLE_TYPE -> TableType.class;
            case UNION_TYPE -> UnionType.class;
            default -> Type.class;
        };
        return gson.fromJson(s, typeClass);
    }

    public static String getTypeSignature(String typeJson) {
        Type type = fromString(typeJson);
        return getTypeSignature(type);
    }

    public static String getTypeSignature(Type type) {
        return switch (type.getTypeName()) {
            case UNION_TYPE -> {
                if (type.name != null && type.name.equals(TARGET_TYPE)) {
                    yield "json";
                }
                UnionType unionType = (UnionType) type;
                yield unionType.members.stream()
                        .map(TypeUtils::getTypeSignature)
                        .collect(Collectors.joining("|"));
            }
            default -> type.getTypeName();
        };
    }

    public static String getClientType(String importPrefix, Type returnType) {
        String clientType = String.format("%s:%s", importPrefix, NewConnection.CLIENT_SYMBOL);
        if (!returnType.getTypeName().equals(UNION_TYPE)) {
            return clientType;
        }
        UnionType unionType = (UnionType) returnType;
        Optional<Type> errorType = unionType.members.stream()
                .filter(member -> member.getTypeName().equals(ERROR_TYPE))
                .findFirst();
        return errorType.map(type -> clientType + "|" + type.getTypeName()).orElse(clientType);
    }

    public static boolean hasReturn(String typeName) {
        return !typeName.equals("()");
    }
}
