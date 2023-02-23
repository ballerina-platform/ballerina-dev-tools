package io.ballerina.graphqlmodelgenerator.core;

import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.graphqlmodelgenerator.core.model.*;
import io.ballerina.graphqlmodelgenerator.core.utils.ModelGenerationUtils;
import io.ballerina.stdlib.graphql.commons.types.*;
import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.List;

public class ServiceModelGenerator {
    private final Schema schemaObj;
    private final String serviceName;
    private final List<ResourceFunction> resourceFunctions;
    private final List<RemoteFunction> remoteFunctions;
    private final LineRange servicePosition;
    private final SyntaxTree syntaxTree;

    public ServiceModelGenerator(Schema schema, String serviceName, LineRange servicePosition, SyntaxTree syntaxTree){
        this.schemaObj = schema;
        this.serviceName = serviceName;
        this.resourceFunctions = new ArrayList<>();
        this.remoteFunctions = new ArrayList<>();
        this.servicePosition = servicePosition;
        this.syntaxTree = syntaxTree;
    }

    public Service generate(){
        if (schemaObj.getQueryType() != null){
            schemaObj.getQueryType().getFields().forEach(field -> {
                String returns = ModelGenerationUtils.getFormattedFieldType(field.getType());
                List<Interaction> links = ModelGenerationUtils.getInteractionList(field);
                List<Param> params = new ArrayList<>();
                field.getArgs().forEach(inputValue -> {
                    Param param = new Param(ModelGenerationUtils.createArgType(inputValue),
                            inputValue.getName(), inputValue.getDescription(), inputValue.getDefaultValue());
                    params.add(param);
                    Type paramType = ModelGenerationUtils.getType(inputValue.getType());
                    if (paramType.getKind().equals(TypeKind.INPUT_OBJECT)){
                        String inputObj = ModelGenerationUtils.getFieldType(paramType);
                        if (inputObj != null){
                            links.add(new Interaction(inputObj, ModelGenerationUtils.getPathOfFieldType(paramType)));
                        }
                    }
                });
                Position position = ModelGenerationUtils.findNodeRange(field.getPosition(), this.syntaxTree);
                ResourceFunction resourceFunction = new ResourceFunction(field.getName(),false,returns,
                        position, field.getDescription(), field.isDeprecated(), field.getDeprecationReason(), params, links);
                resourceFunctions.add(resourceFunction);
            });
        }
        // Mutation
        if (schemaObj.getMutationType() != null){
            schemaObj.getMutationType().getFields().forEach(field -> {
                String returns = ModelGenerationUtils.getFormattedFieldType(field.getType());
                List<Interaction> links = ModelGenerationUtils.getInteractionList(field);

                List<Param> params = new ArrayList<>();
                field.getArgs().forEach(inputValue -> {
                    Param param = new Param(ModelGenerationUtils.createArgType(inputValue),
                            inputValue.getName(), inputValue.getDescription(), inputValue.getDefaultValue());
                    params.add(param);
                    Type paramType = ModelGenerationUtils.getType(inputValue.getType());
                    if (paramType.getKind().equals(TypeKind.INPUT_OBJECT)){
                        String inputObj = ModelGenerationUtils.getFieldType(paramType);
                        if (inputObj != null){
                            links.add(new Interaction(inputObj, ModelGenerationUtils.getPathOfFieldType(paramType)));
                        }
                    }
                });
                Position position = ModelGenerationUtils.findNodeRange(field.getPosition(), this.syntaxTree);
                RemoteFunction remoteFunction = new RemoteFunction(field.getName(),returns, position, field.getDescription(),
                        field.isDeprecated(), field.getDeprecationReason(), params, links);
                remoteFunctions.add(remoteFunction);
            });
        }

        // Subscription
        if (schemaObj.getSubscriptionType() != null){
            schemaObj.getSubscriptionType().getFields().forEach(field -> {
                String returns = ModelGenerationUtils.getFormattedFieldType(field.getType());
                List<Interaction> links = ModelGenerationUtils.getInteractionList(field);

                List<Param> params = new ArrayList<>();
                field.getArgs().forEach(inputValue -> {
                    Param param = new Param(ModelGenerationUtils.createArgType(inputValue),
                            inputValue.getName(), inputValue.getDescription(), inputValue.getDefaultValue());
                    params.add(param);
                    Type paramType = ModelGenerationUtils.getType(inputValue.getType());
                    if (paramType.getKind().equals(TypeKind.INPUT_OBJECT)){
                        String inputObj = ModelGenerationUtils.getFieldType(paramType);
                        if (inputObj != null){
                            links.add(new Interaction(inputObj, ModelGenerationUtils.getPathOfFieldType(paramType)));
                        }
                    }
                });
                Position position = ModelGenerationUtils.findNodeRange(field.getPosition(), this.syntaxTree);
                ResourceFunction resourceFunction = new ResourceFunction(field.getName(),true,returns,
                        position, field.getDescription(), field.isDeprecated(), field.getDeprecationReason(), params, links);
                resourceFunctions.add(resourceFunction);
            });
        }

        Position nodePosition = new Position(servicePosition.filePath(),
                new LinePosition(servicePosition.startLine().line(), servicePosition.startLine().offset()),
                new LinePosition(servicePosition.endLine().line(), servicePosition.endLine().offset()));

       return new Service(serviceName, nodePosition, schemaObj.getDescription(), resourceFunctions, remoteFunctions);
    }
}
