import ballerina/http;

final http:ClientAuthConfig clientAuthConfig = {
    username: "admin",
    password: "password"
};

function foo() returns error? {
    final http:Client cl1 = check new ("http://localhost:9090");

    final http:Client cl2 = check new ("http://localhost:9090", {
        timeout: 5000
    });

    final http:Client cl3 = check new ("http://localhost:9090", auth = clientAuthConfig);

    final http:Client cl4 = check new ("http://localhost:9090", auth = {username: "", password: ""});

    final http:Client cl5 = check new ("http://localhost:9090", auth = {jwtId: "Id"});

    final http:Client httpClientResult2 = check new ("", auth = {
        username: "test",
        password: "22"
    }, retryConfig = {count: 0, interval: 0.0d});

}
