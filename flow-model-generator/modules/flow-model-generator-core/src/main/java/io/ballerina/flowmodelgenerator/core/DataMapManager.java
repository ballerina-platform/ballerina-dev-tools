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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.flowmodelgenerator.core.model.*;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.*;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.PrimitiveType;
import org.ballerinalang.diagramutil.connector.models.connector.types.RecordType;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates types of the data mapper model.
 *
 * @since 1.4.0
 */
public class DataMapManager {

    private final WorkspaceManager workspaceManager;
    private final SemanticModel semanticModel;
    private final Document document;
    private final Gson gson;

    public DataMapManager(WorkspaceManager workspaceManager, SemanticModel semanticModel, Document document) {
        this.workspaceManager = workspaceManager;
        this.semanticModel = semanticModel;
        this.document = document;
        this.gson = new Gson();
    }

    public JsonElement getTypes(JsonElement node, String propertyKey) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        Codedata codedata = flowNode.codedata();
        NodeKind nodeKind = codedata.node();
        if (nodeKind == NodeKind.VARIABLE) {
            String dataType = flowNode.properties().get(Property.DATA_TYPE_KEY).toSourceCode();
            Optional<Symbol> varSymbol = getSymbol(semanticModel.moduleSymbols(), dataType);
            if (varSymbol.isEmpty()) {
                throw new IllegalStateException("Symbol cannot be found for : " + dataType);
            }
            Type t = Type.fromSemanticSymbol(varSymbol.get());
            if (t == null) {
                throw new IllegalStateException("Type cannot be found for : " + propertyKey);
            }
            return gson.toJsonTree(t);
        } else if (nodeKind == NodeKind.FUNCTION_CALL) {
            Optional<Symbol> varSymbol = getSymbol(semanticModel.moduleSymbols(), codedata.symbol());
            if (varSymbol.isEmpty() || varSymbol.get().kind() != SymbolKind.FUNCTION) {
                throw new IllegalStateException("Symbol cannot be found for : " + codedata.symbol());
            }
            Optional<List<ParameterSymbol>> optParams = ((FunctionSymbol) varSymbol.get()).typeDescriptor().params();
            if (optParams.isEmpty()) {
                return new JsonObject();
            }
            Optional<Type> type = optParams.flatMap(params -> params.parallelStream()
                    .filter(param -> param.nameEquals(propertyKey)).findAny()).map(Type::fromSemanticSymbol);
            if (type.isEmpty()) {
                throw new IllegalStateException("Type cannot be found for : " + propertyKey);
            }
            return gson.toJsonTree(type.get());
        }
        return new JsonObject();
    }

    private Optional<Symbol> getSymbol(List<Symbol> symbols, String name) {
        return symbols.parallelStream()
                .filter(symbol -> symbol.nameEquals(name))
                .findAny();
    }

    public JsonElement getLinks(JsonElement node, LinePosition position, String propertyKey, Path filePath,
                                Project project) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        SourceBuilder sourceBuilder = new SourceBuilder(flowNode, this.workspaceManager, filePath);
        Map<Path, List<TextEdit>> textEdits =
                NodeBuilder.getNodeFromKind(flowNode.codedata().node()).toSource(sourceBuilder);
        String source = textEdits.entrySet().stream().iterator().next().getValue().get(0).getNewText();
        TextDocument textDocument = document.textDocument();
        int startTextPosition = textDocument.textPositionFrom(position);
        io.ballerina.tools.text.TextEdit te = io.ballerina.tools.text.TextEdit.from(TextRange.from(startTextPosition,
                source.length()), source);

        TextDocument modifiedTextDoc = textDocument
                .apply(TextDocumentChange.from(List.of(te).toArray(new io.ballerina.tools.text.TextEdit[0])));
        Document modifiedDoc =
                project.duplicate().currentPackage().module(document.module().moduleId())
                        .document(document.documentId()).modify().withContent(String.join(System.lineSeparator(),
                                modifiedTextDoc.textLines())).apply();

        SemanticModel newSemanticModel = modifiedDoc.module().packageInstance().getCompilation()
                .getSemanticModel(modifiedDoc.module().moduleId());
        LineRange lineRange = LineRange.from(filePath.getFileName().toString(),
                modifiedTextDoc.linePositionFrom(startTextPosition),
                modifiedTextDoc.linePositionFrom(startTextPosition + source.length()));
        LinePosition startLine = lineRange.startLine();
        LinePosition endLine = lineRange.endLine();
        Range range = new Range(new Position(startLine.line(), startLine.offset()),
                new Position(endLine.line(), endLine.offset()));
        NonTerminalNode stNode = CommonUtil.findNode(range, modifiedDoc.syntaxTree());

        List<MappingType> inputTypes = getInputTypes(newSemanticModel, modifiedDoc, position);
        inputTypes.sort(Comparator.comparing(mt -> mt.id));
        MappingType output = null;
        List<Mapping> mappings = new ArrayList<>();

        if (stNode.kind() == SyntaxKind.LOCAL_VAR_DECL) {
            Optional<Symbol> optSymbol = newSemanticModel.symbol(stNode);
            if (optSymbol.isPresent()) {
                Symbol symbol = optSymbol.get();
                if (symbol.kind() == SymbolKind.VARIABLE) {
                    Optional<String> optSymbolName = symbol.getName();
                    if (optSymbolName.isPresent()) {
                        String symbolName = optSymbolName.get();
                        Type type = Type.fromSemanticSymbol(symbol);
                        output = getMappingType(symbolName, type);
                        generateDataMappingForVariableDecl((VariableDeclarationNode) stNode, newSemanticModel,
                                mappings, (VariableSymbol) symbol, symbolName);
                    }
                }
            }
        }
        return gson.toJsonTree(new Model(inputTypes, output, mappings));
    }

    private void generateDataMappingForVariableDecl(VariableDeclarationNode varDecl, SemanticModel semanticModel,
                                                    List<Mapping> mappings, VariableSymbol varSymbol, String name) {
        RecordTypeSymbol recordType = getRecordType((varSymbol).typeDescriptor());
        if (recordType == null) {
            return;
        }
        Optional<ExpressionNode> optInitializer = varDecl.initializer();
        if (optInitializer.isEmpty()) {
            return;
        }
        ExpressionNode expressionNode = optInitializer.get();
        if (expressionNode.kind() != SyntaxKind.MAPPING_CONSTRUCTOR) {
            return;
        }
        MappingConstructorExpressionNode mappingCtrExpr = (MappingConstructorExpressionNode) expressionNode;
        for (MappingFieldNode field : mappingCtrExpr.fields()) {
            if (field.kind() == SyntaxKind.SPECIFIC_FIELD) {
                SpecificFieldNode f = (SpecificFieldNode) field;
                Optional<ExpressionNode> optFieldExpr = f.valueExpr();
                if (optFieldExpr.isEmpty()) {
                    continue;
                }
                ExpressionNode fieldExpr = optFieldExpr.get();
                List<String> inputs = new ArrayList<>();
                genInputs(fieldExpr, inputs);
                Mapping mapping = new Mapping(name + "." + f.fieldName(), inputs,
                        fieldExpr.toSourceCode(), getDiagnostics(fieldExpr.lineRange(), semanticModel));
                mappings.add(mapping);
            }
        }
    }

    private RecordTypeSymbol getRecordType(TypeSymbol typeSymbol) {
        TypeDescKind kind = typeSymbol.typeKind();
        if (kind == TypeDescKind.RECORD) {
            return (RecordTypeSymbol) typeSymbol;
        }
        if (kind == TypeDescKind.TYPE_REFERENCE) {
            return getRecordType(((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor());
        }
        return null;
    }

    private void genInputs(Node expr, List<String> inputs) {
        SyntaxKind kind = expr.kind();
        if (kind == SyntaxKind.FIELD_ACCESS || kind == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            inputs.add(expr.toSourceCode().trim());
        } else if (kind == SyntaxKind.BINARY_EXPRESSION) {
            BinaryExpressionNode binaryExpr = (BinaryExpressionNode) expr;
            genInputs(binaryExpr.lhsExpr(), inputs);
            genInputs(binaryExpr.rhsExpr(), inputs);
        }
    }

    private List<String> getDiagnostics(LineRange lineRange, SemanticModel semanticModel) {
        List<Diagnostic> diagnostics = semanticModel.diagnostics(lineRange);
        List<String> diagnosticMsgs = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics) {
            diagnosticMsgs.add(diagnostic.message());
        }
        return diagnosticMsgs;
    }

    private List<MappingType> getInputTypes(SemanticModel semanticModel, Document document, LinePosition position) {
        List<MappingType> mappingTypes = new ArrayList<>();

        List<Symbol> symbols = semanticModel.visibleSymbols(document, position);
        for (Symbol symbol : symbols) {
            SymbolKind kind = symbol.kind();
            if (kind == SymbolKind.VARIABLE) {
                Optional<String> optName = symbol.getName();
                if (optName.isEmpty()) {
                    continue;
                }
                Type type = Type.fromSemanticSymbol(symbol);
                MappingType mappingType = getMappingType(optName.get(), type);
                if (mappingType == null) {
                    continue;
                }
                VariableSymbol varSymbol = (VariableSymbol) symbol;
                if (varSymbol.qualifiers().contains(Qualifier.CONFIGURABLE)) {
                    mappingType.category = "configurable";
                } else {
                    mappingType.category = "variable";
                }
                mappingTypes.add(mappingType);
            } else if (kind == SymbolKind.CONSTANT) {
                Type type = Type.fromSemanticSymbol(symbol);
                // TODO: Name of constant is set to type name, check that
                MappingType mappingType = getMappingType(type.getTypeName(), type);
                if (mappingType == null) {
                    continue;
                }
                mappingType.category = "constant";
                mappingTypes.add(mappingType);
            }
        }
        return mappingTypes;
    }

    private MappingType getMappingType(String name, Type type) {
        if (type.getTypeName().equals("record")) {
            RecordType recordType = (RecordType) type;
            MappingRecordType mappingRecordType = new MappingRecordType(name, type);
            for (Type field : recordType.fields) {
                mappingRecordType.fields.add(getMappingType(name + "." + field.getName(), field));
            }
            return mappingRecordType;
        } else if (type instanceof PrimitiveType) {
            return new MappingType(name, type);
        } else {
            return null;
        }
    }


    private record Model(List<MappingType> inputs, MappingType output, List<Mapping> mappings) {

    }

    private record Mapping(String output, List<String> inputs, String expression, List<String> diagnostics) {

    }

    // TODO: Recheck the constructor generation
    private static class MappingType {
        String id;
        String category;
        Type type;

        MappingType(String id, Type type) {
            this.id = id;
            this.type = type;
        }
    }

    private static class MappingRecordType extends MappingType {
        List<MappingType> fields = new ArrayList<>();

        MappingRecordType(String id, Type type) {
            super(id, type);
        }
    }

    private class MappingArrayType {

    }
}
