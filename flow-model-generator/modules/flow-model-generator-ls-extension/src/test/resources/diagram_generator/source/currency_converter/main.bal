import ballerina/http;
import ballerina/log;
import ballerinax/redis;

// new connection
final redis:Client redisClient = check new (connection = {
    host: "localhost",
    port: 6379
});

// new connection
final http:Client currencyClient = check new (url = "https://api.exchangeratesapi.io/latest");

service / on new http:Listener(8080) {
    // http api event
    resource function get converter(string 'from, string to, decimal amount) returns json|http:InternalServerError {
        // error handler
        do {
            // new data
            string key = 'from + "-" + to;
            // action call - redis get call
            string? cacheValue = check redisClient->get(key);

            // if node
            if cacheValue is string {
                // new data
                json newData = {
                    value: check calculate(amount, cacheValue)
                };
                // return
                return newData;
            } else {
                // action call - http get call
                json response = check currencyClient->get("/latest?base=" + 'from + "&to=" + to);

                // new data
                decimal value = check response.value;
                // function call
                decimal convertedAmount = check calculate(amount, value.toString());

                // action call - redis set call
                _ = check redisClient->set(key, convertedAmount.toString());

                // new data
                json newData = {
                    value
                };
                // return
                return newData;
            }
        } on fail error e {
            // library call - log error
            log:printError("error occured", e);
            // return 
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}

function calculate(decimal amount, string cacheValue) returns decimal|error 
    => amount * check decimal:fromString(cacheValue);
