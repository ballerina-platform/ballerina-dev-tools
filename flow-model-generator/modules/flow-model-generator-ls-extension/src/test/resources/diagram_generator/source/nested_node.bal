import ballerina/http;

final http:Client asiri = check new ("http://localhost:9090");

service /nested on new http:Listener(8080) {
    resource function get path1() returns error? {
        do {
            json j = check asiri->get(path = "/doctors/kandy");
        }
    }

    resource function get path2(boolean isKandy) returns error? {
        transaction {
            if isKandy {
                json j = check asiri->get(path = "/doctors/kandy");
            } else {
                json j = check asiri->get(path = "/doctors/colombo");
            }
            error? unionResult = commit;
        }
    }

    resource function options path() {
        do {

        }
    }

    resource function head path() returns error? {
        do {
            do {
                transaction {
                    json j = check asiri->post(path = "/doctors/kandy", message = "text");
                    check commit;
                }
                json k = check asiri->get(path = "/doctors/kandy");
            }
            json l = check asiri->get(path = "/doctors/kandy");
        }
    }

    resource function put path() returns error? {
        do {
            do {
                transaction {
                    json j = check asiri->post(path = "/doctors/kandy", message = "text");
                    check commit;
                }
                do {
                    json k = check asiri->get(path = "/doctors/kandy");
                }
            }
        }
    }
}
