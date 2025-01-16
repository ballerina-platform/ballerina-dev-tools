import ballerina/http;

type User record {
    string[] phoneNumber;
};

type Person5 record {|
    Temp[] contacts;
|};

type Temp record {|
    string primaryPhone;
    string[] secondaryPhones;
|};

import ballerina/http;

service / on new http:Listener(9090) {

    function init() returns error? {
    }

    resource function post getPerson(@http:Payload User user) returns Person|http:InternalServerError {
        do {
            User u1 = getUser();
            // Person5 var5 = {
            //     contacts: [
            //         {
            //             primaryPhone: "",
            //             secondaryPhones: [
            //                 "1",
            //                 user.phoneNumber[0]
            //             ]
            //         }
            //     ]
            // };
        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}