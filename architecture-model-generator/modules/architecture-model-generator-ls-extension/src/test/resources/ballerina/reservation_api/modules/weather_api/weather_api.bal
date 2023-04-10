import ballerina/http;

string currentWeather = "Sunny";

@display {
    label: "",
    id: "weather"
}
service /api/weather on new http:Listener(9092) {
    resource function get getCurrentWeather() returns string {
        return currentWeather;
    }
}
