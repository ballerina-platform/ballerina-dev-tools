import ballerinax/redis;

final redis:Client redisClient = check new (connection = "localhost");
