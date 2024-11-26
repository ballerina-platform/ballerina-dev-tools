import ballerina/http;

type Fruit record {|
    string name;
    int price;
    int quantity?;
    string quality;
|};

service /market on new http:Listener(9090) {
    resource function get apples(string color) returns string {
        match color {
            "red"|"blue" => {
                return "sweet";
            }
            "green" => {
                return "sour";
            }
            _ => {
                return "unknown";
            }
        }
    }

    resource function get bananas(int ripeness) returns string {
        match ripeness {
            1 => {
                return "unripe";
            }
            2 => {
                return "ripe";
            }
            3 => {
                return "overripe";
            }
            var r if r > 3 => {
                return "spoiled";
            }
            _ => {
                return "invalid ripeness level";
            }
        }
    }

    resource function get oranges(Fruit fruit) returns Fruit {
        match fruit.price {
            var p if p < 100 => {
                fruit.quality = "cheap";
            }
            var p if p >= 100 && p < 200 => {
                match fruit.quantity {
                    var q if q is int && q > 0 => {
                        fruit.quality = "fresh; moderate";
                    }
                    _ => {
                        fruit.quality = "expired; moderate";
                    }
                }
            }
            var p if p >= 200 => {
            }
            _ => {
                fruit.quality = "expensive";
            }
        }
        return fruit;
    }

    resource function get grapes(int price) returns string {
        match price {
            var p if p > 100 => {
                return "premium";
            }
            var p if p <= 100 => {
                match p % 4 {
                    0 => {
                        return "discounted";
                    }
                    1 => {
                        if p < 25 {
                            return "clearance";
                        }
                    }
                    _ => {
                        return "regular price";
                    }
                }
            }
        }
        return "unpriced";
    }

    resource function get watermelons(int quantity) returns string {
        match quantity {
            var q if q > 10 => {
                return "large order";
            }
            var q if q > 5 => {
                return "medium order";
            }
            var q if q > 0 => {
                return "small order";
            }
            _ => {
                return "invalid order";
            }
        }
    }

    resource function get pineapples(int count) returns string {
        match count {
            var c if c > 0 => {
                return "available";
            }
            0 => {
                return "out of stock";
            }
            _ => {
                return "invalid count";
            }
        }
    }

    resource function get mangoes(int quantity) returns string {
        match quantity {
            var q if q > 10 => {
                return "bulk order";
            }
            var q if q > 5 => {
                match q {
                    var n if n > 8 => {
                        return "large pack";
                    }
                    _ => {
                        return "medium pack";
                    }
                }
            }
            _ => {
                return "small pack";
            }
        }
    }

    resource function get tomatoes(any quantity) returns string {
        if quantity is int {
            var q = <int>quantity;
            match q {
                var n if n > 10 => {
                    return "bulk";
                }
                var n if n > 5 => {
                    match n {
                        var m if m > 8 => {
                            return "large pack";
                        }
                        var m if m > 6 => {
                            return "medium pack";
                        }
                        _ => {
                            return "small pack";
                        }
                    }
                }
                _ => {
                    return "individual";
                }
            }
        }
        return "invalid quantity";
    }

    resource function get strawberries(int quantity, string season) returns string {
        match quantity {
            var q if q > 10 => {
                match season {
                    "summer" => {
                        return "large summer pack";
                    }
                    _ => {
                        return "large off-season pack";
                    }
                }
            }
            _ => {
                match season {
                    "summer" => {
                        return "regular summer pack";
                    }
                    _ => {
                        return "regular off-season pack";
                    }
                }
            }
        }
    }

    resource function get avocados(int quantity, int ripeness) returns string {
        match quantity {
            var q if q > 10 => {
                return "bulk order";
            }
            var q if q > 5 => {
                match ripeness {
                    1 => {
                        return "medium unripe pack";
                    }
                    2 => {
                        return "medium ripe pack";
                    }
                    _ => {
                        return "medium mixed pack";
                    }
                }
            }
            var q if q > 0 => {
                return "small pack";
            }
            _ => {
                return "invalid order";
            }
        }
    }

    resource function get peaches(map<anydata> data) returns string {
        match data {
            [var quantity, var ripeness] if quantity is int && ripeness is int => {
                if quantity < 0 || ripeness < 1 || ripeness > 5 {
                    panic error("Invalid input values");
                }
                return string `Order for ${quantity} peaches with ripeness ${ripeness}`;
            }
            _ => {
                panic error("Invalid data format");
            }
        } on fail var e {
            return string `Error processing peach order: ${e.message()}`;
        }
    }
    
    resource function get cherries(map<anydata> data) returns string|error {
        match data {
            var obj if obj is record {|int quantity; string 'type;|} => {
                int quantity = obj.quantity;
                string 'type = obj.'type;
                return string `Order for ${quantity} ${'type} cherries placed`;
            }
            _ => {
                return error("Invalid data format");
            }
        } on fail var e {
            if e is error {
                return error("Data processing failed", e);
            }
        }
    }
}
