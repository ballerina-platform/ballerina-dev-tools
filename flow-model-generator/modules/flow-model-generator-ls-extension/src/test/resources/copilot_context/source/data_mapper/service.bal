import ballerina/http;

service /p1 on new http:Listener(9091) {
    resource function get greeting() returns json|http:InternalServerError {
        do {
            Input localInput = {name: "John", age: 30};

        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}

Input moduleInput = {name: "John", age: 30};
