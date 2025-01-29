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

package io.ballerina.flowmodelgenerator.core.converters;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.ArrayDimensionNode;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParenthesisedTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordRestDescriptorNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.flowmodelgenerator.core.TypesManager;
import io.ballerina.flowmodelgenerator.core.converters.exception.JsonToRecordConverterException;
import io.ballerina.flowmodelgenerator.core.model.TypeData;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import org.apache.commons.lang3.StringUtils;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;
import org.ballerinalang.formatter.core.options.ForceFormattingOptions;
import org.ballerinalang.formatter.core.options.FormattingOptions;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.escapeIdentifier;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.extractArrayTypeDescNode;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.extractTypeDescriptorNodes;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.extractUnionTypeDescNode;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.getAndUpdateFieldNames;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.getExistingTypeNames;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.getPrimitiveTypeName;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.getNumberOfDimensions;
import static io.ballerina.flowmodelgenerator.core.converters.utils.JsonToRecordMapperConverterUtils.sortTypeDescriptorNodes;
import static io.ballerina.flowmodelgenerator.core.converters.utils.ListOperationUtils.difference;
import static io.ballerina.flowmodelgenerator.core.converters.utils.ListOperationUtils.intersection;

/**
 * APIs for direct conversion from JSON strings to Ballerina records.
 *
 * @since 2.0.0
 */
public final class JsonToRecordMapper {

    private static final Gson gson = new Gson();
    private final String recordName;
    private final String prefix;
    private final Project project;
    private final Document document;
    private final Path filePath;
    private final TypesManager typesManager;

    public JsonToRecordMapper(String recordName, String prefix, Project project, Document document, Path filePath,
                              TypesManager typesManager) {
        this.recordName = recordName;
        this.prefix = prefix;
        this.project = project;
        this.document = document;
        this.filePath = filePath;
        this.typesManager = typesManager;
    }

    private static final String NEW_RECORD_NAME = "NewRecord";
    private static final String ARRAY_RECORD_SUFFIX = "Item";

    /**
     * This method returns the Ballerina code for the provided JSON value or the diagnostics.
     *
     * @param jsonString              JSON string of the JSON value to be converted to Ballerina record
     * @param isRecordTypeDesc        To denote final record, a record type descriptor (In line records)
     * @param isClosed                To denote whether the response record is closed or not
     * @param forceFormatRecordFields To denote whether the inline records to be formatted for multi-line or in-line
     * @param workspaceManager        Workspace manager instance
     * @param isNullAsOptional        To denote whether the null values in the JSON should be considered as optional
     *                                fields
     * @return Record types
     */
    public JsonElement convert(String jsonString, boolean isRecordTypeDesc, boolean isClosed,
                               boolean forceFormatRecordFields, WorkspaceManager workspaceManager,
                               boolean isNullAsOptional)
            throws JsonToRecordConverterException, FormatterException {
        List<String> existingFieldNames = getExistingTypeNames(workspaceManager, this.filePath);
        Map<String, String> updatedFieldNames = new HashMap<>();
        Map<String, NonTerminalNode> recordToTypeDescNodes = new LinkedHashMap<>();
        Map<String, JsonElement> jsonFieldToElements = new LinkedHashMap<>();

        if (existingFieldNames.contains(this.recordName)) {
            throw new JsonToRecordConverterException("Given record name already exists in the module");
        }
        JsonElement parsedJson = JsonParser.parseString(jsonString);
        if (parsedJson.isJsonObject()) {
            generateRecords(parsedJson.getAsJsonObject(), null, isClosed, recordToTypeDescNodes, null,
                    jsonFieldToElements, existingFieldNames, updatedFieldNames, isNullAsOptional);
        } else if (parsedJson.isJsonArray()) {
            JsonObject object = new JsonObject();
            object.add(((this.recordName == null) || this.recordName.isEmpty()) ?
                    StringUtils.uncapitalize(NEW_RECORD_NAME) :
                    StringUtils.uncapitalize(this.recordName), parsedJson);
            generateRecords(object, null, isClosed, recordToTypeDescNodes, null, jsonFieldToElements,
                    existingFieldNames, updatedFieldNames, isNullAsOptional);
        } else {
            throw new JsonToRecordConverterException("Error occurred while parsing the JSON string");
        }

        NodeList<ImportDeclarationNode> imports = AbstractNodeFactory.createEmptyNodeList();
        Set<String> typeNames = new HashSet<>();
        List<TypeDefinitionNode> typeDefNodes = recordToTypeDescNodes.entrySet().stream()
                .map(entry -> {
                    Token typeKeyWord = AbstractNodeFactory.createToken(SyntaxKind.TYPE_KEYWORD);
                    String recordTypeName = entry.getKey() == null ?
                            (this.recordName == null || this.recordName.isEmpty()) ?
                                    getAndUpdateFieldNames(NEW_RECORD_NAME, false,
                                            existingFieldNames, updatedFieldNames)
                                    : escapeIdentifier(StringUtils.capitalize(this.recordName)) : entry.getKey();
                    IdentifierToken typeName = AbstractNodeFactory
                            .createIdentifierToken(recordTypeName);
                    Token semicolon = AbstractNodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN);
                    typeNames.add(recordTypeName);
                    return NodeFactory.createTypeDefinitionNode(null, null, typeKeyWord, typeName,
                            entry.getValue(), semicolon);
                }).toList();

