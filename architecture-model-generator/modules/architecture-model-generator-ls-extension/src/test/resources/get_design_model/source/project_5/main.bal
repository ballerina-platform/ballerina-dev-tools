import ballerina/http;

public type FlowVars record {|
    string userId?;
|};

public type InboundProperties record {|
    http:Response response;
    map<string> uriParams;
|};

public type Context record {|
    anydata payload;
    FlowVars flowVars;
    InboundProperties inboundProperties;
|};

public type Record record {
};

public listener http:Listener HTTP_Listener_Configuration = new (8081, {host: "0.0.0.0"});

service / on HTTP_Listener_Configuration {
    Context ctx;

    function init() {
        self.ctx = {payload: (), flowVars: {}, inboundProperties: {response: new, uriParams: {}}};
    }

    resource function get user/[string id]() returns http:Response|error {
        self.ctx.inboundProperties.uriParams = {id};
        return self._invokeEndPoint0_(self.ctx);
    }

    private function _invokeEndPoint0_(Context ctx) returns http:Response|error {
        ctx.flowVars.userId = ctx.inboundProperties.uriParams["userId"];

        if ctx.flowVars.userId == null {
            ctx.flowVars.userId = "1";
        }

        // http client request
        http:Client User_API_Config = check new ("jsonplaceholder.typicode.com:80");
        http:Response _clientResult0_ = check User_API_Config->/users/\#\[flowVars\.userId\].get();
        ctx.payload = check _clientResult0_.getJsonPayload();

        processUserDataSubflow(ctx);

        // async operation
        _ = start _async0_(ctx);
        ctx.inboundProperties.response.setPayload("");
        return ctx.inboundProperties.response;
    }
}

public function _vmReceive0_(Context ctx) {
}

public function _async0_(Context ctx) {
    worker W returns error? {
        // VM Inbound Endpoint
        anydata receivedPayload = check <- function;
        ctx.payload = receivedPayload;
        _vmReceive0_(ctx);
    }

    // VM Outbound Endpoint
    ctx.payload -> W;
}

public function processUserDataSubflow(Context ctx) {
    // set payload
    string _payload0_ = "Final processed ctx.payload with user info and roles: " + ctx.payload.toString();
    ctx.payload = _payload0_;
    ctx.inboundProperties.response.setPayload(_payload0_);
}
