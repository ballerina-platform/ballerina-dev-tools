import ballerina/http;

// Module-level variable
int moduleVar = 10;

service / on new http:Listener(8080) {
    // Class-level variable
    private int classVar = 20;

    resource function get test() {
        // Local-level variable
        float localVar = 3.14;

        if (moduleVar > 11) {
            int i = 32;
        } else if (self.classVar > ) {

        }
    }
}

function fn(MyRecord rec) {

}

type MyRecord record {|
    int id;
|};

configurable int port = 8080;
final http:Client httpClient = check new (string `http://localhost:${port}`);

function serviceCall() {
    json val = check httpClient->get("/foo");
}

enum MyEnum {
    FIRST, 
    SECOND
}

type MyError distinct error;
