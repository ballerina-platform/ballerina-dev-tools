import ballerina/http;

http:Client httpEpS1 = check new (url = "");

public function foo() returns error? {
    http:Client httpEpS2 = check new (url = "");

    json getResponse4 = check httpEpS1->/users/test1/test2;
    json getResponse5 = check httpEpS2->/users;

}
