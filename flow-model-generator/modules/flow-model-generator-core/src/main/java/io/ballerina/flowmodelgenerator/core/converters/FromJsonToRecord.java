package io.ballerina.flowmodelgenerator.core.converters;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.ballerina.flowmodelgenerator.core.TypesManager;
import io.ballerina.flowmodelgenerator.core.converters.exception.JsonToRecordConverterException;
import io.ballerina.flowmodelgenerator.core.model.Member;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.TypeData;
import io.ballerina.flowmodelgenerator.core.utils.SourceCodeGenerator;
import org.apache.commons.lang3.StringUtils;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.escapeIdentifier;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.getAndUpdateFieldNames;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.getExistingTypeNames;
import static io.ballerina.flowmodelgenerator.core.converters.utils.ListOperationUtils.difference;
import static io.ballerina.flowmodelgenerator.core.converters.utils.ListOperationUtils.intersection;

public class FromJsonToRecord {

    private static final Gson GSON = new Gson();

    private static final String ARRAY_RECORD_SUFFIX = "Item";
    private static final String JSON_KEYWORD = "json";
    private static final String NEW_RECORD_NAME = "NewRecord";
    private static final String STRING_TYPE = "string";
    private static final String BOOLEAN_TYPE = "boolean";
    private static final String DECIMAL_TYPE = "decimal";
    private static final String INT_TYPE = "int";
    private static final String ANYDATA_TYPE = "anydata";

    private final Set<String> existingFieldNamesSet;
    private final Map<String, String> updatedFieldNames;
    private final Map<String, TypeDesc> typeDefinitions;

    // Cache for primitive type detection
    private final Map<String, String> primitiveTypeCache = new ConcurrentHashMap<>();

    private final String typePrefix;
    private final boolean isClosed;
    private final boolean asInlineType;

    public FromJsonToRecord(boolean isClosed, boolean isRecordTypeDesc, String typePrefix,
                            WorkspaceManager workspaceManager, Path filePath) {
        this.isClosed = isClosed;
        this.asInlineType = isRecordTypeDesc;
        this.typePrefix = typePrefix;

        List<String> existingNames = getExistingTypeNames(workspaceManager, filePath);
        this.existingFieldNamesSet = new HashSet<>(existingNames);
        this.updatedFieldNames = new HashMap<>();
        this.typeDefinitions = new LinkedHashMap<>();
    }

    public JsonElement convert(String jsonString, String name) throws JsonToRecordConverterException {
        JsonElement jsonElement;
        try {
            jsonElement = JsonParser.parseString(jsonString);
        } catch (Exception e) {
            throw new JsonToRecordConverterException("JSON string parsing failed: Invalid JSON structure");
        }

        String typeDefName = (name == null || name.isEmpty()) ? NEW_RECORD_NAME : name;

        if (jsonElement.isJsonObject()) {
            generateForNestedStructures(jsonElement, name, false, null);
        } else if (jsonElement.isJsonArray()) {
            generateNestedStructuresForArray(jsonElement.getAsJsonArray(), typeDefName, null);
        } else {
            throw new JsonToRecordConverterException("JSON string parsing failed: Invalid JSON structure");
        }

        List<TypesManager.TypeDataWithRefs> typeDataList = new ArrayList<>(typeDefinitions.size());

        if (asInlineType) {
            TypeDesc mainTypeDesc = typeDefinitions.get(typeDefName);
            if (mainTypeDesc == null) {
                throw new JsonToRecordConverterException("Error occurred while generating type");
            }
            TypeDesc inlineTypeDesc = convertToInlineTypeDesc(mainTypeDesc);
            typeDataList.add(
                    new TypesManager.TypeDataWithRefs(
                            toTypeData(name, inlineTypeDesc),
                            List.of()
                    )
            );
        } else {
            typeDefinitions.forEach((key, value) -> typeDataList.add(
                    new TypesManager.TypeDataWithRefs(
                            toTypeData(key, value),
                            List.of()
                    )
            ));
        }

        // Clear to free up memory
        typeDefinitions.clear();
        existingFieldNamesSet.clear();
        updatedFieldNames.clear();

        return GSON.toJsonTree(typeDataList);
    }

