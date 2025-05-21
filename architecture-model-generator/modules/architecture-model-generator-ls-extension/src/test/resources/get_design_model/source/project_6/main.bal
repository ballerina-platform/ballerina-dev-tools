import ballerina/http;

listener http:Listener httpDefaultListener = http:getDefaultListener();

service /api on httpDefaultListener {
    resource function get .() returns error|json|http:InternalServerError {
        do {
        } on fail error err {
            // handle error
            return error("unhandled error", err);
        }
    }

    resource function get [Country country]/foo() returns error|json {
        do {
        } on fail error err {
            // handle error
            return error("unhandled error", err);
        }
    }

    resource function get [string...]() returns error|json {
        do {
        } on fail error err {
            // handle error
            return error("unhandled error", err);
        }
    }

    resource function get [string]() returns error|json {
        do {
        } on fail error err {
            // handle error
            return error("unhandled error", err);
        }
    }

    resource function get foo/[string... foo]() returns error|json {
        do {
        } on fail error err {
            // handle error
            return error("unhandled error", err);
        }
    }
}
