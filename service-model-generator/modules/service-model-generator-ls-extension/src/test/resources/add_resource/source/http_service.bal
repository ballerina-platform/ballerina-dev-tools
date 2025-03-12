import ballerina/http;

listener http:Listener httpListener = new (port = 8080);

service /api on httpListener {
    function init() returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

}