    private void generateNestedStructuresForArray(JsonArray jsonArray, String typeName, String moveBefore) {
        for (JsonElement element : jsonArray) {
            generateForNestedStructures(element, typeName + ARRAY_RECORD_SUFFIX, true, null);
        }
        generateArray(jsonArray, typeName, moveBefore);
    }

    private void generateForNestedStructures(JsonElement jsonElement, String elementKey,
                                             boolean isArraySuffixAdded, String moveBefore) {
        String typeName = escapeIdentifier(StringUtils.capitalize(elementKey));
        String updatedTypeName = getAndUpdateFieldNames(
                typeName, isArraySuffixAdded, new ArrayList<>(existingFieldNamesSet), updatedFieldNames
        );

        if (jsonElement.isJsonObject()) {
            generateRecord(jsonElement.getAsJsonObject(), updatedTypeName, moveBefore);
        } else if (jsonElement.isJsonArray()) {
            generateNestedStructuresForArray(jsonElement.getAsJsonArray(), updatedTypeName, moveBefore);
        }
    }

    private void generateRecord(JsonObject jsonObject, String typeName, String moveBefore) {
        RecordTypeDesc recordTypeDesc = createRecordTypeDesc(jsonObject, typeName);
        insertIntoTypeDescMap(recordTypeDesc, typeName, moveBefore);
    }

    private void generateArray(JsonArray jsonArray, String typeName, String moveBefore) {
        ArrayTypeDesc arrayTypeDesc = createArrayTypeDesc(typeName, jsonArray);
        insertIntoTypeDescMap(arrayTypeDesc, typeName, moveBefore);
    }

    private void insertIntoTypeDescMap(TypeDesc typeDesc, String typeName, String moveBefore) {
        if (moveBefore == null || moveBefore.equals(typeName)) {
            typeDefinitions.put(typeName, typeDesc);
            return;
        }

        // Optimize the reordering logic
        Map<String, TypeDesc> tempMap = new LinkedHashMap<>();
        boolean inserted = false;

        for (Map.Entry<String, TypeDesc> entry : typeDefinitions.entrySet()) {
            if (entry.getKey().equals(moveBefore) && !inserted) {
                tempMap.put(typeName, typeDesc);
                inserted = true;
            }
            tempMap.put(entry.getKey(), entry.getValue());
        }

        if (!inserted) {
            tempMap.put(typeName, typeDesc);
        }

        typeDefinitions.clear();
        typeDefinitions.putAll(tempMap);
    }

