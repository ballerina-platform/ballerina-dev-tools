import ballerina/http;

http:Client cl = check new ("/doctors");

type Album record {|
    string name;
    string artist;
    int year;
|};

public function main() returns http:ClientError? {
    Album output = check cl->get("/kandy", {
        "first-header": "val1",
        "second-header": "val2"
    });
}
