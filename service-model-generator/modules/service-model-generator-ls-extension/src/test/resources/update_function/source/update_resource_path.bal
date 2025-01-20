import ballerina/http;

listener http:Listener httpListener = new (port = 8080);

service /api/v2\.0 on httpListener {
    function init() returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    resource function get greeting/[int ints]() returns json|http:InternalServerError {
        do {
        } on fail error err {
            // handle error
        }
    }
}
