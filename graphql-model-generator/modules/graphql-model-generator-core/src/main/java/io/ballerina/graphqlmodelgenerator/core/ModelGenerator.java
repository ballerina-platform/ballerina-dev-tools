package io.ballerina.graphqlmodelgenerator.core;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.graphqlmodelgenerator.core.exception.GraphqlModelGenerationException;
import io.ballerina.graphqlmodelgenerator.core.model.GraphqlModel;
import io.ballerina.graphqlmodelgenerator.core.model.Service;
import io.ballerina.graphqlmodelgenerator.core.utils.CommonUtil;
import io.ballerina.graphqlmodelgenerator.core.utils.ModelGenerationUtils;
import io.ballerina.projects.*;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.stdlib.graphql.commons.types.Schema;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.nio.file.Path;

import static io.ballerina.graphqlmodelgenerator.core.Constants.*;
import static io.ballerina.stdlib.graphql.compiler.Utils.getSchemaObject;

public class ModelGenerator {

    public GraphqlModel getGraphqlModel(Project project, LineRange position, SemanticModel semanticModel) throws
            GraphqlModelGenerationException {
        Package packageName = project.currentPackage();
        DocumentId docId;
        Document doc;
        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            Path filePath = Path.of(position.filePath());
            docId = project.documentId(filePath);
            ModuleId moduleId = docId.moduleId();
            doc = project.currentPackage().module(moduleId).document(docId);
        } else {
            Module currentModule = packageName.getDefaultModule();
            docId = currentModule.documentIds().iterator().next();
            doc = currentModule.document(docId);
        }

        SyntaxTree syntaxTree = doc.syntaxTree();
        Range range = CommonUtil.toRange(position);
        NonTerminalNode node = CommonUtil.findSTNode(range, syntaxTree);
        if (node.kind() != SyntaxKind.SERVICE_DECLARATION && node.kind() != SyntaxKind.MODULE_VAR_DECL) {
            throw new GraphqlModelGenerationException(INVALID_NODE_MSG);
        }

        Schema schemaObject = getSchemaObject(node, semanticModel, project);
        if (schemaObject.getTypes().isEmpty()){
            throw new GraphqlModelGenerationException(EMPTY_SCHEMA_MSG);
        }
        String serviceName = "";
        if (node instanceof ServiceDeclarationNode){
            ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) node;
            serviceName = ModelGenerationUtils.getServiceBasePath(serviceDeclarationNode);
        } else if (node instanceof ModuleVariableDeclarationNode){
            ModuleVariableDeclarationNode moduleVarDclNode = (ModuleVariableDeclarationNode)node;
           serviceName =  moduleVarDclNode.typedBindingPattern().bindingPattern().toSourceCode();
        }

        return constructGraphqlModel(schemaObject, serviceName, position, syntaxTree);
    }

    public GraphqlModel constructGraphqlModel(Schema schemaObj, String serviceName, LineRange nodeLocation,
                                              SyntaxTree syntaxTree) throws GraphqlModelGenerationException {
        try {
            ServiceModelGenerator serviceModelGenerator = new ServiceModelGenerator(schemaObj, serviceName,
                    nodeLocation, syntaxTree);
            Service graphqlService = serviceModelGenerator.generate();

            InteractedComponentModelGenerator componentModelGenerator = new
                    InteractedComponentModelGenerator(schemaObj);
            componentModelGenerator.generate();

            return new GraphqlModel(graphqlService, componentModelGenerator.getRecords(),
                    componentModelGenerator.getServiceClasses(), componentModelGenerator.getEnums(),
                    componentModelGenerator.getUnions());
        } catch (Exception e){
            throw new GraphqlModelGenerationException(String.format(MODEL_GENERATION_ERROR_MSG, e.getMessage()));
        }
    }



//    /**
//     * Convert the syntax-node line range into a lsp4j range.
//     *
//     * @param lineRange - line range
//     * @return {@link Range} converted range
//     */
//    public static Range toRange(LineRange lineRange) {
//        return new Range(toPosition(lineRange.startLine()), toPosition(lineRange.endLine()));
//    }
//
//    /**
//     * Converts syntax-node line position into a lsp4j position.
//     *
//     * @param linePosition - line position
//     * @return {@link Position} converted position
//     */
//    public static Position toPosition(LinePosition linePosition) {
//        return new Position(linePosition.line(), linePosition.offset());
//    }

