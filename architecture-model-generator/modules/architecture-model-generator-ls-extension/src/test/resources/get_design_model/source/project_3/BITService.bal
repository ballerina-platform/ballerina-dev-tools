import ballerina/http;

service /api on new http:Listener(8080) {

    resource function post path() returns error? {
        check sentimentAnalysis();
    }
}

function sentimentAnalysis() returns error? {
    http:Client cl = check new ("");
    json j = check cl->/aa;
}
