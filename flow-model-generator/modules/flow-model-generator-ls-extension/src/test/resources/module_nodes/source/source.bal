import ballerina/grpc;
import ballerina/http;
import ballerinax/redis;

final http:Client httpClient = check new ("http://example.com");
final redis:Client redisClient = check new ();
final grpc:Client grpcClient = check new ("http://example.com");

public function main() returns error? {
    http:Client localHttpClient = check new ("http://localhost:8080");
}
