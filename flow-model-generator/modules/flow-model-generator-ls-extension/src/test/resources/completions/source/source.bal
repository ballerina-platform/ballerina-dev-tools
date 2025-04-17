import ballerina/http;

// Module-level variable
int moduleVar = 10;

service / on new http:Listener(8080) {
    // Class-level variable
    private int classVar = 20;

    resource function get test() {
        // Local-level variable
        float localVar = 3.14;

        if (moduleVar > 11) {
            int i = 32;
        } else if (self.classVar > ) {

        }
    }
}

function testCaller(http:Caller caller) {
    
}
