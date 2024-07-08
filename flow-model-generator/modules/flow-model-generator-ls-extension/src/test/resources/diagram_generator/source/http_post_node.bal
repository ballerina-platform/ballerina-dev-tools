import ballerina/http;

http:Client foodClient = check new ("/food");

type Food record {|
    string name;
    string 'type;
    int expire?;
|};

string varRef = "text";

service /market on new http:Listener(9090) {
    resource function post apples() returns http:ClientError? {
        var res1 = check foodClient->post("/apples", "red apple", targetType = json);
        _ = check foodClient->post("/apples", targetType = json, message = "green apple");
        json res2 = check foodClient->/western/apples.post("western red apple");
        json res3 = check foodClient->/apples.post({"type": "red apple"}, mediaType = "application/json");
        json res4 = check foodClient->/.post("green apple");
        var res5 = check foodClient->/apples.post("green apple", targetType = json);
        json res7 = check foodClient->/apples.post("red apple", headers = {
            "first-header": "first",
            "second-header": "second"
        });
        Food res8 = check foodClient->/apples.post("red apple");
        json res9 = check foodClient->/apples/[varRef]/[12 + 3].post("green apple");
    }

    resource function post pears() {
        json|error res1 = foodClient->post("/pears", "pear");
        json|http:ClientError res2 = foodClient->/pears.post("pear");
    }

    resource function post bananas() returns json|http:ClientError {
        return foodClient->post("/bananas", "banana");
    }

    resource function post milkshake() returns json|http:ClientError {
        http:Client drinkClient = check new ("/drink");
        return drinkClient->post("/milkshake", "mixed fruit");
    }
}