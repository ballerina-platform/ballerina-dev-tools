import ballerina/http;

service / on new http:Listener(port = 8080) {

    resource function get path1(http:Request req) returns json {
        do {
        } on fail error err {
            // handle error
        }
    }

    @http:ResourceConfig {
        consumes: []
    }
    resource function get path2(http:Request req) returns json {
        do {
        } on fail error err {
            // handle error
        }
    }
}
