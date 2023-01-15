package io.ballerina.graphqlmodelgenerator.core;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.graphqlmodelgenerator.core.diagnostic.DiagnosticMessage;
import io.ballerina.graphqlmodelgenerator.core.exception.SchemaFileGenerationException;
import io.ballerina.graphqlmodelgenerator.core.model.*;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static io.ballerina.stdlib.graphql.commons.utils.Utils.PACKAGE_NAME;

public class ModelGenerator {

    public GraphqlModel getGraphqlModel(Project project, LineRange position) throws  SchemaFileGenerationException{
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

        // TODO: check if the service is a graphql service
        SyntaxTree syntaxTree = doc.syntaxTree();
        Range range = toRange(position);
        NonTerminalNode node = findSTNode(range, syntaxTree);
        if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
            return null;
        }

        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) node;

        String schemaString = getSchemaString(serviceDeclarationNode);
        Schema schemaObject = getDecodedSchema(schemaString);

        String serviceName = ModelGenerationUtils.getServiceBasePath(serviceDeclarationNode);
        return constructGraphqlModel(schemaObject, serviceName, position);
    }

    public GraphqlModel constructGraphqlModel(Schema schemaObj, String serviceName, LineRange nodeLocation) {
        ServiceModelGenerator serviceModelGenerator = new ServiceModelGenerator(schemaObj, serviceName, nodeLocation);
        Service graphqlService = serviceModelGenerator.generate();

        InteractedComponentModelGenerator componentModelGenerator = new InteractedComponentModelGenerator(schemaObj);
        componentModelGenerator.generate();

        return new GraphqlModel(graphqlService,componentModelGenerator.getObjects(),componentModelGenerator.getEnums(),componentModelGenerator.getUnions());
    }



    /**
     * Convert the syntax-node line range into a lsp4j range.
     *
     * @param lineRange - line range
     * @return {@link Range} converted range
     */
    public static Range toRange(LineRange lineRange) {
        return new Range(toPosition(lineRange.startLine()), toPosition(lineRange.endLine()));
    }

    /**
     * Converts syntax-node line position into a lsp4j position.
     *
     * @param linePosition - line position
     * @return {@link Position} converted position
     */
    public static Position toPosition(LinePosition linePosition) {
        return new Position(linePosition.line(), linePosition.offset());
    }

    /**
     * Get encoded schema string from the given node.
     */
    public static String getSchemaString(ServiceDeclarationNode node) throws SchemaFileGenerationException {
        if (node.metadata().isPresent()) {
            if (!node.metadata().get().annotations().isEmpty()) {
                MappingConstructorExpressionNode annotationValue = getAnnotationValue(node.metadata().get());
                return getSchemaStringFieldFromValue(annotationValue);
            }
        }
        throw new SchemaFileGenerationException(DiagnosticMessage.SDL_SCHEMA_102, null, Constants.MESSAGE_MISSING_ANNOTATION);
    }

    /**
     * Get schema string field from the given node.
     */
    private static String getSchemaStringFieldFromValue(MappingConstructorExpressionNode annotationValue)
            throws SchemaFileGenerationException {
        SeparatedNodeList<MappingFieldNode> existingFields = annotationValue.fields();
        for (MappingFieldNode field : existingFields) {
            if (field.children().get(0).toString().contains(Constants.SCHEMA_STRING_FIELD)) {
                String schemaString = field.children().get(2).toString();
                return schemaString.substring(1, schemaString.length() - 1);
            }
        }
        throw new SchemaFileGenerationException(DiagnosticMessage.SDL_SCHEMA_102, null,
                Constants.MESSAGE_MISSING_FIELD_SCHEMA_STRING);
    }

    /**
     * This method use for decode the encoded schema string.
     *
     * @param schemaString     encoded schema string
     * @return GraphQL schema object
     */
    public static Schema getDecodedSchema(String schemaString) throws SchemaFileGenerationException {
        if (schemaString == null || schemaString.isBlank() || schemaString.isEmpty()) {
            throw new SchemaFileGenerationException(DiagnosticMessage.SDL_SCHEMA_102, null,
                    Constants.MESSAGE_INVALID_SCHEMA_STRING);
        }
        byte[] decodedString = Base64.getDecoder().decode(schemaString.getBytes(StandardCharsets.UTF_8));
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(decodedString);
            ObjectInputStream inputStream = new ObjectInputStream(byteStream);
            return (Schema) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SchemaFileGenerationException(DiagnosticMessage.SDL_SCHEMA_102, null,
                    Constants.MESSAGE_CANNOT_READ_SCHEMA_STRING);
        }
    }

    /**
     * Get annotation value string from the given metadata node.
     */
    private static MappingConstructorExpressionNode getAnnotationValue(MetadataNode metadataNode)
            throws SchemaFileGenerationException {
        for (AnnotationNode annotationNode: metadataNode.annotations()) {
            if (isGraphqlServiceConfig(annotationNode) && annotationNode.annotValue().isPresent()) {
                return annotationNode.annotValue().get();
            }
        }
        throw new SchemaFileGenerationException(DiagnosticMessage.SDL_SCHEMA_102, null,
                Constants.MESSAGE_MISSING_SERVICE_CONFIG);
    }

    /**
     * Check whether the given annotation is a GraphQL service config.
     *
     * @param annotationNode     annotation node
     */
    private static boolean isGraphqlServiceConfig(AnnotationNode annotationNode) {
        if (annotationNode.annotReference().kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE) {
            return false;
        }
        QualifiedNameReferenceNode referenceNode = ((QualifiedNameReferenceNode) annotationNode.annotReference());
        if (!PACKAGE_NAME.equals(referenceNode.modulePrefix().text())) {
            return false;
        }
        return Constants.SERVICE_CONFIG_IDENTIFIER.equals(referenceNode.identifier().text());
    }

    public static NonTerminalNode findSTNode(Range range, SyntaxTree syntaxTree) {
        TextDocument textDocument = syntaxTree.textDocument();
        Position rangeStart = range.getStart();
        Position rangeEnd = range.getEnd();
        int start = textDocument.textPositionFrom(LinePosition.from(rangeStart.getLine(), rangeStart.getCharacter()));
        int end = textDocument.textPositionFrom(LinePosition.from(rangeEnd.getLine(), rangeEnd.getCharacter()));
        return ((ModulePartNode) syntaxTree.rootNode()).findNode(TextRange.from(start, end - start), true);
    }
}

