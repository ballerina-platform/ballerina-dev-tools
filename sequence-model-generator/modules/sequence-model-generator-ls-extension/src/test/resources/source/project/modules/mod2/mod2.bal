import project.mod1;

import ballerina/http;
import ballerina/io;

http:Client cl = check new ("http://localhost:9090");

function fn(int val) returns error? {
    json res;
    if mod1:isEven(val) {
        res = check cl->get("/hello");
    } else {
        res = null;
    }
    print(res);
}

function print(json val) => io:println(val);
