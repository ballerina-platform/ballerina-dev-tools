import ballerina/http;

type Country record {
    string country;
    int population;
    string continent;
    int cases;
    int deaths;
};

type CountryFatalityRatio record {
    string country;
    string continent;
    int population;
    decimal caseFatalityRatio;
};

service /sampleSvc on new http:Listener(9091) {

    resource function get path() returns CountryFatalityRatio[]?|error {
        http:Client diseaseEp = check new ("https://disease.sh/v3");
        http:Client diseaseEp2 = check new ("https://disease.sh/v3");
        Country[] countries = check diseaseEp->/covid\-19/countries;
        CountryFatalityRatio[] summary = getSummary(countries);
        string testFunc = test2();
        return summary;
    }
}

function getSummary(Country[] countries) returns CountryFatalityRatio[] {
    CountryFatalityRatio[] summary = from var {country, continent, population, cases, deaths} in countries
        where population >= 100000 && deaths >= 100
        let decimal caseFatalityRatio = <decimal>deaths / <decimal>cases * 100
        order by caseFatalityRatio descending
        limit 10
        select {country, continent, population, caseFatalityRatio};
    return summary;
}

function test2() returns string{
    string test = test3();
    return "";
}

function test3() returns string{
    return "";
}

