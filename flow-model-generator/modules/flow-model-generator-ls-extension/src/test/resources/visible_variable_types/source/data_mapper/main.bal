import ballerina/http;

http:Client cl = check new ("http://localhost:9090");
boolean w = false;

public function main() {
    int x = 32;
    while x < 50 {
        boolean w = x % 2 == 0;
        if (w) {
            int y = 12;
        } else {
            string z = "hello";
            do {
                if z.length() == x {
                    Address address = {houseNo: "10", line1: "foo", line2: "bar", city: "Colombo", country: "Sri Lanka"};
                    
                } else {
                    fail error("error");
                }
            } on fail {
                break;
            }
        }
        x += 2;
    }
}
