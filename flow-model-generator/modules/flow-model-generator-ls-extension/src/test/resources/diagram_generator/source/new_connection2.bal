import ballerina/http;

service / on new http:Listener(9090) {
    resource function get path() returns error? {
        http:Client localHttpCl = check new ("http://localhost:8080", {
            timeout: 0,
            forwarded: ""
        });
    }
}

