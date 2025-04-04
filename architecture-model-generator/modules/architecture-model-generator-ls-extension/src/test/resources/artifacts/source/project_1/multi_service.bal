import ballerina/http;

listener http:Listener ls = new http:Listener(9093);

service /api1 on ls {
    
    resource function get path() returns error? {
        check self.baz();
    }

    function baz() returns error? {
        check foo();
    }
}

service /api2 on ls {
    
    resource function get path() returns error? {
        check bar();
    }
}

function bar() returns error? {
    check foo();
}
