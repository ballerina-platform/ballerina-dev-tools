import ballerina/http;

final http:Client asiri = check new ("http://localhost:9090");
final http:Client nawaloka = check new ("http://localhost:9091");

service on new http:Listener(9090) {
    resource function get search/doctors/[string area]() returns json|error {
        if area == "kandy" {
            json j = check asiri->get(path = "/doctors/kandy");
            return j;
        } else {
            json j = check nawaloka->get("/doctors");
            return j;
        }
    }
}
