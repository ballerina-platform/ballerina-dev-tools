import ballerina/http;

service /p1 on new http:Listener(9091) {
    resource function get greeting(Input paramInput) returns json|http:InternalServerError {
        do {
            Input localInput = {name: "John", age: 30};

        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}

type Input record {
    string name;
    int age;
};

type Output record {
    string name;
    int age;
};

Input moduleInput = {name: "John", age: 30};
