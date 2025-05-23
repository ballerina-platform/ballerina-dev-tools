import ballerina/http;

listener http:Listener httpListener = new (port = 9090);

service /api on httpListener {

}

const CONST = 10;

enum Value {
    VALUE1 = "VALUE1",
    VALUE2,
    VALUE3
}

type STRING string;
