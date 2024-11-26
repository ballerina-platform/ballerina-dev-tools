import ballerina/http;
import ballerinax/github;

final github:Client githubClient = check new ({
    auth: {token: "GITHUB_PAT"}
}, "https://api.github.com");


service /api on new http:Listener(8080) {

    resource function get stats(http:Caller caller, http:Request req) returns error? {
    }

}
