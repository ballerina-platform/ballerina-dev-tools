import ballerina/http;

listener http:Listener httpDefaultListener = http:getDefaultListener();

service /home on httpDefaultListener {
    resource function get greeting() returns error|json|http:InternalServerError {
        do {
            int var1 = 10;
            if (var1 == 10) {

            }
            foreach int var2 in [1, 2, 3] {
                foreach int var3 in [1, 2, 3] {
                    if true {

                    }
                }

            }

        } on fail error err {
            // handle error
            return error("unhandled error", err);
        }
    }
}
