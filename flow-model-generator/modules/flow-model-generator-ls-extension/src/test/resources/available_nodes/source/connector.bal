import ballerina/http;
import ballerinax/redis;

final http:Client moduleClient = check new ("http://localhost:8080");

service / on new http:Listener(8080) {
    resource function accessor get(boolean cond) {
        if cond {

        }
    }

    resource function accessor put() returns error? {
        redis:Client redisClient = check new ();
        final redis:Client redisClientResult = check new ();
        do {

        } on fail error e {
            
        }
    }

}
