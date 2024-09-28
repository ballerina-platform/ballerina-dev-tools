import ballerina/http;

configurable string serviceName = "DataMapperService";
configurable int servicePort = 9091;
configurable Input configInput = ?;
configurable table<Input> key(name) configTable = table [
    {name: "Alice", age: 25},
    {name: "Bob", age: 28}
];

service /p1 on new http:Listener(9091) {
    resource function get greeting() returns json|http:InternalServerError {
        do {
            Input localInput = {name: "John", age: 30};

        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}

type Input record {
    readonly string name;
    int age;
};

type Output record {
    string name;
    int age;
};

Input moduleInput = {name: "John", age: 30};
