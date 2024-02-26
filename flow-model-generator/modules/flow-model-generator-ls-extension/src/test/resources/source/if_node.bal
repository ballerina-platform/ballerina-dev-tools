import ballerina/http;

http:Client foodClient = check new ("/food");

type Food record {|
    string name;
    int price;
    int remaining?;
    string comment;
|};

string varRef = "text";

service /market on new http:Listener(9090) {
    resource function get apples(boolean flag) {
        if flag {
            int a = 1;
        } else {
            int b = 2;
            string c = "12";
        }
    }

    resource function get bananas(int price) returns string {
        if price > 10 {
            if price > 100 {
                int c = 12;
                return "expensive";
            } else {
                int b = 2;
                return "moderate";
            }
        }
        return "cheap";
    }

    resource function get oranges(Food food) returns Food {
        if food.price < 100 {
            food.comment = "cheap";
            return food;
        } else if food.price < 200 {
            if food.remaining > 0 {
                food.comment = "fresh; ";
            } else {
                food.comment = "expired; ";
            }
            food.comment += "moderate";
            return food;
        } else {
            food.comment = "expensive";
            return food;
        }
    }
}
