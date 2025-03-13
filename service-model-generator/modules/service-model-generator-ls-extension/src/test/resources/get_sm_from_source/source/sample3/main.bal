import ballerina/http;

listener http:Listener httpListener = new (port = 9090);
listener http:Listener githubListener = new (port = 9091, httpVersion = "1.1");

service /api/test on new http:Listener(port = 8080), httpListener, githubListener {
    resource function get path(http:Request request) returns error? {
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
