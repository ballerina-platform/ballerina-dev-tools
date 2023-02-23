package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.Map;

public class GraphqlModel {
    private final Service graphqlService;
    // Service, records, interfaces, enums
    private final Map<String, RecordComponent> records;
    private final Map<String, ServiceClassComponent> serviceClasses;
    private final Map<String, EnumComponent> enums;
    private final Map<String, UnionComponent> unions;
    private final Map<String, InterfaceComponent> interfaces;

    public GraphqlModel(Service graphqlService, Map<String, RecordComponent> records,
                        Map<String, ServiceClassComponent> serviceClasses, Map<String, EnumComponent> enums, Map<String, UnionComponent> unions, Map<String, InterfaceComponent> interfaces) {
        this.graphqlService = graphqlService;
        this.records = records;
        this.serviceClasses = serviceClasses;
        this.enums = enums;
        this.unions = unions;
        this.interfaces = interfaces;
    }
}


//    SCALAR("Indicates this type is a scalar."),
//    OBJECT("Indicates this type is an object. `fields` and `interfaces` are valid fields."),
//    INTERFACE("Indicates this type is an interface. `fields`, `interfaces`, and `possibleTypes` are valid fields."),
//    UNION("Indicates this type is a union. `possibleTypes` is a valid field."),
//    ENUM("Indicates this type is an enum. `enumValues` is a valid field."),
//    INPUT_OBJECT("Indicates this type is an input object. `inputFields` is a valid field."),
//    LIST("Indicates this type is a list. `ofType` is a valid field."),
//    NON_NULL("Indicates this type is a non-null. `ofType` is a valid field.");