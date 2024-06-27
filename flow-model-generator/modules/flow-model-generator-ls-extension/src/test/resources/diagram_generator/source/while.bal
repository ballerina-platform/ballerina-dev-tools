import ballerina/http;

http:Client foodClient = check new ("http://localhost:9090");

service /food on new http:Listener(0) {
    resource function get apples(int count) {
        string? msg;
        int i = 0;
        while i < count {
            json|http:ClientError response = foodClient->get("/food/apples");
            if (response is json) {
                msg = "Response received from food service: " + response.toString();
            } else {
                msg = "Error occurred while invoking food service: " + response.message();
            }
            i = i + 1;
        }
    }

    resource function get nothing() {
        while true {
        }
    }

    resource function get oranges(int count) {
        string? msg;
        int i = 0;
        while i < count {
            json response = check foodClient->get("/food/oranges");
            msg = "Response received from food service: " + response.toString();
            i += 1;
        } on fail {
            msg = "Error occurred while invoking food service";
        }
    }

    resource function get mangos(int count) {
        string? msg;
        int i = 0;
        while i < count {
            json response = check foodClient->get("/food/mangos");
            msg = "Response received from food service: " + response.toString();
            i += 1;
        } on fail http:ClientError e {
            msg = "Error occurred while invoking food service" + e.message();
        }
    }

    resource function get pineapples(int count, int retries) returns string {
        int i = 0;
        string finalMsg = "";
        while i < count {
            json response = check foodClient->get("/food/pineapples");
            finalMsg += response.toString();
            i += 1;
        } on fail http:ClientError e {
            int attempts = 0;
            http:Client|http:ClientError adminClient = new ("http://localhost:9090");
            if adminClient is http:ClientError {
                return "Error occurred while creating admin client";
            }
            while attempts < retries {
                json response = check adminClient->post("/admin/restart", {body: e.message()});
                return "Response received from admin service: " + response.toString();
            } on fail {
                return "Error occurred while invoking admin service";
            }
            return "Error occurred while invoking food service";
        }
        return finalMsg;
    }

    resource function get bananas(int count) {
        string? msg;
        int i = 0;
        while i < count {
            json|error response = foodClient->get("/food/bananas");
            if (response is json) {
                msg = "Response received from food service: " + response.toString();
            } else {
                msg = "Error occurred while invoking food service: " + response.message();
                continue;
            }
            i += 1;
        }
    }

    resource function get grapes(int count) {
        string? msg;
        int i = 0;
        while i < count {
            json|error response = foodClient->get("/food/grapes");
            if (response is json) {
                msg = "Response received from food service: " + response.toString();
            } else {
                msg = "Error occurred while invoking food service: " + response.message();
                break;
            }
            i += 1;
        }
    }

    resource function get watermelons(int count) {
        string? msg;
        int i = 0;
        while i < count {
            json|error response = foodClient->get("/food/watermelons");
            if (response is json) {
                msg = "Response received from food service: " + response.toString();
            } else {
                msg = "Error occurred while invoking food service: " + response.message();
                continue;
            }
            if (i == 3) {
                break;
            }
            i += 1;
        }
    }
}
