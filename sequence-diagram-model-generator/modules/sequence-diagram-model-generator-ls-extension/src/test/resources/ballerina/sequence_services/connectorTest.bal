import ballerina/http;

type Country record {
    string country;
    int population;
    string continent;
    int cases;
    int deaths;
};


# A service representing a network-accessible API
# bound to port `9090`.
@display {
    label: "vehicles",
    id: "Vehicles-8b0487e2-e8f4-441c-8634-17e6f57e8408"
}
service /vehicles on new http:Listener(9090) {
    # A resource for generating greetings
    # + name - the input string name
    # + return - string name with hello message or error
    resource function get greeting(string name) returns string|error {

        // Resource level initialization
        http:Client diseaseEpB = check new ("https://disease.sh/v3");
        http:Client diseaseEp2 = check new ("https://disease.sh/v3");
        Country[] countries = check diseaseEpB->/covid\-19/countries;

        // sample response
        return "Hello, " + name;
    }
}



service /users on new http:Listener(9090) {
    // Service level
    @display {
        label: "http",
        id: "http-e0ad7e71-84bf-4862-a48c-84df252e3203"
    }
    http:Client httpEp;

    function init() returns error? {
        self.httpEp = check new (url = "");
    }

    # A resource for generating greetings
    # + name - the input string name
    # + return - string name with hello message or error
    resource function get greeting(string name) returns string|error {
        // different notations for get resource
        json getResponse = check self.httpEp->/;
        json getResponse2 = check self.httpEp->/.get;
        json getResponse3 = check self.httpEp->/.get();
        // different path params
        json getResponse4 = check self.httpEp->/users/test1/test2;
        json getResponse5 = check self.httpEp->/users/[name];
        // different query params
        json postResponse = check self.httpEp->/.post(message = ());
        record {} putResponse = check self.httpEp->/.put(message = (), headers = {});

        return "Hello, " + name;
    }
}

