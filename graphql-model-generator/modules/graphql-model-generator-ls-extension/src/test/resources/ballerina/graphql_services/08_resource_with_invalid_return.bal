import ballerina/graphql;

service /graphql on new graphql:Listener(9090) {

    resource function get greeting() returns error? {
        return null;
    }
}
