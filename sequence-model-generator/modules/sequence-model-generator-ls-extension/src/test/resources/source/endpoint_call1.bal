import ballerina/http;

http:Client cl = check new ("http://localhost:9090");

function getCall() returns error? {
    json response = check cl->get("/hello");
}
