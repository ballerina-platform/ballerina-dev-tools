import ballerina/http;
import ballerinax/kafka;
import ballerinax/rabbitmq;

listener http:Listener httpListener = new (port = 9090);
listener http:Listener githubListener = new (port = 9091, httpVersion = "1.1");

service /api/test on httpListener {
    function init() returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    resource function get greeting/[string name](@http:Header string header, int id = 45) returns json|http:InternalServerError {
        do {
        } on fail error err {
            // handle error
        }
    }
}
