package io.ballerina.graphqlmodelgenerator.core;

import io.ballerina.graphqlmodelgenerator.core.model.Interaction;
import io.ballerina.graphqlmodelgenerator.core.model.RemoteFunction;
import io.ballerina.graphqlmodelgenerator.core.model.ResourceFunction;
import io.ballerina.graphqlmodelgenerator.core.model.Service;
import io.ballerina.graphqlmodelgenerator.core.utils.ModelGenerationUtils;
import io.ballerina.stdlib.graphql.commons.types.Schema;

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
                ResourceFunction resourceFunction = new ResourceFunction(field.getName(),false,returns,links);
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
                RemoteFunction remoteFunction = new RemoteFunction(field.getName(),returns,links);
                remoteFunctions.add(remoteFunction);
            });
        }
       return new Service("graphql",serviceName,resourceFunctions,remoteFunctions);

    }
}
