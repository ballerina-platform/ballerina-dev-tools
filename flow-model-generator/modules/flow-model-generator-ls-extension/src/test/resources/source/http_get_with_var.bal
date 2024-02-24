import ballerina/http;

http:Client cl = check new ("/doctors");

public function main() returns http:ClientError? {
    var output = check cl->get("/kandy", targetType = json);
}
