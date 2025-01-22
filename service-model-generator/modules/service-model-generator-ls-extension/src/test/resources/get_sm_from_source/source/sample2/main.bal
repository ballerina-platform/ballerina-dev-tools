import ballerina/http;

service /api/test on new http:Listener(port = 8080) {
    function init() returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    resource function get greeting/[string name](@http:Header string header, int id = 45) returns OkResponse {
        do {
        } on fail error err {
            // handle error
        }
    }
}

public type OkResponse record {|
    *http:Ok;
    json body;
|};
