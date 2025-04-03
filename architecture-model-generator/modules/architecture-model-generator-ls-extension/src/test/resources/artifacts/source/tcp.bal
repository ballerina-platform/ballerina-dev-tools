import ballerina/tcp;

listener tcp:Listener tcpListener = new (localPort = 9000);

service tcp:Service on tcpListener {
    remote function onConnect(tcp:Caller caller) returns tcp:ConnectionService {
        do {
            TcpEchoService connectionService = new TcpEchoService();
            return connectionService;
        } on fail error err {
            // handle error
            panic error("Unhandled error", err);
        }
    }
}

service class TcpEchoService {
    *tcp:ConnectionService;

    remote function onBytes(tcp:Caller caller, readonly & byte[] data) returns tcp:Error? {
        do {

        } on fail error err {
            // handle error
            panic error("Unhandled error", err);
        }
    }

    remote function onError(tcp:Error tcpError) {
        do {

        } on fail error err {
            // handle error
            panic error("Unhandled error", err);
        }
    }

    remote function onClose() {
        do {
            
        } on fail error err {
            // handle error
            panic error("Unhandled error", err);
        }
    }
}