        NodeList<ModuleMemberDeclarationNode> moduleMembers;
        if (isRecordTypeDesc) {
            Optional<TypeDefinitionNode> lastTypeDefNode = convertToInlineRecord(typeDefNodes);
            moduleMembers = lastTypeDefNode
                    .<NodeList<ModuleMemberDeclarationNode>>map(AbstractNodeFactory::createNodeList)
                    .orElseGet(AbstractNodeFactory::createEmptyNodeList);
        } else {
            moduleMembers = AbstractNodeFactory.createNodeList(new ArrayList<>(typeDefNodes));
        }

        Token eofToken = AbstractNodeFactory.createIdentifierToken("");
        ModulePartNode modulePartNode = NodeFactory.createModulePartNode(imports, moduleMembers, eofToken);
        ForceFormattingOptions forceFormattingOptions = ForceFormattingOptions.builder()
                .setForceFormatRecordFields(forceFormatRecordFields).build();
        FormattingOptions formattingOptions = FormattingOptions.builder()
                .setForceFormattingOptions(forceFormattingOptions).build();
        String str = Formatter.format(modulePartNode.syntaxTree(), formattingOptions).toSourceCode();
        // TODO: Check this
        Document modifiedDoc =
                project.duplicate().currentPackage().module(document.module().moduleId())
                        .document(document.documentId()).modify().withContent(str).apply();
        SemanticModel semanticModel = modifiedDoc.module().getCompilation().getSemanticModel();

