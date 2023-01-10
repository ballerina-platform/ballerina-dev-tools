package io.ballerina.graphqlmodelgenerator.core;

import io.ballerina.graphqlmodelgenerator.core.model.*;
import io.ballerina.graphqlmodelgenerator.core.utils.ModelGenerationUtils;
import io.ballerina.stdlib.graphql.commons.types.Schema;
import io.ballerina.stdlib.graphql.commons.utils.SdlSchemaStringGenerator;

import java.util.ArrayList;
import java.util.List;

public class ServiceModelGenerator {
    private final Schema schemaObj;
    private final String serviceName;
    private final List<ResourceFunction> resourceFunctions;
    private final List<RemoteFunction> remoteFunctions;

    public ServiceModelGenerator(Schema schema, String serviceName){
        this.schemaObj = schema;
        this.serviceName = serviceName;
        this.resourceFunctions = (schema.getQueryType() != null) ? new ArrayList<>() : null;
        this.remoteFunctions = (schema.getMutationType() != null) ? new ArrayList<>() : null;
    }

    public Service generate(){
        if (schemaObj.getQueryType() != null){
            schemaObj.getQueryType().getFields().forEach(field -> {
                List<String> returns = new ArrayList<>();
                returns.add(ModelGenerationUtils.getFormattedFieldType(field.getType()));
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
                });
                ResourceFunction resourceFunction = new ResourceFunction(field.getName(),false,returns, params, links);
                resourceFunctions.add(resourceFunction);
            });
        }
        // TODO: ADD parameters
        if (schemaObj.getMutationType() != null){
            schemaObj.getMutationType().getFields().forEach(field -> {
                List<String> returns = new ArrayList<>();
                returns.add(ModelGenerationUtils.getFormattedFieldType(field.getType()));
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
                });
                RemoteFunction remoteFunction = new RemoteFunction(field.getName(),returns, params, links);
                remoteFunctions.add(remoteFunction);
            });
        }
       return new Service("graphql",serviceName,resourceFunctions,remoteFunctions);

    }
}
