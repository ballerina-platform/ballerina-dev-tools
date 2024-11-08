import ballerina/http;
import ballerina/lang.runtime;
import ballerinax/redis;

configurable string currencyServiceUrl = "http://localhost:9090/currency";
configurable decimal cacheExpirationTime = 3600.0; // Cache expiration time in seconds

// Client for the currency rate service
final http:Client currencyClient = check new (currencyServiceUrl);

// Redis client
final redis:Client redisClient = check new ({});

service /convert on new http:Listener(9091) {

    function init() returns error? {
    }

    resource function get .(@http:Query string fromCurrency, @http:Query string toCurrency, @http:Query decimal amount) returns json|http:BadRequest|http:InternalServerError {
        do {
            if fromCurrency == "" || toCurrency == "" || amount < 0.0d {
                return <http:BadRequest>{
                    body: {
                        "error": "Invalid input parameters"
                    }
                };
            }

            string cacheKey = string `${fromCurrency}:${toCurrency}`;
            decimal exchangeRate;

            // Try to get the exchange rate from Redis cache
            string|redis:Error? cachedRate = redisClient->get(cacheKey);

            if cachedRate is string {
                exchangeRate = check decimal:fromString(cachedRate);
            } else {
                // If not in cache, call the currency rate service
                json|error response = currencyClient->get("/rate?sourceCurrency=" + fromCurrency + "&targetCurrency=" + toCurrency);

                if response is error {
                    return <http:InternalServerError>{
                        body: {
                            "error": "Failed to fetch exchange rate"
                        }
                    };
                }

                json rateJson = check response.rate;
                exchangeRate = <decimal>rateJson;

                // Cache the exchange rate in Redis
                string|redis:Error setResult = redisClient->set(cacheKey, exchangeRate.toString());
                if setResult is redis:Error {
                    // Log the error or handle it as needed
                    runtime:sleep(0.1); // Small delay to simulate error handling
                } else {
                    // Set expiration time for the cached value
                    string|redis:Error expireResult = redisClient->set(cacheKey, cacheExpirationTime.toString());

                }
            }

            decimal convertedAmount = amount * exchangeRate;

            return {
                "fromCurrency": fromCurrency,
                "toCurrency": toCurrency,
                "amount": amount,
                "convertedAmount": convertedAmount,
                "exchangeRate": exchangeRate
            };
        } on fail error e {
            return <http:InternalServerError>{
                body: {
                    "error": e.message()
                }
            };
        }
    }
}
