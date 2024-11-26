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
        json res1 = check foodClient->/western/apples.get();
        json res2 = check foodClient->/apples.get(param1="param1", param2="param2");
        json res3 = check foodClient->/.get();
        var res4 = check foodClient->/apples.get(targetType = json);
        json res5 = check foodClient->/apples.get();
        json res6 = check foodClient->/apples.get(headers = {
            "first-header": "first",
            "second-header": "second"
        });
        Food res7 = check foodClient->/apples.get();
        json res8 = check foodClient->/apples/[varRef]/[12 + 3].get();
        Food res9 = check foodClient->/apples;
    }

    resource function get pears() {
        json|error res1 = foodClient->get("/pears");
        json|http:ClientError res2 = foodClient->get("/pears");
    }

    resource function get bananas() returns json|http:ClientError {
        var res1 = check foodClient->get("/bananas", targetType = json);
        _ = check foodClient->get("/bananas", targetType = json);
        _ = check foodClient->get("/bananas", targetType = json, headers = ());
        return foodClient->get("/bananas");
    }

    resource function get milkshake() returns json|http:ClientError {
        http:Client drinkClient = check new ("/drink");
        return drinkClient->get("/milkshake");
    }
}
