import ballerina/tcp;

listener tcp:Listener tcpListener = new (localPort = 8080);

service tcp:Service on tcpListener {
    remote function onConnect(tcp:Caller caller) returns tcp:ConnectionService {
        do {
            panic error("Error occurred");
        } on fail error err {
            // handle error
            panic err;
        }
    }
}
