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

    resource function get grapes(int price) {
        int mod = price % 4;
        if price > 100 {
            return;
        } else {
            if price < 50 {
                if price < 25 {
                    if price < 10 && mod == 1 {
                        return;
                    }
                }
            }
            if mod == 0 {
                return;
            }
        }
    }

    resource function get watermelons(int quantity) returns string {
        if quantity > 10 {
            return "large";
        } else if quantity > 5 {
            return "medium";
        } else {
            return "small";
        }
    }

    resource function get pineapples(int count) returns string {
        if count > 0 {
            return "available";
        } else if count == 0 {
            return "out of stock";
        }
        return "invalid";
    }

    resource function get mangoes(int quantity) returns string {
        if quantity > 10 {
            return "large";
        } else if quantity > 5 {
            if quantity > 8 {
                return "medium";
            } else {
                return "small";
            }
        } else {
            return "tiny";
        }
    }

    resource function get tomatoes(int quantity) returns string {
        if quantity > 10 {
            return "large";
        } else if quantity > 5 {
            if quantity > 3 {
                return "medium";
            } else if quantity > 1 {
                return "small";
            } else {
                return "tiny";
            }
        } else {
            return "tiny";
        }
    }

    resource function get strawberries(int quantity) returns string {
        if quantity > 10 {
            return "large";
        } else {
            if quantity > 8 {
                return "medium";
            } else {
                return "small";
            }
        }
    }

    resource function get avocados(int quantity) returns string {
        if quantity > 10 {
            return "large";
        } else if quantity > 5 {
            return "medium";
        } else if quantity > 2 {
            return "small";
        } else {
            return "tiny";
        }
    }
}
