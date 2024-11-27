import ballerina/http;
import ballerina/io;

final http:Client foodClient = check new ("http://localhost:9090/food");

type Food record {|
    string name;
    string 'type;
    int price;
|};

final map<Food> menu = {};

function printMenu(string 'type) returns http:ClientError? {
    Food[] menu = check foodClient->get("menu");
    var itemCounts = getTypeCount();
    print(itemCounts);
    var foodInfo = extractFoodType(menu, 'type);
    print(foodInfo);
}

function extractFoodType(Food[] menu, string selectedType) returns ItemInfo[]|http:ClientError {
    var result = from var {name, 'type, price} in menu
        where 'type == selectedType
        select {name, price};

    ItemInfo[] itemInfo = [];
    while result.length() > 0 {
        var item = result.pop();
        record {|string keyword; boolean isAvailable;|} ratingRes = check foodClient->get("types/" + selectedType);
        string ratingUrl = "ratings/" + ratingRes.keyword;
        int rating = 0;
        if ratingRes.isAvailable {
            rating = check foodClient->get(ratingUrl);
        } else {
            http:Response postRes = check foodClient->post(ratingUrl, 0);
        }
        itemInfo.push({name: item.name, price: item.price, ratings: rating});
    }
    return itemInfo;
}

function getTypeCount() returns record {|string 'type; int count;|}[] {
    return from var {'type, price} in menu
        group by 'type
        select {'type, count: count(price)};
}

function print(io:Printable input) => io:println(input);

type ItemInfo record {|
    string name;
    int price;
    int ratings;
|};
