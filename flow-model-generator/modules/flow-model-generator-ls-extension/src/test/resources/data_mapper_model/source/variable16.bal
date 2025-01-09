import ballerina/http;

type User record {
    string[] phoneNumber;
};

type Person record {
    Contact[] contacts;
};

type Contact record {
    string primaryPhone;
    string secondaryPhone;
};

import ballerina/http;

service / on new http:Listener(9090) {

    function init() returns error? {
    }

    resource function post getPerson(@http:Payload User user) returns Person|http:InternalServerError {
        do {
            // Person var3 = {contacts: [{primaryPhone: user.phoneNumber[0], secondaryPhone: user.phoneNumber[1]}]};
        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}