//    /**
//     * Get encoded schema string from the given node.
//     */
//    public static String getSchemaString(ServiceDeclarationNode node) throws SchemaFileGenerationException {
//        if (node.metadata().isPresent()) {
//            if (!node.metadata().get().annotations().isEmpty()) {
//                MappingConstructorExpressionNode annotationValue = getAnnotationValue(node.metadata().get());
//                return getSchemaStringFieldFromValue(annotationValue);
//            }
//        }
//        throw new SchemaFileGenerationException(DiagnosticMessage.SDL_SCHEMA_102, null, Constants.MESSAGE_MISSING_ANNOTATION);
//    }

//    /**
//     * Get schema string field from the given node.
//     */
//    private static String getSchemaStringFieldFromValue(MappingConstructorExpressionNode annotationValue)
//            throws SchemaFileGenerationException {
//        SeparatedNodeList<MappingFieldNode> existingFields = annotationValue.fields();
//        for (MappingFieldNode field : existingFields) {
//            if (field.children().get(0).toString().contains(Constants.SCHEMA_STRING_FIELD)) {
//                String schemaString = field.children().get(2).toString();
//                return schemaString.substring(1, schemaString.length() - 1);
//            }
//        }
//        throw new SchemaFileGenerationException(DiagnosticMessage.SDL_SCHEMA_102, null,
//                Constants.MESSAGE_MISSING_FIELD_SCHEMA_STRING);
//    }

//    /**
//     * This method use for decode the encoded schema string.
//     *
//     * @param schemaString     encoded schema string
//     * @return GraphQL schema object
//     */
//    public static Schema getDecodedSchema(String schemaString) throws SchemaFileGenerationException {
//        if (schemaString == null || schemaString.isBlank() || schemaString.isEmpty()) {
//            throw new SchemaFileGenerationException(DiagnosticMessage.SDL_SCHEMA_102, null,
//                    Constants.MESSAGE_INVALID_SCHEMA_STRING);
//        }
//        byte[] decodedString = Base64.getDecoder().decode(schemaString.getBytes(StandardCharsets.UTF_8));
//        try {
//            ByteArrayInputStream byteStream = new ByteArrayInputStream(decodedString);
//            ObjectInputStream inputStream = new ObjectInputStream(byteStream);
//            return (Schema) inputStream.readObject();
//        } catch (IOException | ClassNotFoundException e) {
//            throw new SchemaFileGenerationException(DiagnosticMessage.SDL_SCHEMA_102, null,
//                    Constants.MESSAGE_CANNOT_READ_SCHEMA_STRING);
//        }
//    }

//    /**
//     * Get annotation value string from the given metadata node.
//     */
//    private static MappingConstructorExpressionNode getAnnotationValue(MetadataNode metadataNode)
//            throws SchemaFileGenerationException {
//        for (AnnotationNode annotationNode: metadataNode.annotations()) {
//            if (isGraphqlServiceConfig(annotationNode) && annotationNode.annotValue().isPresent()) {
//                return annotationNode.annotValue().get();
//            }
//        }
//        throw new SchemaFileGenerationException(DiagnosticMessage.SDL_SCHEMA_102, null,
//                Constants.MESSAGE_MISSING_SERVICE_CONFIG);
//    }

//    /**
//     * Check whether the given annotation is a GraphQL service config.
//     *
//     * @param annotationNode     annotation node
//     */
//    private static boolean isGraphqlServiceConfig(AnnotationNode annotationNode) {
//        if (annotationNode.annotReference().kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE) {
//            return false;
//        }
//        QualifiedNameReferenceNode referenceNode = ((QualifiedNameReferenceNode) annotationNode.annotReference());
//        if (!PACKAGE_NAME.equals(referenceNode.modulePrefix().text())) {
//            return false;
//        }
//        return Constants.SERVICE_CONFIG_IDENTIFIER.equals(referenceNode.identifier().text());
//    }

//    public static NonTerminalNode findSTNode(Range range, SyntaxTree syntaxTree) {
//        TextDocument textDocument = syntaxTree.textDocument();
//        Position rangeStart = range.getStart();
//        Position rangeEnd = range.getEnd();
//        int start = textDocument.textPositionFrom(LinePosition.from(rangeStart.getLine(), rangeStart.getCharacter()));
//        int end = textDocument.textPositionFrom(LinePosition.from(rangeEnd.getLine(), rangeEnd.getCharacter()));
//        return ((ModulePartNode) syntaxTree.rootNode()).findNode(TextRange.from(start, end - start), true);
//    }
}

