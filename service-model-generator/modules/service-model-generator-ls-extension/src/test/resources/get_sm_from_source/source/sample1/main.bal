import ballerina/http;

listener http:Listener httpListener = new (port = 9090);
listener http:Listener githubListener = new (port = 9091, httpVersion = "1.1");

@http:ServiceConfig {
    host: "localhost"
}
service /api/test on httpListener {
    function init() returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    @http:ResourceConfig {
        consumes: []
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
