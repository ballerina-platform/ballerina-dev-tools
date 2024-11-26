import ballerina/http;
import ballerinax/redis;

http:Client moduleHttpCl = check new ("http://localhost:9090", {});
redis:Client moduleRedisCl = check new;

public function fn1() returns error? {
    http:Client localHttpCl = check new ("http://localhost:8080");
    redis:Client localRedisCl = check new;
}

http:Client moduleHttpExCl = check new http:Client("http://localhost:9090");
redis:Client moduleRedisExCl = check new redis:Client();

public function fn2() returns error? {
    http:Client localHttpCl = check new http:Client("http://localhost:8080");
    redis:Client localRedisCl = check new redis:Client();
}

service / on new http:Listener(8080) {
    http:Client serviceHttpCl = check new ("http://localhost:9090");
    redis:Client serviceRedisCl = check new;

    function init() returns error? {
    }

    resource function get .() {

    }
}
