import ballerina/http;
import ballerina/log;

type Person record {
    string name;
    int age;
    string city;
    record {
        Student bar;
    } foo;
};

type Student record {
    string firstName;
    string lastName;
    int stdAge;
};

service / on new http:Listener(9090) {

    function init() returns error? {
    }

    resource function post greeting(Student std) returns json|http:InternalServerError {
        do {
            // Person var1909 = {name: std.firstName + std.lastName, age: std.stdAge};
            // Person tnfed = transform(std);
        } on fail error e {
            log:printError("Error: ", 'error = e);
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}
