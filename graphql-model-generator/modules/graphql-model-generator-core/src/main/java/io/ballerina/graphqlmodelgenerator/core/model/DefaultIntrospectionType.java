package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.Arrays;
import java.util.List;

public enum DefaultIntrospectionType {
    STRING("String"),
    INT("Int"),
    FLOAT("Float"),
    BOOLEAN("Boolean"),
    DECIMAL("Decimal"),
    UPLOAD("Upload"),
    SCHEMA("__Schema"),
    TYPE("__Type"),
    FIELD("__Field"),
    INPUT_VALUE("__InputValue"),
    ENUM_VALUE("__EnumValue"),
    TYPE_KIND("__TypeKind"),
    DIRECTIVE("__Directive"),
    DIRECTIVE_LOCATION("__DirectiveLocation"),
    QUERY("Query"),
    MUTATION("Mutation"),
    SUBSCRIPTION("Subscription");

    private final String name;
    private final static List<DefaultIntrospectionType> reservedIntrospectionTypes = Arrays.asList(DefaultIntrospectionType.values());

    public static List getReservedIntrospectionTypes() {
        return reservedIntrospectionTypes;
    }

    public static boolean isReservedType(String typeName){
        return reservedIntrospectionTypes.stream().anyMatch(value -> value.getName().equals(typeName));
    }

    DefaultIntrospectionType(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
