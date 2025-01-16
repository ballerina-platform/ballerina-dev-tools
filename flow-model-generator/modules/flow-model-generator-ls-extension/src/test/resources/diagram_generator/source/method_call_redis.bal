import ballerinax/redis;

final redis:Client redisClient = check new;

function closeConnection() returns error? {
    check redisClient.close();
}
