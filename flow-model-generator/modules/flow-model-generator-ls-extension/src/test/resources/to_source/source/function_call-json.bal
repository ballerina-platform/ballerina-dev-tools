import ballerina/data.jsondata;

function fn(string payload) returns error? {
    _ = check jsondata:toJson(payload);
}