        List<TypesManager.TypeDataWithRefs> typeDataList = new ArrayList<>();
        for (Symbol symbol : semanticModel.moduleSymbols()) {
            if (symbol.kind() == SymbolKind.TYPE_DEFINITION) {
                TypeDefinitionSymbol typeDefSymbol = (TypeDefinitionSymbol) symbol;
                if (typeNames.contains(typeDefSymbol.getName().get())) {
                    typeDataList.add(typesManager.getTypeDataWithRefs(typeDefSymbol));
                }
            }
        }
        return gson.toJsonTree(typeDataList);
    }

    /**
     * This method generates the TypeDescriptorNodes for the parsed JSON value.
     *
     * @param jsonObject            JSON object node that has to be generated as Ballerina record
     * @param recordName            Name of the generated record
     * @param isClosed              To denote whether the response record is closed or not
     * @param recordToTypeDescNodes The map of recordNames and the TypeDescriptorNodes already generated
     * @param moveBefore            To move generated TypeDescriptorNode before specified TypeDescriptorNode
     * @param jsonNodes             The map of JSON field names and the JSON nodes for already created
     *                              TypeDescriptorNodes
     * @param existingFieldNames    The list of already existing record names in the ModulePartNode
     * @param updatedFieldNames     The map of updated record names for already existing record names in the
     *                              ModulePartNode
     * @param isNullAsOptional      To denote whether the null values in the JSON should be considered as optional
     *                              fields
     */
    private void generateRecords(JsonObject jsonObject, String recordName, boolean isClosed,
                                 Map<String, NonTerminalNode> recordToTypeDescNodes, String moveBefore,
                                 Map<String, JsonElement> jsonNodes,
                                 List<String> existingFieldNames,
                                 Map<String, String> updatedFieldNames,
                                 boolean isNullAsOptional) throws JsonToRecordConverterException {
        Token recordKeyWord = AbstractNodeFactory.createToken(SyntaxKind.RECORD_KEYWORD);
        Token bodyStartDelimiter = AbstractNodeFactory.createToken(SyntaxKind.OPEN_BRACE_PIPE_TOKEN);

        List<Node> recordFields = new ArrayList<>();
        if (recordToTypeDescNodes.containsKey(recordName)) {
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (entry.getValue().isJsonObject() || entry.getValue().isJsonArray()) {
                    String name = getRecordName(prefix, entry.getKey());
                    generateRecordForObjAndArray(entry.getValue(), name, isClosed, recordToTypeDescNodes,
                            recordName, jsonNodes, existingFieldNames, updatedFieldNames, false,
                            isNullAsOptional);
                }
                jsonNodes.put(entry.getKey(), entry.getValue());
            }
            prepareAndUpdateRecordFields(jsonObject, recordName, jsonNodes, recordToTypeDescNodes,
                    recordFields, existingFieldNames, updatedFieldNames, false, isNullAsOptional);
        } else {
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (entry.getValue().isJsonObject() || entry.getValue().isJsonArray()) {
                    String name = getRecordName(prefix, entry.getKey());
                    generateRecordForObjAndArray(entry.getValue(), name, isClosed, recordToTypeDescNodes,
                            null, jsonNodes, existingFieldNames, updatedFieldNames, false,
                            isNullAsOptional);
                }
                jsonNodes.put(entry.getKey(), entry.getValue());
                Node recordField = getRecordField(entry, existingFieldNames, updatedFieldNames, false);
                recordFields.add(recordField);
            }
            if (recordToTypeDescNodes.containsKey(recordName)) {
                recordFields.clear();
                prepareAndUpdateRecordFields(jsonObject, recordName, jsonNodes, recordToTypeDescNodes,
                        recordFields, existingFieldNames, updatedFieldNames, true,
                        isNullAsOptional);
            }
        }

        NodeList<Node> fieldNodes = AbstractNodeFactory.createNodeList(recordFields);
        Token bodyEndDelimiter = AbstractNodeFactory.createToken(SyntaxKind.CLOSE_BRACE_PIPE_TOKEN);
        RecordRestDescriptorNode restDescriptorNode = isClosed ? null :
                NodeFactory.createRecordRestDescriptorNode(
                        NodeFactory.createBuiltinSimpleNameReferenceNode(SyntaxKind.JSON_KEYWORD,
                                AbstractNodeFactory.createToken(SyntaxKind.JSON_KEYWORD)),
                        AbstractNodeFactory.createToken(SyntaxKind.ELLIPSIS_TOKEN),
                        AbstractNodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));
        RecordTypeDescriptorNode recordTypeDescriptorNode =
                NodeFactory.createRecordTypeDescriptorNode(recordKeyWord, bodyStartDelimiter,
                        fieldNodes, restDescriptorNode, bodyEndDelimiter);

        if (moveBefore == null || moveBefore.equals(recordName)) {
            recordToTypeDescNodes.put(recordName, recordTypeDescriptorNode);
        } else {
            List<Map.Entry<String, NonTerminalNode>> typeDescNodes = new ArrayList<>(recordToTypeDescNodes.entrySet());
            List<String> recordNames = typeDescNodes.stream().map(Map.Entry::getKey).toList();
            typeDescNodes.add(recordNames.indexOf(moveBefore), Map.entry(recordName, recordTypeDescriptorNode));
            recordToTypeDescNodes.clear();
            typeDescNodes.forEach(node -> recordToTypeDescNodes.put(node.getKey(), node.getValue()));
        }
    }

    private void generateRecordForObjAndArray(JsonElement jsonElement, String elementKey, boolean isClosed,
                                              Map<String, NonTerminalNode> recordToTypeDescNodes,
                                              String moveBefore, Map<String, JsonElement> jsonNodes,
                                              List<String> existingFieldNames,
                                              Map<String, String> updatedFieldNames,
                                              boolean arraySuffixAdded,
                                              boolean isNullAsOptional) throws JsonToRecordConverterException {
        if (jsonElement.isJsonObject()) {
            String type = escapeIdentifier(StringUtils.capitalize(elementKey));
            String updatedType = getAndUpdateFieldNames(type, arraySuffixAdded, existingFieldNames, updatedFieldNames);
            generateRecords(jsonElement.getAsJsonObject(), updatedType, isClosed, recordToTypeDescNodes,
                    moveBefore, jsonNodes, existingFieldNames, updatedFieldNames, isNullAsOptional);
        } else if (jsonElement.isJsonArray()) {
            for (JsonElement element : jsonElement.getAsJsonArray()) {
                String arrayElementKey = elementKey + (arraySuffixAdded ? "" : ARRAY_RECORD_SUFFIX);
                generateRecordForObjAndArray(element, arrayElementKey, isClosed, recordToTypeDescNodes, moveBefore,
                        jsonNodes, existingFieldNames, updatedFieldNames, true, isNullAsOptional);
            }
        }
    }

    /**
     * This method prepares the existing fields and new fields to generate updated record fields.
     *
     * @param jsonObject                JSON object node that has to be generated as Ballerina record
     * @param recordName                Name of the generated record
     * @param recordToTypeDescNodes     The map of recordNames and the TypeDescriptorNodes already generated
     * @param jsonNodes                 The map of JSON field names and the JSON nodes for already created
     *                                  TypeDescriptorNodes
     *                                  //     * @param diagnosticMessages        The list of diagnostic messages
     *                                  generated by the method
     * @param recordFields              The list generated record fields
     * @param existingFieldNames        The list of already existing record names in the ModulePartNode
     * @param updatedFieldNames         The map of updated record names for already existing record names in the
     *                                  ModulePartNode
     * @param prepareForNestedSameField To denote if the fields are being prepared for normal processing or
     *                                  for nested JSON with same field
     * @param isNullAsOptional          To denote whether the null values in the JSON should be considered as
     *                                  optional fields
     */
    private void prepareAndUpdateRecordFields(JsonObject jsonObject, String recordName,
                                              Map<String, JsonElement> jsonNodes,
                                              Map<String, NonTerminalNode> recordToTypeDescNodes,
                                              List<Node> recordFields, List<String> existingFieldNames,
                                              Map<String, String> updatedFieldNames,
                                              boolean prepareForNestedSameField, boolean isNullAsOptional)
            throws JsonToRecordConverterException {
        RecordTypeDescriptorNode previousRecordTypeDescriptorNode =
                (RecordTypeDescriptorNode) recordToTypeDescNodes.get(recordName);
        List<RecordFieldNode> previousRecordFields = previousRecordTypeDescriptorNode.fields().stream()
                .map(node -> (RecordFieldNode) node).toList();
        Map<String, RecordFieldNode> previousRecordFieldToNodes = previousRecordFields.stream()
                .collect(Collectors.toMap(node -> node.fieldName().text(), Function.identity(),
                        (val1, val2) -> val1, LinkedHashMap::new));
        Map<String, RecordFieldNode> newRecordFieldToNodes = jsonObject.entrySet().stream()
                .map(entry ->
                        (RecordFieldNode) getRecordField(entry, existingFieldNames, updatedFieldNames, false))
                .toList().stream()
                .collect(Collectors.toMap(node -> node.fieldName().text(), Function.identity(),
                        (val1, val2) -> val1, LinkedHashMap::new));
        if (prepareForNestedSameField) {
            updateRecordFields(jsonObject, jsonNodes, recordFields, existingFieldNames,
                    updatedFieldNames, newRecordFieldToNodes, previousRecordFieldToNodes, isNullAsOptional);
        } else {
            updateRecordFields(jsonObject, jsonNodes, recordFields, existingFieldNames,
                    updatedFieldNames, previousRecordFieldToNodes, newRecordFieldToNodes, isNullAsOptional);
        }
    }

    /**
     * This method updates the record fields already generated, if the fields are optional.
     *
     * @param jsonObject                 JSON object node that has to be generated as Ballerina record
     * @param jsonNodes                  The map of JSON field names and the JSON nodes for already created
     *                                   TypeDescriptorNodes
     *                                   //     * @param diagnosticMessages         The list of diagnostic messages
     *                                   generated by the method
     * @param recordFields               The list generated record fields
     * @param existingFieldNames         The list of already existing record names in the ModulePartNode
     * @param updatedFieldNames          The map of updated record names for already existing record names in the
     *                                   ModulePartNode
     * @param previousRecordFieldToNodes The list of already generated field nodes
     * @param newRecordFieldToNodes      The list of newly generated field nodes for the same record
     * @param isNullAsOptional           To denote whether the null values in the JSON should be considered as
     *                                   optional fields
     */
    private void updateRecordFields(JsonObject jsonObject, Map<String, JsonElement> jsonNodes,
                                    List<Node> recordFields,
                                    List<String> existingFieldNames,
                                    Map<String, String> updatedFieldNames,
                                    Map<String, RecordFieldNode> previousRecordFieldToNodes,
                                    Map<String, RecordFieldNode> newRecordFieldToNodes,
                                    boolean isNullAsOptional) throws JsonToRecordConverterException {
        Map<String, Map.Entry<RecordFieldNode, RecordFieldNode>> intersectingRecordFields =
                intersection(previousRecordFieldToNodes, newRecordFieldToNodes);
        Map<String, RecordFieldNode> differencingRecordFields =
                difference(previousRecordFieldToNodes, newRecordFieldToNodes);

        for (Map.Entry<String, Map.Entry<RecordFieldNode, RecordFieldNode>> entry :
                intersectingRecordFields.entrySet()) {
            boolean isOptional = entry.getValue().getKey().questionMarkToken().isPresent();
            Map<String, String> jsonEscapedFieldToFields = jsonNodes.entrySet().stream()
                    .collect(Collectors.toMap(jsonEntry -> escapeIdentifier(jsonEntry.getKey()), Map.Entry::getKey));
            Map.Entry<String, JsonElement> jsonEntry = new AbstractMap.SimpleEntry<>(jsonEscapedFieldToFields
                    .get(entry.getKey()), jsonNodes.get(jsonEscapedFieldToFields.get(entry.getKey())));
            if (!entry.getValue().getKey().typeName().toSourceCode()
                    .equals(entry.getValue().getValue().typeName().toSourceCode())) {
                TypeDescriptorNode node1 = (TypeDescriptorNode) entry.getValue().getKey().typeName();
                TypeDescriptorNode node2 = (TypeDescriptorNode) entry.getValue().getValue().typeName();

                TypeDescriptorNode nonJsonDataNode = null;
                IdentifierToken optionalFieldName = null;
                boolean alreadyOptionalTypeDesc = false;

                if (node1.kind().equals(SyntaxKind.OPTIONAL_TYPE_DESC)) {
                    OptionalTypeDescriptorNode optionalTypeDescNode = (OptionalTypeDescriptorNode) node1;
                    node1 = (TypeDescriptorNode) optionalTypeDescNode.typeDescriptor();
                    alreadyOptionalTypeDesc = true;
                } else if (node2.kind().equals(SyntaxKind.OPTIONAL_TYPE_DESC)) {
                    OptionalTypeDescriptorNode optionalTypeDescNode = (OptionalTypeDescriptorNode) node2;
                    node2 = (TypeDescriptorNode) optionalTypeDescNode.typeDescriptor();
                    alreadyOptionalTypeDesc = true;
                } else if ((node1.kind().equals(SyntaxKind.JSON_KEYWORD) ||
                        node2.kind().equals(SyntaxKind.JSON_KEYWORD))) {
                    if (isNullAsOptional) {
                        nonJsonDataNode = NodeParser.parseTypeDescriptor(node1.kind().equals(SyntaxKind.JSON_KEYWORD)
                                ? node2.toSourceCode() : node1.toSourceCode());
                        optionalFieldName = AbstractNodeFactory.createIdentifierToken(entry.getKey() +
                                SyntaxKind.QUESTION_MARK_TOKEN.stringValue());
                    } else {
                        nonJsonDataNode = NodeParser.parseTypeDescriptor(node1.kind().equals(SyntaxKind.JSON_KEYWORD)
                                ? node2.toSourceCode() + SyntaxKind.QUESTION_MARK_TOKEN.stringValue() :
                                node1.toSourceCode() + SyntaxKind.QUESTION_MARK_TOKEN.stringValue());
                    }
                }

                List<TypeDescriptorNode> typeDescNodesSorted =
                        sortTypeDescriptorNodes(extractTypeDescriptorNodes(List.of(node1, node2)));
                TypeDescriptorNode unionTypeDescNode =
                        createUnionTypeDescriptorNode(typeDescNodesSorted, alreadyOptionalTypeDesc);

                RecordFieldNode recordField =
                        (RecordFieldNode) getRecordField(jsonEntry, existingFieldNames, updatedFieldNames, isOptional);
                recordField = recordField.modify()
                        .withTypeName(nonJsonDataNode == null ? unionTypeDescNode : nonJsonDataNode)
                        .withFieldName(optionalFieldName == null ? recordField.fieldName() : optionalFieldName)
                        .apply();
                recordFields.add(recordField);
            } else {
                Node recordField = getRecordField(jsonEntry, existingFieldNames, updatedFieldNames, isOptional);
                recordFields.add(recordField);
            }
        }

        for (Map.Entry<String, RecordFieldNode> entry : differencingRecordFields.entrySet()) {
            String jsonField = entry.getKey();
            Map<String, String> jsonEscapedFieldToFields = jsonNodes.entrySet().stream()
                    .collect(Collectors.toMap(jsonEntry -> escapeIdentifier(jsonEntry.getKey()), Map.Entry::getKey));
            JsonElement jsonElement = jsonNodes.get(jsonEscapedFieldToFields.get(jsonField));
            Map.Entry<String, JsonElement> jsonEntry = jsonElement != null ?
                    new AbstractMap.SimpleEntry<>(jsonEscapedFieldToFields.get(jsonField), jsonElement) :
                    jsonObject.entrySet().stream().filter(elementEntry -> escapeIdentifier(elementEntry.getKey())
                            .equals(jsonField)).findFirst().orElse(null);
            if (jsonEntry != null) {
                Node recordField = getRecordField(jsonEntry, existingFieldNames, updatedFieldNames, true);
                recordFields.add(recordField);
            } else {
                /*
                  This else bloc is unreachable as, there is no way jsonEntry can become null. But since it is an
                  optional field this else bloc is written to capture error and store in diagnostics.
                 */
                throw new JsonToRecordConverterException("Error occurred while updating the record fields");
            }
        }
    }

    /**
     * This method generates the record fields for the corresponding JSON fields.
     *
     * @param entry              Map entry of a JSON field name and the corresponding JSON element
     * @param existingFieldNames The list of already existing record names in the ModulePartNode
     * @param updatedFieldNames  The map of updated record names for already existing record names in the ModulePartNode
     * @param isOptionalField    To denote whether the record field is optional or not
     * @return {@link Node} Record field node for the corresponding JSON field
     */
    private Node getRecordField(Map.Entry<String, JsonElement> entry, List<String> existingFieldNames,
                                Map<String, String> updatedFieldNames,
                                boolean isOptionalField) {
        Token typeName = AbstractNodeFactory.createToken(SyntaxKind.JSON_KEYWORD);
        Token questionMarkToken = AbstractNodeFactory.createToken(SyntaxKind.QUESTION_MARK_TOKEN);
        TypeDescriptorNode fieldTypeName = NodeFactory.createBuiltinSimpleNameReferenceNode(typeName.kind(), typeName);
        IdentifierToken fieldName = AbstractNodeFactory.createIdentifierToken(escapeIdentifier(entry.getKey().trim()));
        Token optionalFieldToken = isOptionalField ? questionMarkToken : null;
        Token semicolonToken = AbstractNodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN);

        RecordFieldNode recordFieldNode = NodeFactory.createRecordFieldNode(null, null,
                fieldTypeName, fieldName, optionalFieldToken, semicolonToken);

        if (entry.getValue().isJsonPrimitive()) {
            typeName = getPrimitiveTypeName(entry.getValue().getAsJsonPrimitive());
            fieldTypeName = NodeFactory.createBuiltinSimpleNameReferenceNode(typeName.kind(), typeName);
            recordFieldNode = NodeFactory.createRecordFieldNode(null, null,
                    fieldTypeName, fieldName,
                    optionalFieldToken, semicolonToken);
        } else if (entry.getValue().isJsonObject()) {
            String type = escapeIdentifier(getRecordName(prefix, entry.getKey().trim()));
            String updatedType = getAndUpdateFieldNames(type, false, existingFieldNames, updatedFieldNames);
            typeName = AbstractNodeFactory.createIdentifierToken(updatedType);
            fieldTypeName = NodeFactory.createBuiltinSimpleNameReferenceNode(typeName.kind(), typeName);
            recordFieldNode = NodeFactory.createRecordFieldNode(null, null,
                    fieldTypeName, fieldName,
                    optionalFieldToken, semicolonToken);
        } else if (entry.getValue().isJsonArray()) {
            Map.Entry<String, JsonArray> jsonArrayEntry = Map.entry(getRecordName(prefix, entry.getKey()),
                    entry.getValue().getAsJsonArray());
            ArrayTypeDescriptorNode arrayTypeName =
                    getArrayTypeDescriptorNode(jsonArrayEntry, existingFieldNames, updatedFieldNames);
            recordFieldNode = NodeFactory.createRecordFieldNode(null, null,
                    arrayTypeName, fieldName,
                    optionalFieldToken, semicolonToken);
        }
        return recordFieldNode;
    }

    /**
     * This method converts the list of TypeDefinitionNodes into single inline TypeDefinitionNode.
     *
     * @param typeDefNodes List of TypeDefinitionNodes that has to be converted into inline TypeDefinitionNode.
     * @return {@link TypeDefinitionNode} The converted inline TypeDefinitionNode.
     */
    private static Optional<TypeDefinitionNode> convertToInlineRecord(List<TypeDefinitionNode> typeDefNodes) {
        Map<String, RecordTypeDescriptorNode> visitedRecordTypeDescNodeTypeToNodes = new LinkedHashMap<>();
        for (TypeDefinitionNode typeDefNode : typeDefNodes) {
            RecordTypeDescriptorNode recordTypeDescNode = (RecordTypeDescriptorNode) typeDefNode.typeDescriptor();
            List<RecordFieldNode> recordFieldNodes = recordTypeDescNode.fields().stream()
                    .map(node -> (RecordFieldNode) node).toList();
            List<Node> intermediateRecordFieldNodes = new ArrayList<>();
            for (RecordFieldNode recordFieldNode : recordFieldNodes) {
                TypeDescriptorNode fieldTypeName = (TypeDescriptorNode) recordFieldNode.typeName();
                TypeDescriptorNode converted =
                        convertUnionTypeToInline(fieldTypeName, visitedRecordTypeDescNodeTypeToNodes);
                if (converted == null) {
                    return Optional.empty();
                }
                Token semicolonToken = AbstractNodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN);
                RecordFieldNode updatedRecordFieldNode = NodeFactory.createRecordFieldNode(null, null,
                        converted, recordFieldNode.fieldName(),
                        recordFieldNode.questionMarkToken().orElse(null), semicolonToken);
                intermediateRecordFieldNodes.add(updatedRecordFieldNode);
            }
            NodeList<Node> updatedRecordFieldNodes = AbstractNodeFactory.createNodeList(intermediateRecordFieldNodes);
            RecordTypeDescriptorNode updatedRecordTypeDescNode =
                    recordTypeDescNode.modify().withFields(updatedRecordFieldNodes).apply();
            visitedRecordTypeDescNodeTypeToNodes.put(typeDefNode.typeName().toSourceCode(), updatedRecordTypeDescNode);
        }

        List<Map.Entry<String, RecordTypeDescriptorNode>> visitedRecordTypeDescNodes =
                new ArrayList<>(visitedRecordTypeDescNodeTypeToNodes.entrySet());
        Map.Entry<String, RecordTypeDescriptorNode> lastRecordTypeDescNode =
                visitedRecordTypeDescNodes.get(visitedRecordTypeDescNodes.size() - 1);
        Token typeKeyWord = AbstractNodeFactory.createToken(SyntaxKind.TYPE_KEYWORD);
        IdentifierToken typeName =
                AbstractNodeFactory.createIdentifierToken(escapeIdentifier(lastRecordTypeDescNode.getKey()));
        Token semicolon = AbstractNodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN);
        return Optional.of(NodeFactory.createTypeDefinitionNode(null, null, typeKeyWord, typeName,
                lastRecordTypeDescNode.getValue(), semicolon));
    }

    /**
     * This method generates the record fields for the corresponding JSON fields if it's an array.
     *
     * @param entry              Map entry of a JSON field name and the corresponding JSON element
     * @param existingFieldNames The list of already existing record names in the ModulePartNode
     * @param updatedFieldNames  The map of updated record names for already existing record names in the ModulePartNode
     * @return {@link ArrayTypeDescriptorNode} Record field node for the corresponding JSON array field
     */
    private static ArrayTypeDescriptorNode getArrayTypeDescriptorNode(Map.Entry<String, JsonArray> entry,
                                                                      List<String> existingFieldNames,
                                                                      Map<String, String> updatedFieldNames) {
        Token openSBracketToken = AbstractNodeFactory.createToken(SyntaxKind.OPEN_BRACKET_TOKEN);
        Token closeSBracketToken = AbstractNodeFactory.createToken(SyntaxKind.CLOSE_BRACKET_TOKEN);

        Iterator<JsonElement> iterator = entry.getValue().iterator();
        List<TypeDescriptorNode> typeDescriptorNodes = new ArrayList<>();
        while (iterator.hasNext()) {
            JsonElement element = iterator.next();
            if (element.isJsonPrimitive()) {
                Token tempTypeName = getPrimitiveTypeName(element.getAsJsonPrimitive());
                TypeDescriptorNode tempTypeNode =
                        NodeFactory.createBuiltinSimpleNameReferenceNode(tempTypeName.kind(), tempTypeName);
                if (!typeDescriptorNodes.stream().map(Node::toSourceCode)
                        .toList().contains(tempTypeNode.toSourceCode())) {
                    typeDescriptorNodes.add(tempTypeNode);
                }
            } else if (element.isJsonNull()) {
                Token tempTypeName = AbstractNodeFactory.createToken(SyntaxKind.JSON_KEYWORD);
                TypeDescriptorNode tempTypeNode =
                        NodeFactory.createBuiltinSimpleNameReferenceNode(tempTypeName.kind(), tempTypeName);
                if (!typeDescriptorNodes.stream().map(Node::toSourceCode)
                        .toList().contains(tempTypeNode.toSourceCode())) {
                    typeDescriptorNodes.add(tempTypeNode);
                }
            } else if (element.isJsonObject()) {
                String elementKey = entry.getKey();
                String type = escapeIdentifier(StringUtils.capitalize(elementKey) + ARRAY_RECORD_SUFFIX);
                String updatedType = getAndUpdateFieldNames(type, true, existingFieldNames, updatedFieldNames);
                Token tempTypeName = AbstractNodeFactory.createIdentifierToken(updatedType);

                TypeDescriptorNode tempTypeNode =
                        NodeFactory.createBuiltinSimpleNameReferenceNode(tempTypeName.kind(), tempTypeName);
                if (!typeDescriptorNodes.stream().map(Node::toSourceCode)
                        .toList().contains(tempTypeNode.toSourceCode())) {
                    typeDescriptorNodes.add(tempTypeNode);
                }
            } else if (element.isJsonArray()) {
                Map.Entry<String, JsonArray> arrayEntry =
                        new AbstractMap.SimpleEntry<>(entry.getKey(), element.getAsJsonArray());
                TypeDescriptorNode tempTypeNode =
                        getArrayTypeDescriptorNode(arrayEntry, existingFieldNames, updatedFieldNames);
                if (!typeDescriptorNodes.stream().map(Node::toSourceCode)
                        .toList().contains(tempTypeNode.toSourceCode())) {
                    typeDescriptorNodes.add(tempTypeNode);
                }
            }
        }

        List<TypeDescriptorNode> typeDescriptorNodesSorted = sortTypeDescriptorNodes(typeDescriptorNodes);
        TypeDescriptorNode fieldTypeName = createUnionTypeDescriptorNode(typeDescriptorNodesSorted, false);
        NodeList<ArrayDimensionNode> arrayDimensions = NodeFactory.createEmptyNodeList();
        ArrayDimensionNode arrayDimension = NodeFactory.createArrayDimensionNode(openSBracketToken, null,
                closeSBracketToken);
        arrayDimensions = arrayDimensions.add(arrayDimension);

        return NodeFactory.createArrayTypeDescriptorNode(fieldTypeName, arrayDimensions);
    }

    /**
     * This method generates the Union of all provided TypeDescriptorNodes.
     *
     * @param typeNames List of TypeDescriptorNodes to be unionized
     * @return {@link TypeDescriptorNode} Union TypeDescriptorNode of provided TypeDescriptorNodes
     */
    private static TypeDescriptorNode createUnionTypeDescriptorNode(List<TypeDescriptorNode> typeNames,
                                                                    boolean isOptional) {
        if (typeNames.isEmpty()) {
            Token typeName = AbstractNodeFactory.createToken(SyntaxKind.JSON_KEYWORD);
            return NodeFactory.createBuiltinSimpleNameReferenceNode(typeName.kind(), typeName);
        } else if (typeNames.size() == 1) {
            return typeNames.get(0);
        }
        TypeDescriptorNode unionTypeDescNode = joinToUnionTypeDescriptorNode(typeNames);
        Token openParenToken = NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN);
        Token closeParenToken = NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN);
        Token questionMarkToken = NodeFactory.createToken(SyntaxKind.QUESTION_MARK_TOKEN);

        ParenthesisedTypeDescriptorNode parenTypeDescNode =
                NodeFactory.createParenthesisedTypeDescriptorNode(openParenToken, unionTypeDescNode, closeParenToken);

        return isOptional ?
                NodeFactory.createOptionalTypeDescriptorNode(parenTypeDescNode, questionMarkToken) : parenTypeDescNode;
    }

    /**
     * This method joins types to create UnionTypeDescriptorNode.
     *
     * @param typeNames List of TypeDescriptorNodes to be unionized - the size of the list should be always >= 2
     * @return {@link TypeDescriptorNode} Union TypeDescriptorNode of provided TypeDescriptorNodes
     */
    private static TypeDescriptorNode joinToUnionTypeDescriptorNode(List<TypeDescriptorNode> typeNames) {
        Token pipeToken = NodeFactory.createToken(SyntaxKind.PIPE_TOKEN);

        TypeDescriptorNode unionTypeDescNode = typeNames.get(0);
        for (int i = 1; i < typeNames.size(); i++) {
            unionTypeDescNode =
                    NodeFactory.createUnionTypeDescriptorNode(unionTypeDescNode, pipeToken, typeNames.get(i));
        }
        return unionTypeDescNode;
    }

    /**
     * This method converts UnionTypeDescriptorNode with IDENTIFIER_TOKENS, to its relevant TypeDescriptorNodes.
     *
     * @param typeDescNode                         UnionTypeDescriptorNode which has to be converted
     *                                             UnionTypeDescriptorNode with inline
     *                                             RecordTypeDescriptorNode
     * @param visitedRecordTypeDescNodeTypeToNodes Already analyzed RecordTypeDescriptorNodeType and Nodes.
     * @return {@link TypeDescriptorNode} Converted UnionTypeDescriptorNode.
     */
    private static TypeDescriptorNode convertUnionTypeToInline(TypeDescriptorNode typeDescNode,
                                                               Map<String, RecordTypeDescriptorNode>
                                                                       visitedRecordTypeDescNodeTypeToNodes) {
        List<TypeDescriptorNode> extractedTypeDescNodes =
                extractUnionTypeDescNode(extractArrayTypeDescNode(typeDescNode));
        List<TypeDescriptorNode> updatedTypeDescNodes = new ArrayList<>();

        if (extractedTypeDescNodes.size() == 1) {
            TypeDescriptorNode arrayExtractedNode = extractArrayTypeDescNode(typeDescNode);
            String fieldTypeNameText = arrayExtractedNode.toSourceCode();
            SyntaxKind fieldKind = arrayExtractedNode.kind();
            if (extractArrayTypeDescNode(typeDescNode).kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE)) {
                SimpleNameReferenceNode fieldNameRefNode =
                        (SimpleNameReferenceNode) extractArrayTypeDescNode(typeDescNode);
                fieldTypeNameText = fieldNameRefNode.name().toSourceCode();
                fieldKind = fieldNameRefNode.name().kind();
            }

            if (fieldKind.equals(SyntaxKind.IDENTIFIER_TOKEN)) {
                arrayExtractedNode = visitedRecordTypeDescNodeTypeToNodes.get(fieldTypeNameText);
                if (arrayExtractedNode == null) {
                    return null;
                }
            }
            updatedTypeDescNodes.add(arrayExtractedNode);
        } else {
            for (TypeDescriptorNode extractedTypeDescNode : extractedTypeDescNodes) {
                TypeDescriptorNode updatedTypeDescNode =
                        convertUnionTypeToInline(extractedTypeDescNode, visitedRecordTypeDescNodeTypeToNodes);
                updatedTypeDescNodes.add(updatedTypeDescNode);
            }
        }

        List<TypeDescriptorNode> typeDescNodesSorted = sortTypeDescriptorNodes(updatedTypeDescNodes);
        TypeDescriptorNode unionTypeDescNode = createUnionTypeDescriptorNode(typeDescNodesSorted, false);
        if (typeDescNode.kind().equals(SyntaxKind.ARRAY_TYPE_DESC)) {
            Token openSBracketToken = AbstractNodeFactory.createToken(SyntaxKind.OPEN_BRACKET_TOKEN);
            Token closeSBracketToken = AbstractNodeFactory.createToken(SyntaxKind.CLOSE_BRACKET_TOKEN);
            NodeList<ArrayDimensionNode> arrayDimensions = NodeFactory.createEmptyNodeList();
            ArrayDimensionNode arrayDimension = NodeFactory.createArrayDimensionNode(openSBracketToken, null,
                    closeSBracketToken);
            int numberOfDimensions = getNumberOfDimensions((ArrayTypeDescriptorNode) typeDescNode);
            for (int i = 0; i < numberOfDimensions; i++) {
                arrayDimensions = arrayDimensions.add(arrayDimension);
            }
            unionTypeDescNode = NodeFactory.createArrayTypeDescriptorNode(unionTypeDescNode, arrayDimensions);
        }
        return unionTypeDescNode;
    }

    private String getRecordName(String prefix, String name) {
        String cap = StringUtils.capitalize(name);
        return prefix == null || prefix.isEmpty() ? cap : StringUtils.capitalize(prefix) + cap;
    }
}
