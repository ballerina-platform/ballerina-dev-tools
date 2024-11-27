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

package io.ballerina.flowmodelgenerator.core.db.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.ballerinalang.diagramutil.connector.models.connector.Type;

/**
 * This class is a JPA attribute converter that converts between the `Type` object and its JSON string representation
 * for database storage.
 *
 * @since 2.0.0
 */
@Converter
public class TypeConverter implements AttributeConverter<Type, String> {

    private static final Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    @Override
    public String convertToDatabaseColumn(Type type) {
        return type != null ? gson.toJson(type) : null;
    }

    @Override
    public Type convertToEntityAttribute(String json) {
        return json != null ? gson.fromJson(json, Type.class) : null;
    }
}
