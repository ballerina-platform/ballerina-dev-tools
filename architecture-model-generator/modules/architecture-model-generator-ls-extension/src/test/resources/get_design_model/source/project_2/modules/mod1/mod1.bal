import ballerina/http;

public listener http:Listener securedEP = new (9091,
    secureSocket = {
        key: {
            certFile: "../resource/path/to/public.crt",
            keyFile: "../resource/path/to/private.key"
        }
    }
);
