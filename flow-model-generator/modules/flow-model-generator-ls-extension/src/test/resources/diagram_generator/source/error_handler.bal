import ballerina/http;

http:Client foodClient = check new ("http://localhost:9090");

service /food on new http:Listener(0) {
    resource function get apples() {
        string? msg;
        do {
            json res2 = check foodClient->get("/western/apples");
            msg = res2.toString();
        } on fail {
            msg = "Failed to get the response";
        }
    }

    resource function get oranges() {
        string? msg;
        do {
            json res3 = check foodClient->get("/western/oranges");
            msg = res3.toString();
        } on fail {
            msg = "Failed to get the response";
            do {
                json res = check foodClient->post("/log", "Error occurred while getting the response");
                msg = msg.toString() + res.toString();
            } on fail {
                msg = "Error occurred while logging the error";
            }
        }
    }

    resource function get pineapples() returns string {
        do {
            json res = check foodClient->get("/western/pineapples");
            return res.toString();
        } on fail http:ClientError err {
            return err.message();
        }
    }
}
