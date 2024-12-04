import ballerina/http;

type User record {
    string[] phoneNumber;
};

type TwoDim record {|
    string[][] values;
|};

import ballerina/http;

service / on new http:Listener(9090) {

    function init() returns error? {
    }

    resource function post getPerson(@http:Payload User user) returns Person|http:InternalServerError {
        do {
            // TwoDim td = {values: [[user.phoneNumber[0]], [user.phoneNumber[1]], [user.phoneNumber[2], user.phoneNumber[3]]]};
        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}