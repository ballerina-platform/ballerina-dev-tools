import ballerina/random;
import ballerina/uuid;

function name() {
    float value = 2 + random:createDecimal();
    string someId = random:createDecimal().toString() + uuid:createRandomUuid();
    string randomId = randomUuid(random:createDecimal(), uuid:createRandomUuid());
}

function randomUuid(float randomNumber, string id) returns string => randomNumber.toString() + id;
