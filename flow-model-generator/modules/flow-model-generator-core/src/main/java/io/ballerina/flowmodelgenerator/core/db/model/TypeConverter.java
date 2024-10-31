package io.ballerina.flowmodelgenerator.core.db.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.ballerinalang.diagramutil.connector.models.connector.Type;

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