import ballerina/http;

service / on new http:Listener(9090) {

    resource function get greeting(string? name) returns string|error {
        do {
            if name is string {

            }
        } on fail error e {
            
        }
    }

    resource function get hello(int id) returns http:InternalServerError {
        do {
            
        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}
