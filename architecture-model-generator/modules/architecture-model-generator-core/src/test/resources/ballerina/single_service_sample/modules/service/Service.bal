import ballerina/http;
import single_service_sample.repo;
import single_service_sample.model;

final http:Client weatherClient = check new ("http://localhost:9092");

public class AccountService {
    private repo:Repo repository = new();

    public isolated function getAccounts() returns model:Account[] {
        return self.repository.getAllAccounts().toArray();
    }
}

function weatherFunction() returns string|error {
    return weatherClient->/getCurrentWeather.get;
}

public function anotherFunc() returns string|error {
    var res = anotherFunc();
    if res is error {
        return weatherFunction();
    }
    return res;
}
