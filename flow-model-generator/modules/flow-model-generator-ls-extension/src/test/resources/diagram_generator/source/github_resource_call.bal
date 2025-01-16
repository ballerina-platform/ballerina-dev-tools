import ballerinax/github;

final github:Client githubClient = check new ({
    auth: {token: ""}
});

function foo(int threadId) returns error? {
    github:ManifestConversions manifestConversions = check githubClient->/app\-manifests/["code-123"]/conversions.post;
    http:Response response = check githubClient->/notifications/threads/[threadId]/subscription.delete;
}
