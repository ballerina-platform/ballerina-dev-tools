import ballerina/http;

type Person record {|
    int age;
|};

type Student record {|
    string[] name;
|};

service / on new http:Listener(9090) {

    function init() returns error? {
    }

    resource function post getPerson(@http:Payload User user) returns Person|http:InternalServerError {
        do {
            int intResult = 1;
        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}