    private RecordTypeDesc createRecordTypeDesc(JsonObject jsonObject, String recordTypeName) {
        List<RecordField> recordFields = new ArrayList<>(jsonObject.size());

        if (typeDefinitions.containsKey(recordTypeName)) {
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonArray() || value.isJsonObject()) {
                    String name = getRecordName(entry.getKey());
                    generateForNestedStructures(value, name, false, name);
                }
            }
            mergeWithExistingRecordFields(jsonObject, recordTypeName, recordFields);
        } else {
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonObject() || value.isJsonArray()) {
                    String name = getRecordName(entry.getKey());
                    generateForNestedStructures(value, name, false, null);
                }
                recordFields.add(createRecordField(entry, false));
            }

            // Check again if type was created during processing (nested structure with same field)
            if (typeDefinitions.containsKey(recordTypeName)) {
                recordFields.clear();
                mergeWithExistingRecordFields(jsonObject, recordTypeName, recordFields);
            }
        }

        return new RecordTypeDesc(recordFields, this.isClosed);
    }

    private RecordField createRecordField(Map.Entry<String, JsonElement> entry, boolean isOptional) {
        String fieldName = entry.getKey();
        JsonElement element = entry.getValue();

        if (element.isJsonPrimitive()) {
            String typeName = getPrimitiveType(element.getAsJsonPrimitive());
            return new RecordField(fieldName, new PrimitiveTypeDesc(typeName), isOptional);
        } else if (element.isJsonObject()) {
            String typeName = escapeIdentifier(getRecordName(fieldName.trim()));
            String updatedTypeName = getAndUpdateFieldNames(
                    typeName, false, new ArrayList<>(existingFieldNamesSet), updatedFieldNames
            );
            return new RecordField(fieldName, new ReferenceTypeDesc(updatedTypeName), isOptional);
        } else if (element.isJsonArray()) {
            String typeName = escapeIdentifier(getRecordName(fieldName.trim()));
            String updatedTypeName = getAndUpdateFieldNames(
                    typeName, false, new ArrayList<>(existingFieldNamesSet), updatedFieldNames
            );
            return new RecordField(fieldName, new ReferenceTypeDesc(updatedTypeName), isOptional);
        }
        return new RecordField(fieldName, null);
    }

    private ArrayTypeDesc createArrayTypeDesc(String fieldName, JsonArray jsonArray) {
        String typeName = getRecordName(fieldName);

        // Use Set to avoid duplicates more efficiently
        Set<String> seenTypes = new HashSet<>();
        List<TypeDesc> typeDescList = new ArrayList<>();

        for (JsonElement element : jsonArray) {
            TypeDesc typeDesc = null;
            String typeString = null;

            if (element.isJsonPrimitive()) {
                String primitiveType = getPrimitiveType(element.getAsJsonPrimitive());
                typeDesc = new PrimitiveTypeDesc(primitiveType);
                typeString = primitiveType;
            } else if (element.isJsonNull()) {
                typeDesc = new PrimitiveTypeDesc(JSON_KEYWORD);
                typeString = JSON_KEYWORD;
            } else if (element.isJsonObject()) {
                String typeNameWithArrSuffix = escapeIdentifier(StringUtils.capitalize(typeName) + ARRAY_RECORD_SUFFIX);
                String updatedType = getAndUpdateFieldNames(
                        typeNameWithArrSuffix, true, new ArrayList<>(existingFieldNamesSet), updatedFieldNames
                );
                typeDesc = new ReferenceTypeDesc(updatedType);
                typeString = updatedType;
            } else if (element.isJsonArray()) {
                typeDesc = createArrayTypeDesc(fieldName, element.getAsJsonArray());
                typeString = typeDesc.toString();
            }

            if (typeDesc != null && seenTypes.add(typeString)) {
                typeDescList.add(typeDesc);
            }
        }

        TypeDesc elementType = createUnionType(typeDescList);
        return new ArrayTypeDesc(elementType);
    }

    private TypeDesc createUnionType(List<TypeDesc> typeDescList) {
        if (typeDescList.isEmpty()) {
            return new PrimitiveTypeDesc(JSON_KEYWORD);
        }

        if (typeDescList.size() == 1) {
            return typeDescList.getFirst();
        }

        return new UnionTypeDesc(typeDescList);
    }

    private String getRecordName(String name) {
        String cap = StringUtils.capitalize(name);
        return (typePrefix == null || typePrefix.isEmpty()) ? cap : StringUtils.capitalize(typePrefix) + cap;
    }

    private void mergeWithExistingRecordFields(JsonObject jsonObject, String recordName,
                                               List<RecordField> recordFields) {
        TypeDesc typeDesc = typeDefinitions.get(recordName);
        if (!(typeDesc instanceof RecordTypeDesc recordTypeDesc)) {
            return;
        }

        Map<String, RecordField> previousRecordFields = recordTypeDesc.fields.stream()
                .collect(Collectors.toMap(
                        recordField -> recordField.fieldName,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        Map<String, RecordField> newRecordFields = new LinkedHashMap<>(jsonObject.size());
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            RecordField field = createRecordField(entry, false);
            newRecordFields.put(field.fieldName, field);
        }

        updateRecordFields(recordFields, previousRecordFields, newRecordFields);
    }

    private void updateRecordFields(List<RecordField> updatedRecordFields,
                                    Map<String, RecordField> previousRecordFields,
                                    Map<String, RecordField> newRecordFields) {
        Map<String, Map.Entry<RecordField, RecordField>> intersectingRecordFields =
                intersection(previousRecordFields, newRecordFields);
        Map<String, RecordField> differentRecordFields = difference(previousRecordFields, newRecordFields);

        for (Map.Entry<String, Map.Entry<RecordField, RecordField>> entry : intersectingRecordFields.entrySet()) {
            String fieldName = entry.getKey();
            RecordField f1 = entry.getValue().getKey();
            RecordField f2 = entry.getValue().getValue();

            if (f1.toString().equals(f2.toString())) {
                updatedRecordFields.add(f1);
                continue;
            }

            boolean isOptional = f1.isOptional || f2.isOptional;

            if (f1.type.toString().equals(f2.type.toString()) && isOptional) {
                updatedRecordFields.add(new RecordField(fieldName, f1.type, true));
                continue;
            }

            if (f1.type instanceof OptionalTypeDesc || f2.type instanceof OptionalTypeDesc) {
                TypeDesc f1Type = getNonOptionalTypeDesc(f1.type);
                TypeDesc f2Type = getNonOptionalTypeDesc(f2.type);

                if (f1Type.toString().equals(f2Type.toString())) {
                    OptionalTypeDesc optionalTypeDesc = new OptionalTypeDesc(f1Type);
                    updatedRecordFields.add(new RecordField(fieldName, optionalTypeDesc, isOptional));
                    continue;
                }

                UnionTypeDesc unionTypeDesc = new UnionTypeDesc(List.of(f1Type, f2Type));
                OptionalTypeDesc optionalTypeDesc = new OptionalTypeDesc(unionTypeDesc);
                updatedRecordFields.add(new RecordField(fieldName, optionalTypeDesc, isOptional));
                continue;
            }

            UnionTypeDesc unionTypeDesc = new UnionTypeDesc(List.of(f1.type, f2.type));
            updatedRecordFields.add(new RecordField(fieldName, unionTypeDesc, isOptional));
        }

        for (RecordField recordField : differentRecordFields.values()) {
            recordField.isOptional = true;
            updatedRecordFields.add(recordField);
        }
    }

    private TypeDesc convertToInlineTypeDesc(TypeDesc mainTypeDesc) {
        return switch (mainTypeDesc) {
            case RecordTypeDesc recordTypeDesc -> {
                List<RecordField> recordFields = new ArrayList<>(recordTypeDesc.fields.size());
                for (RecordField field : recordTypeDesc.fields) {
                    recordFields.add(new RecordField(
                            field.fieldName,
                            convertToInlineTypeDesc(field.type),
                            field.isOptional
                    ));
                }
                yield new RecordTypeDesc(recordFields, this.isClosed);
            }
            case ArrayTypeDesc arrayTypeDesc -> {
                TypeDesc elementType = convertToInlineTypeDesc(arrayTypeDesc.elementType);
                yield new ArrayTypeDesc(elementType);
            }
            case OptionalTypeDesc optionalTypeDesc -> {
                TypeDesc nonOptionalType = getNonOptionalTypeDesc(optionalTypeDesc.typeDesc);
                yield new OptionalTypeDesc(convertToInlineTypeDesc(nonOptionalType));
            }
            case UnionTypeDesc unionTypeDesc -> {
                List<TypeDesc> inlineMembers = new ArrayList<>(unionTypeDesc.members.size());
                for (TypeDesc member : unionTypeDesc.members) {
                    inlineMembers.add(convertToInlineTypeDesc(member));
                }
                yield new UnionTypeDesc(inlineMembers);
            }
            case PrimitiveTypeDesc primitiveTypeDesc -> primitiveTypeDesc;
            case ReferenceTypeDesc referenceTypeDesc ->
                    convertToInlineTypeDesc(typeDefinitions.get(referenceTypeDesc.typeName));
            default -> mainTypeDesc;
        };
    }

    private Object toTypeData(String name, TypeDesc typeDesc) {
        TypeData.TypeDataBuilder typeDataBuilder = new TypeData.TypeDataBuilder();

        if (name != null) {
            typeDataBuilder.name(name);
        }

        typeDataBuilder
                .includes(List.of())
                .editable()
                .metadata()
                    .label(name)
                    .description("")
                    .stepOut()
                .properties()
                    .name(name, false, false, false)
                    .isPublic(true, true, false, false)
                    .description("", true, false, false)
                    .isArray("false", true, false, false)
                    .arraySize("", true, true, true);

        return switch (typeDesc) {
            case RecordTypeDesc recordTypeDesc -> {
                Member.MemberBuilder memberBuilder = new Member.MemberBuilder();
                List<Member> members = new ArrayList<>(recordTypeDesc.fields.size());

                for (RecordField field : recordTypeDesc.fields) {
                    members.add(memberBuilder
                            .kind(Member.MemberKind.FIELD)
                            .type(toTypeData(null, field.type))
                            .name(field.fieldName)
                            .refs(List.of())
                            .optional(field.isOptional)
                            .build());
                }

                yield typeDataBuilder
                        .members(members)
                        .codedata()
                            .node(NodeKind.RECORD)
                            .stepOut()
                        .allowAdditionalFields(!this.isClosed)
                        .build();
            }
            case ArrayTypeDesc arrayTypeDesc -> {
                Member.MemberBuilder memberBuilder = new Member.MemberBuilder();
                Member arrayTypeParam = memberBuilder
                        .kind(Member.MemberKind.TYPE)
                        .type(toTypeData(null, arrayTypeDesc.elementType))
                        .build();
                yield typeDataBuilder
                        .members(List.of(arrayTypeParam))
                        .codedata()
                            .node(NodeKind.ARRAY)
                            .stepOut()
                        .properties()
                            .isArray("true", true, false, false)
                            .arraySize("", true, false, false)
                            .stepOut()
                        .build();
            }
            case UnionTypeDesc unionTypeDesc -> {
                Member.MemberBuilder memberBuilder = new Member.MemberBuilder();
                List<Member> members = new ArrayList<>(unionTypeDesc.members.size());

                for (TypeDesc member : unionTypeDesc.members) {
                    members.add(memberBuilder
                            .kind(Member.MemberKind.TYPE)
                            .type(toTypeData(null, member))
                            .build());
                }

                yield typeDataBuilder
                        .members(members)
                        .codedata()
                            .node(NodeKind.UNION)
                            .stepOut()
                        .build();
            }
            case OptionalTypeDesc optionalTypeDesc -> {
                TypeDesc nonOptionalTypeDesc = getNonOptionalTypeDesc(optionalTypeDesc.typeDesc);
                String typeAsString = nonOptionalTypeDesc.toString();

                yield switch (nonOptionalTypeDesc) {
                    case UnionTypeDesc __ -> "(" + typeAsString + ")?";
                    case ArrayTypeDesc __ -> "(" + typeAsString + ")?";
                    default -> typeAsString + "?";
                };
            }
            case PrimitiveTypeDesc primitiveTypeDesc -> primitiveTypeDesc.typeName;
            case ReferenceTypeDesc referenceTypeDesc -> referenceTypeDesc.typeName;
            default -> typeDesc.toString();
        };
    }

    private TypeDesc getNonOptionalTypeDesc(TypeDesc typeDesc) {
        return switch (typeDesc) {
            case OptionalTypeDesc optionalTypeDesc ->
                    optionalTypeDesc.typeDesc instanceof OptionalTypeDesc
                            ? getNonOptionalTypeDesc(optionalTypeDesc.typeDesc)
                            : optionalTypeDesc.typeDesc;
            case UnionTypeDesc unionTypeDesc -> {
                List<TypeDesc> nonOptionalMembers = new ArrayList<>(unionTypeDesc.members.size());
                for (TypeDesc member : unionTypeDesc.members) {
                    nonOptionalMembers.add(getNonOptionalTypeDesc(member));
                }
                yield new UnionTypeDesc(nonOptionalMembers);
            }
            default -> typeDesc;
        };
    }

    private String getPrimitiveType(JsonPrimitive value) {
        // Use caching for primitive type detection
        String valueStr = value.toString();
        return primitiveTypeCache.computeIfAbsent(valueStr, __ -> {
            if (value.isString()) {
                return STRING_TYPE;
            } else if (value.isBoolean()) {
                return BOOLEAN_TYPE;
            } else if (value.isNumber()) {
                String strValue = value.getAsNumber().toString();
                return strValue.contains(".") ? DECIMAL_TYPE : INT_TYPE;
            }
            return ANYDATA_TYPE;
        });
    }

    private static final class RecordField {
        final String fieldName;
        final TypeDesc type;
        boolean isOptional = false;

        RecordField(String fieldName, TypeDesc type) {
            this.fieldName = fieldName;
            this.type = type;
        }

        RecordField(String fieldName, TypeDesc type, boolean isOptional) {
            this.fieldName = fieldName;
            this.type = type;
            this.isOptional = isOptional;
        }

        @Override
        public String toString() {
            return type.toString() + " " + fieldName + (isOptional ? "?" : "") + ";";
        }
    }

    private static final class OptionalTypeDesc extends TypeDesc {
        final TypeDesc typeDesc;

        OptionalTypeDesc(TypeDesc typeDesc) {
            this.typeDesc = typeDesc;
        }

        @Override
        public String toString() {
            return typeDesc.toString() + "?";
        }
    }

    private static final class ArrayTypeDesc extends TypeDesc {
        final TypeDesc elementType;

        ArrayTypeDesc(TypeDesc elementType) {
            this.elementType = elementType;
        }

        @Override
        public String toString() {
            return (elementType instanceof UnionTypeDesc ? "(" + elementType + ")" : elementType.toString()) + "[]";
        }
    }

    private static final class RecordTypeDesc extends TypeDesc {
        final List<RecordField> fields;
        final boolean isClosed;

        RecordTypeDesc(List<RecordField> fields, boolean isClosed) {
            this.fields = fields;
            this.isClosed = isClosed;
        }

        @Override
        public String toString() {
            String template = this.isClosed ? "record {|%s|}" : "record {%s}";
            StringBuilder sb = new StringBuilder();
            for (RecordField field : fields) {
                sb.append(field.toString());
            }
            return String.format(template, sb);
        }
    }

    private static final class UnionTypeDesc extends TypeDesc {
        final List<TypeDesc> members;

        UnionTypeDesc(List<TypeDesc> members) {
            this.members = members;
        }

        @Override
        public String toString() {
            if (members.isEmpty()) {
                return "";
            }

            if (members.size() == 1) {
                return members.getFirst().toString();
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < members.size(); i++) {
                if (i > 0) {
                    sb.append("|");
                }
                sb.append(members.get(i).toString());
            }
            return sb.toString();
        }
    }

    private static final class ReferenceTypeDesc extends TypeDesc {
        final String typeName;

        ReferenceTypeDesc(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public String toString() {
            return typeName;
        }
    }

    private static final class PrimitiveTypeDesc extends TypeDesc {
        final String typeName;

        PrimitiveTypeDesc(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public String toString() {
            return this.typeName;
        }
    }

    private static class TypeDesc {
        TypeDesc() {
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
