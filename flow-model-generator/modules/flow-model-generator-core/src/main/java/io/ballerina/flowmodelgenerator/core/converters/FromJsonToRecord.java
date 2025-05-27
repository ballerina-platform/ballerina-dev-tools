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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.escapeIdentifier;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.getAndUpdateFieldNames;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.getExistingTypeNames;
import static io.ballerina.flowmodelgenerator.core.converters.utils.ListOperationUtils.difference;
import static io.ballerina.flowmodelgenerator.core.converters.utils.ListOperationUtils.intersection;

public class FromJsonToRecord {

    private static final Gson gson = new Gson();

    private final List<String> existingFieldNames;
    private final Map<String, String> updatedFieldNames = new HashMap<>();
    private final Map<String, TypeDesc> typeDefinitions = new LinkedHashMap<>();

    private final String typePrefix;
    private final boolean isClosed;
    private final boolean asInlineType;

    private static final String ARRAY_RECORD_SUFFIX = "Item";
    private static final String JSON_KEYWORD = "json";
    private static final String NEW_RECORD_NAME = "NewRecord";

    public FromJsonToRecord(boolean isClosed, boolean isRecordTypeDesc, String typePrefix,
                            WorkspaceManager workspaceManager, Path filePath) {
        this.isClosed = isClosed;
        this.asInlineType = isRecordTypeDesc;
        this.typePrefix = typePrefix;
        this.existingFieldNames = getExistingTypeNames(workspaceManager, filePath);
    }

    public JsonElement convert(String jsonString, String name)
            throws JsonToRecordConverterException {

        JsonElement jsonElement = JsonParser.parseString(jsonString);

        String typeDefName = (name == null || name.isEmpty()) ? NEW_RECORD_NAME : name;

        if (jsonElement.isJsonObject()) {
            generateForNestedStructures(jsonElement, name, false, null);
        } else if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            generateNestedStructuresForArray(jsonArray, typeDefName);
            generateArray(jsonArray, typeDefName, null);
        } else {
            throw new JsonToRecordConverterException("JSON string parsing failed: Invalid JSON structure");
        }

        List<TypesManager.TypeDataWithRefs> typeDataList = new ArrayList<>();

        if (asInlineType) {
            Map.Entry<String, TypeDesc> mainTypeDef = typeDefinitions.entrySet().stream()
                    .filter(typeDesc -> typeDesc.getKey().equals(typeDefName))
                    .findFirst()
                    .orElseThrow(() -> new JsonToRecordConverterException("Error occurred while generating type"));
            TypeDesc typeDesc = convertToInlineTypeDesc(mainTypeDef.getValue());
            typeDataList.add(
                    new TypesManager.TypeDataWithRefs(
                            toTypeData(name, typeDesc, new TypeData.TypeDataBuilder()),
                            List.of()
                    )
            );
        } else {
            typeDefinitions.forEach((key, value) -> typeDataList.add(
                    new TypesManager.TypeDataWithRefs(
                            toTypeData(key, value, new TypeData.TypeDataBuilder()),
                            List.of()
                    )
            ));
        }

        SourceCodeGenerator srcGen = new SourceCodeGenerator();
        typeDataList.forEach(td -> {
            srcGen.generateCodeSnippetForType((TypeData) td.type());
        });

