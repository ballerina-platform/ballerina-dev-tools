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
}
