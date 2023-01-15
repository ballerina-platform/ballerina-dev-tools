package io.ballerina.graphqlmodelgenerator.core;

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

    public ServiceModelGenerator(Schema schema, String serviceName, LineRange servicePosition){
        this.schemaObj = schema;
        this.serviceName = serviceName;
        this.resourceFunctions = (schema.getQueryType() != null) ? new ArrayList<>() : null;
        this.remoteFunctions = (schema.getMutationType() != null) ? new ArrayList<>() : null;
        this.servicePosition = servicePosition;
    }

    public Service generate(){
        if (schemaObj.getQueryType() != null){
            schemaObj.getQueryType().getFields().forEach(field -> {
                String returns = ModelGenerationUtils.getFormattedFieldType(field.getType());

                List<Interaction> links = new ArrayList<>();
                String link = ModelGenerationUtils.getFieldType(field.getType());
                if (link != null){
                    links.add(new Interaction(link));
                }
                List<Param> params = new ArrayList<>();
                field.getArgs().forEach(inputValue -> {
                    Param param = new Param(ModelGenerationUtils.createArgType(inputValue),
                            inputValue.getName(), inputValue.getDescription(), inputValue.getDefaultValue());
                    params.add(param);
                    Type paramType = ModelGenerationUtils.getType(inputValue.getType());
                    if (paramType.getKind().equals(TypeKind.INPUT_OBJECT)){
                        String inputObj = ModelGenerationUtils.getFieldType(paramType);
                        if (inputObj != null){
                            links.add(new Interaction(inputObj));
                        }
                    }
                });
                ResourceFunction resourceFunction = new ResourceFunction(field.getName(),false,returns, params, links);
                resourceFunctions.add(resourceFunction);
            });
        }
        // Mutation
        if (schemaObj.getMutationType() != null){
            schemaObj.getMutationType().getFields().forEach(field -> {
                String returns = ModelGenerationUtils.getFormattedFieldType(field.getType());

                List<Interaction> links = new ArrayList<>();
                String link = ModelGenerationUtils.getFieldType(field.getType());
                if (link != null){
                    links.add(new Interaction(link));
                }
                List<Param> params = new ArrayList<>();
                field.getArgs().forEach(inputValue -> {
                    Param param = new Param(ModelGenerationUtils.createArgType(inputValue),
                            inputValue.getName(), inputValue.getDescription(), inputValue.getDefaultValue());
                    params.add(param);
                    Type paramType = ModelGenerationUtils.getType(inputValue.getType());
                    if (paramType.getKind().equals(TypeKind.INPUT_OBJECT)){
                        String inputObj = ModelGenerationUtils.getFieldType(paramType);
                        if (inputObj != null){
                            links.add(new Interaction(inputObj));
                        }
                    }
                });
                RemoteFunction remoteFunction = new RemoteFunction(field.getName(),returns, params, links);
                remoteFunctions.add(remoteFunction);
            });
        }
        Position nodePosition = new Position(servicePosition.filePath(),
                new LinePosition(servicePosition.startLine().line(), servicePosition.startLine().offset()),
                new LinePosition(servicePosition.endLine().line(), servicePosition.endLine().offset()));
       return new Service(serviceName, nodePosition, resourceFunctions, remoteFunctions);

    }
}
