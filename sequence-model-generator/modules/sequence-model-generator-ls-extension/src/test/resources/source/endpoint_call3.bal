import ballerina/http;


function getCall() returns error? {
    http:Client cl = check new ("http://localhost:9090");
    json getRes = check cl->get("/hello");
    http:Response postRes = check cl->post("/hello", "Hello Ballerina", {"Content-Type": "text/plain"});
}
