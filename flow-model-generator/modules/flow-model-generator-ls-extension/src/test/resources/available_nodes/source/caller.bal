import ballerina/http;
import ballerina/log;

service / on new http:Listener(9090) {
    
    // Resource function that handles POST requests to /hello path
    resource function post hello(http:Caller caller, http:Request request) returns error? {
        // Extract payload from the request
        var payload = check request.getJsonPayload();
        
        // Prepare response
        http:Response response = new;
        response.setJsonPayload({
            message: "Hello, received your message!",
            echo: payload
        });
        
        // Respond to the client using the caller
        check caller->respond(response);
        
        log:printInfo("Successfully responded to the client");
    }
    
    // Resource function that demonstrates delayed response
    resource function get delayedResponse(http:Caller caller2) returns error? {
        // Send response after some processing
        http:Response response = new;
        response.setTextPayload("This is a delayed response");
        
        // Respond to the client using the caller
        check caller2->respond(response);
    }
}
