import ballerina/http;

http:Client cl = check new ("http://localhost:9090");

function getCall() returns error? {
    json getRes = check cl->get("/hello");
    http:Response postRes = check cl->post("/hello", "Hello Ballerina", {"Content-Type": "text/plain"});
}
