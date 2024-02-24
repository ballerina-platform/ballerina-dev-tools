import ballerina/http;

http:Client cl = check new ("/doctors");

public function main() {
    json|error output = cl->get("/kandy");
}
