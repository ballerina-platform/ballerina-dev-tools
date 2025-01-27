import service_designer.default;

import ballerina/http;

listener http:Listener httpListener = new (port = 9090);
listener http:Listener githubListener = new (port = 9091, httpVersion = "1.1");

service /api/test on default:globalListener, new http:Listener(port = 8080), httpListener, githubListener {
    function init() returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    function get path() {
    }
}

public type OkResponse record {|
    *http:Ok;
    json body;
|};
