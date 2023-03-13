import ballerina/graphql;

service /graphql on new graphql:Listener(9090) {

    resource function get profile/quote() returns string {
        return "I am the one who knocks!";
    }

    # # Deprecated
    @deprecated
    resource function get profile/name/first(int a) returns string {
        return "Walter";
    }

    resource function get profile/name/last() returns string {
        return "White";
    }
}
