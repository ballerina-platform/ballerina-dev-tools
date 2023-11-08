import ballerinax/googleapis.sheets;
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
    label: "Vehicals",
    id: "Vehicals-8b0487e2-e8f4-441c-8634-17e6f57e8408"
}
service /vehicales on new http:Listener(9090) {

    @display {
        label: "Google Sheets",
        id: "sheets-c01701bf-170a-4b3d-9382-68dcd5af8ba2"
    }
    sheets:Client sheetsEp;

    function init() returns error? {
        self.sheetsEp = check new (config = {
            auth: {
                token: ""
            }
        });
    }

    # A resource for generating greetings
    # + name - the input string name
    # + return - string name with hello message or error
    resource function get greeting(string name) returns string|error {
        // with single return type
        sheets:Spreadsheet createSpreadsheetResponse = check self.sheetsEp->createSpreadsheet(name = "");

        // with union return types
        sheets:Spreadsheet|error createSpreadsheetResponse2 = self.sheetsEp->createSpreadsheet(name = "");

        // wildcard return
        _ = check self.sheetsEp->createSpreadsheet(name = "");

        // no return
        check self.sheetsEp->setCell(spreadsheetId = "", sheetName = "", a1Notation = "", value = 0);

        http:Client diseaseEpB = check new ("https://disease.sh/v3");
        http:Client diseaseEp2 = check new ("https://disease.sh/v3");
        Country[] countries = check diseaseEpB->/covid\-19/countries;

        // sample response
        return "Hello, " + name;
    }
}

// http:Client httpEpM1 = check new (url = "");

# A service representing a network-accessible API
# bound to port `9090`.
@display {
    label: "Products",
    id: "Products-8b0487e2-e8f4-441c-8634-17e6f57e8408"
}
service /products on new http:Listener(9090) {

    function init() returns error? {
        self.sheetsEp = check new (config = {
            auth: {
                token: ""
            }
        });
    }

    # A resource for generating greetings
    # + name - the input string name
    # + return - string name with hello message or error
    resource function get greeting(string name) returns string|error {
        // with single return type
        sheets:Spreadsheet createSpreadsheetResponse = check self.sheetsEp->createSpreadsheet(name = "");

        // with union return types
        sheets:Spreadsheet|error createSpreadsheetResponse2 = self.sheetsEp->createSpreadsheet(name = "");

        // wildcard return
        _ = check self.sheetsEp->createSpreadsheet(name = "");

        // no return
        check self.sheetsEp->setCell(spreadsheetId = "", sheetName = "", a1Notation = "", value = 0);

        // sample response
        return "Hello, " + name;
    }

    // enpioind define after the invocations
    sheets:Client sheetsEp;
}

service /users on new http:Listener(9090) {

    @display {
        label: "http",
        id: "http-e0ad7e71-84bf-4862-a48c-84df252e3203"
    }
    http:Client httpEp;
    http:Client httpEpS1 = check new (url = "");

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

        // record {} postResponse1 = check httpEpM2->/.post(message = ());
        // record {} postResponse2 = check self.httpEpS2->/.post(message = ());
        // record {} getResponse1 = check self.httpEp->/;
        return "Hello, " + name;
    }

    // Service level endpoint define after the invocations
    http:Client httpEpS2 = check new (url = "");
}

// Module level endpoint define after the invocations
http:Client httpEpM2 = check new (url = "");
