import ballerina/http;

http:Client foodClient = check new ("/food");

type Food record {|
    string name;
    string 'type;
    int expire?;
|};

string varRef = "text";

service /market on new http:Listener(9090) {
    resource function get apples() returns http:ClientError? {
        var res1 = check foodClient->get("/apples", targetType = json);
        _ = check foodClient->get("/apples", targetType = json);
        _ = check foodClient->get("/apples", targetType = json, headers = ());
        json res2 = check foodClient->/western/apples.get();
        json res3 = check foodClient->/apples.get();
        json res4 = check foodClient->/.get();
        var res5 = check foodClient->/apples.get(targetType = json);
        json res6 = check foodClient->/apples.get();
        json res7 = check foodClient->/apples.get(headers = {
            "first-header": "first",
            "second-header": "second"
        });
        Food res8 = check foodClient->/apples.get();
        json res9 = check foodClient->/apples/[varRef]/[12 + 3].get();
    }

    resource function get pears() {
        json|error res1 = foodClient->get("/pears");
        json|http:ClientError res2 = foodClient->get("/pears");
    }

    resource function get bananas() returns json|http:ClientError {
        return foodClient->get("/bananas");
    }

    resource function get milkshake() returns json|http:ClientError {
        http:Client drinkClient = check new ("/drink");
        return drinkClient->get("/milkshake");
    }
}