        return gson.toJsonTree(typeDataList);
    }

    private void generateNestedStructuresForArray(JsonArray jsonArray, String typeName) {
        jsonArray.forEach(element ->
                generateForNestedStructures(
                        element,
                        typeName + ARRAY_RECORD_SUFFIX,
                        true,
                        null
                )
        );
    }

    private void generateForNestedStructures(JsonElement jsonElement, String elementKey,
                                             boolean isArraySuffixAdded, String moveBefore) {
        String typeName = escapeIdentifier(StringUtils.capitalize(elementKey));
        String updatedTypeName = getAndUpdateFieldNames(
                typeName,
                isArraySuffixAdded,
                existingFieldNames,
                updatedFieldNames
        );
        if (jsonElement.isJsonObject()) {
            generateRecord(jsonElement.getAsJsonObject(), updatedTypeName, moveBefore);
        } else if (jsonElement.isJsonArray()) {
            generateNestedStructuresForArray(jsonElement.getAsJsonArray(), updatedTypeName);
            generateArray(jsonElement.getAsJsonArray(), updatedTypeName, moveBefore);
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
        List<Map.Entry<String, TypeDesc>> typeDescNodes = new ArrayList<>(typeDefinitions.entrySet());
        List<String> recordNames = typeDescNodes.stream().map(Map.Entry::getKey).toList();
        typeDescNodes.add(recordNames.indexOf(moveBefore), Map.entry(typeName, typeDesc));
        typeDefinitions.clear();
        typeDescNodes.forEach(node -> typeDefinitions.put(node.getKey(), node.getValue()));
    }

    private RecordTypeDesc createRecordTypeDesc(JsonObject jsonObject, String recordTypeName) {
        List<RecordField> recordFields = new ArrayList<>();

        if (typeDefinitions.containsKey(recordTypeName)) {
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (entry.getValue().isJsonArray() || entry.getValue().isJsonObject()) {
                    String name = getRecordName(entry.getKey());
                    generateForNestedStructures(entry.getValue(), name, false, name);
                }
                // TODO: Missed Step: Collect to jsonNodes
            }
            mergeWithExistingRecordFields(jsonObject, recordTypeName, recordFields);
        } else {
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (entry.getValue().isJsonObject() || entry.getValue().isJsonArray()) {
                    String name = getRecordName(entry.getKey());
                    generateForNestedStructures(entry.getValue(), name, false, null);
                }
                // TODO: Missed Step: Collect to jsonNodes
                RecordField recordField = createRecordField(entry, false);
                recordFields.add(recordField);
            }
            if (typeDefinitions.containsKey(recordTypeName)) {
                // Handle generation record field for nested object with same field name
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
                    typeName,
                    false,
                    existingFieldNames,
                    updatedFieldNames
            );
            ReferenceTypeDesc referenceTypeDesc = new ReferenceTypeDesc(updatedTypeName);
            return new RecordField(fieldName, referenceTypeDesc, isOptional);
        } else if (element.isJsonArray()) {
            String typeName = escapeIdentifier(getRecordName(fieldName.trim()));
            String updatedTypeName = getAndUpdateFieldNames(
                    typeName,
                    false,
                    existingFieldNames,
                    updatedFieldNames
            );
            ReferenceTypeDesc referenceTypeDesc = new ReferenceTypeDesc(updatedTypeName);
            return new RecordField(fieldName, referenceTypeDesc, isOptional);
        }
        return new RecordField(fieldName, null);
    }

    private ArrayTypeDesc createArrayTypeDesc(String fieldName, JsonArray jsonArray) {
        String typeName = getRecordName(fieldName);

        Iterator<JsonElement> iterator = jsonArray.iterator();
        List<TypeDesc> typeDescList = new ArrayList<>();

        while (iterator.hasNext()) {
            JsonElement element = iterator.next();
            if (element.isJsonPrimitive()) {
                String primitiveType = getPrimitiveType(element.getAsJsonPrimitive());
                PrimitiveTypeDesc typeDesc = new PrimitiveTypeDesc(primitiveType);

                if (!typeDescList.stream().map(TypeDesc::toString).toList().contains(typeDesc.toString())) {
                    typeDescList.add(typeDesc);
                }
            } else if (element.isJsonNull()) {
                String jsonType = JSON_KEYWORD;
                PrimitiveTypeDesc typeDesc = new PrimitiveTypeDesc(jsonType);

                if (!typeDescList.stream().map(TypeDesc::toString).toList().contains(jsonType)) {
                    typeDescList.add(typeDesc);
                }
            } else if (element.isJsonObject()) {
                String typeNameWithArrSuffix = escapeIdentifier(StringUtils.capitalize(typeName) + ARRAY_RECORD_SUFFIX);
                String updatedType = getAndUpdateFieldNames(
                        typeNameWithArrSuffix,
                        true,
                        existingFieldNames,
                        updatedFieldNames
                );
                ReferenceTypeDesc refTypeDesc = new ReferenceTypeDesc(updatedType);
                if (!typeDescList.stream().map(TypeDesc::toString).toList().contains(refTypeDesc.toString())) {
                    typeDescList.add(refTypeDesc);
                }
            } else if (element.isJsonArray()) {
                ArrayTypeDesc typeDesc = createArrayTypeDesc(fieldName, element.getAsJsonArray());
                if (!typeDescList.stream().map(TypeDesc::toString).toList().contains(typeDesc.toString())) {
                    typeDescList.add(typeDesc);
                }
            }
        }
        List<TypeDesc> sortedTypeDescList = List.copyOf(typeDescList); // TODO: Needs to be sorted
        TypeDesc typeDesc = createUnionType(sortedTypeDescList, false);
        return new ArrayTypeDesc(typeDesc);
    }

    private TypeDesc createUnionType(List<TypeDesc> typeDescList, boolean isOptional) {
        TypeDesc typeDesc;
        if (typeDescList.isEmpty()) {
            typeDesc = new PrimitiveTypeDesc(JSON_KEYWORD);
        } else if (typeDescList.size() == 1) {
            typeDesc = typeDescList.getFirst();
        } else {
            typeDesc = new UnionTypeDesc(typeDescList);
        }
        return isOptional ? new OptionalTypeDesc(typeDesc) : typeDesc;
    }

    private String getRecordName(String name) {
        String cap = StringUtils.capitalize(name);
        return typePrefix == null || typePrefix.isEmpty() ? cap : StringUtils.capitalize(typePrefix) + cap;
    }

    private void mergeWithExistingRecordFields(JsonObject jsonObject,
                                               String recordName,
                                               List<RecordField> recordFields) {
        // Get previous record type
        TypeDesc typeDesc = typeDefinitions.get(recordName);
        if (!(typeDesc instanceof RecordTypeDesc recordTypeDesc)) {
            return;
        }

        Map<String, RecordField> previousRecordFields = recordTypeDesc.fields.stream().collect(Collectors.toMap(
                recordField -> recordField.fieldName,
                recordField -> recordField
        ));
        Map<String, RecordField> newRecordFields = jsonObject.entrySet().stream()
                .map(entry -> createRecordField(entry, false))
                .toList().stream()
                .collect(Collectors.toMap(
                        recordField -> recordField.fieldName,
                        Function.identity(),
                        (val1, val2) -> val1,
                        LinkedHashMap::new
                ));

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
                // T1 x; T1 x?;
                RecordField updatedField = new RecordField(fieldName, f1.type, true);
                updatedRecordFields.add(updatedField);
                continue;
            }

            if (f1.type instanceof OptionalTypeDesc || f2.type instanceof OptionalTypeDesc) {
                // T1? x; T2 x; or T1 x; T2? x; or T1? x; T2? x;

                TypeDesc f1Type = getNonOptionalTypeDesc(f1.type);
                TypeDesc f2Type = getNonOptionalTypeDesc(f2.type);

                if (f1Type.toString().equals(f2Type.toString())) {
                    // |T1| = |T2|
                    OptionalTypeDesc optionalTypeDesc = new OptionalTypeDesc(f1Type);
                    RecordField updatedField = new RecordField(fieldName, optionalTypeDesc, isOptional);
                    updatedRecordFields.add(updatedField);
                    continue;
                }

                // |T1| != |T2|
                UnionTypeDesc unionTypeDesc = new UnionTypeDesc(List.of(f1Type, f2Type));
                OptionalTypeDesc optionalTypeDesc = new OptionalTypeDesc(unionTypeDesc);    // (T1|T2)?
                RecordField updatedField = new RecordField(fieldName, optionalTypeDesc, isOptional);
                updatedRecordFields.add(updatedField);
                continue;
            }

            // T1 x; T2 x;
            UnionTypeDesc unionTypeDesc = new UnionTypeDesc(List.of(f1.type, f2.type));
            RecordField updatedField = new RecordField(fieldName, unionTypeDesc, isOptional);
            updatedRecordFields.add(updatedField);
        }

        for (Map.Entry<String, RecordField> entry : differentRecordFields.entrySet()) {
            RecordField recordField = entry.getValue();
            recordField.isOptional = true;
            updatedRecordFields.add(recordField);
        }
    }

    private TypeDesc convertToInlineTypeDesc(TypeDesc mainTypeDesc) {
        switch (mainTypeDesc) {
            case RecordTypeDesc recordTypeDesc -> {
                List<RecordField> recordFields = new ArrayList<>();
                for (RecordField field : recordTypeDesc.fields) {
                    recordFields.add(new RecordField(
                            field.fieldName,
                            convertToInlineTypeDesc(field.type),
                            field.isOptional
                    ));
                }
                return new RecordTypeDesc(recordFields, this.isClosed);
            }
            case ArrayTypeDesc arrayTypeDesc -> {
                TypeDesc elementType = convertToInlineTypeDesc(arrayTypeDesc.elementType);
                return new ArrayTypeDesc(elementType);
            }
            case OptionalTypeDesc optionalTypeDesc -> {
                TypeDesc nonOptionalType = getNonOptionalTypeDesc(optionalTypeDesc.typeDesc);
                return new OptionalTypeDesc(convertToInlineTypeDesc(nonOptionalType));
            }
            case UnionTypeDesc unionTypeDesc -> {
                List<TypeDesc> inlineMembers = new ArrayList<>();
                for (TypeDesc member : unionTypeDesc.members) {
                    inlineMembers.add(convertToInlineTypeDesc(member));
                }
                return new UnionTypeDesc(inlineMembers);
            }
            case PrimitiveTypeDesc primitiveTypeDesc -> {
                return primitiveTypeDesc;
            }
            default -> {
                // Assume that the mainTypeDesc is a ReferenceTypeDesc
                ReferenceTypeDesc referenceTypeDesc = (ReferenceTypeDesc) mainTypeDesc;
                return convertToInlineTypeDesc(typeDefinitions.get(referenceTypeDesc.typeName));
            }
        }
    }

    private Object toTypeData(String name, TypeDesc typeDesc, TypeData.TypeDataBuilder typeDataBuilder) {
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


        switch (typeDesc) {
            case RecordTypeDesc recordTypeDesc -> {
                Member.MemberBuilder memberBuilder = new Member.MemberBuilder();
                List<Member> members = recordTypeDesc.fields.stream().map(
                        field -> memberBuilder
                                .kind(Member.MemberKind.FIELD)
                                .type(toTypeData(null, field.type, new TypeData.TypeDataBuilder()))
                                .name(field.fieldName)
                                .refs(List.of())
                                .optional(field.isOptional)
                                .build()
                ).toList();
                return typeDataBuilder
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
                        .type(toTypeData(null, arrayTypeDesc.elementType, new TypeData.TypeDataBuilder()))
                        .build();
                return typeDataBuilder
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
                List<Member> members = unionTypeDesc.members.stream().map(
                        member -> memberBuilder
                                .kind(Member.MemberKind.TYPE)
                                .type(toTypeData(null, member, new TypeData.TypeDataBuilder()))
                                .build()
                ).toList();
                return typeDataBuilder
                        .members(members)
                        .codedata()
                            .node(NodeKind.UNION)
                            .stepOut()
                        .build();
            }
            case OptionalTypeDesc optionalTypeDesc -> {
                TypeDesc nonOptionalTypeDesc = getNonOptionalTypeDesc(optionalTypeDesc.typeDesc);
                String typeAsString = nonOptionalTypeDesc.toString();
                String bracketsTemplate = "(%s)?";

                // TODO: There is no way to represent `T?` in TypeData except as a union-type,
                //  hence using type as string
                return switch (nonOptionalTypeDesc) {
                    case UnionTypeDesc unionTypeDesc -> bracketsTemplate.formatted(unionTypeDesc.toString());
                    case ArrayTypeDesc arrayTypeDesc -> bracketsTemplate.formatted(arrayTypeDesc.toString());
                    default -> typeAsString + "?";
                };
            }
            case PrimitiveTypeDesc primitiveTypeDesc -> {
                return primitiveTypeDesc.typeName;
            }
            case ReferenceTypeDesc referenceTypeDesc -> {
                return referenceTypeDesc.typeName;
            }
            default -> {
                return typeDesc.toString();
            }
        }
    }

    // Get non-optional type description
    // Ex: T1[] -> T1[]
    // Ex: T1 | T2 -> T1 | T2
    // Ex: T? -> T
    // Ex: (T?)? -> T
    // Ex: (T1 | T2)? -> T1 | T2
    // Ex: (T1 | T2)? | T3? -> T1 | T2 | T3
    private TypeDesc getNonOptionalTypeDesc(TypeDesc typeDesc) {
        if (typeDesc instanceof OptionalTypeDesc optionalTypeDesc) {
            return optionalTypeDesc.typeDesc instanceof OptionalTypeDesc
                    ? getNonOptionalTypeDesc(optionalTypeDesc.typeDesc)
                    : optionalTypeDesc.typeDesc;
        }

        if (typeDesc instanceof UnionTypeDesc unionTypeDesc) {
            List<TypeDesc> nonOptionalMembers = unionTypeDesc.members.stream()
                    .map(this::getNonOptionalTypeDesc)
                    .toList();
            return new UnionTypeDesc(nonOptionalMembers);
        }

        return typeDesc;
    }

    private String getPrimitiveType(JsonPrimitive value) {
        if (value.isString()) {
            return "string";
        } else if (value.isBoolean()) {
            return "boolean";
        } else if (value.isNumber()) {
            String strValue = value.getAsNumber().toString();
            if (strValue.contains(".")) {
                return "decimal";
            } else {
                return "int";
            }
        }
        return "anydata";
    }

    private static class RecordField {
        String fieldName;
        TypeDesc type;
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

    private static class OptionalTypeDesc extends TypeDesc {
        TypeDesc typeDesc;

        OptionalTypeDesc(TypeDesc typeDesc) {
            this.typeDesc = typeDesc;
        }

        @Override
        public String toString() {
            return typeDesc.toString() + "?";
        }
    }

    private static class ArrayTypeDesc extends TypeDesc {
        TypeDesc elementType;

        ArrayTypeDesc(TypeDesc elementType) {
            this.elementType = elementType;
        }

        @Override
        public String toString() {
            return (elementType instanceof UnionTypeDesc ? "(" + elementType + ")" : elementType.toString()) + "[]";
        }
    }

    private static class RecordTypeDesc extends TypeDesc {
        List<RecordField> fields;

        private final boolean isClosed;

        RecordTypeDesc(List<RecordField> fields, boolean isClosed) {
            this.fields = fields;
            this.isClosed = isClosed;
        }

        @Override
        public String toString() {
            String template = isClosed ? "record {|%s|}" : "record {%s}";
            return template.formatted(
                    fields.stream()
                            .map(RecordField::toString)
                            .reduce("", (a, b) -> a + b)
            );
//            return "record " + "{" + fields.stream()
//                    .map(RecordField::toString)
//                    .reduce("", (a, b) -> a + b) + "}";
        }
    }

    private static class UnionTypeDesc extends TypeDesc {
        List<TypeDesc> members;

        UnionTypeDesc(List<TypeDesc> members) {
            this.members = members;
        }

        @Override
        public String toString() {
            return String.join("|", members.stream().map(TypeDesc::toString).toList());
        }
    }

    private static class ReferenceTypeDesc extends TypeDesc {
        String typeName;

        ReferenceTypeDesc(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public String toString() {
            return typeName;
        }
    }

    private static class PrimitiveTypeDesc extends TypeDesc {
        String typeName;

        PrimitiveTypeDesc(String typeName) {
            this.typeName = typeName;
        }

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
