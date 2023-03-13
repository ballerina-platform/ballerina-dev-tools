import graphql_services.utils;
import ballerina/graphql;


service /graphql on new graphql:Listener(9090) {

    resource function get day() returns utils:Weekday {
        return MONDAY;
    }

    resource function get profile() returns Profile {
        return new ("Walter White", 51);
    }
}
