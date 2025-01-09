import ballerina/http;

type User record {
    string[] phoneNumber;
};

type Person record {
    string[] contacts;
};

type Account record {
    string accountNumber;
    int balance;
    string lastTransaction;
};

import ballerina/http;

service / on new http:Listener(9090) {

    function init() returns error? {
    }

    resource function post getPerson(@http:Payload User user) returns Person|http:InternalServerError {
        do {
            User u1 = getUser();
            // Account[] var6 = [{accountNumber: u1.phoneNumber[0]}];
        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}
