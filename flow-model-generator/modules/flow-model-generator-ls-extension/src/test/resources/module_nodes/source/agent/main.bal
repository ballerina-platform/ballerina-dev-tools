import ballerina/http;

final http:Client httpClient = check new ("http://example.com");

public function main() returns error? {
    http:Client localHttpClient = check new ("http://localhost:8080");
}
