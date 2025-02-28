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

    resource function head queryAction() {
        int[] nums = [1, 2, 3, 4];
        int[] numsTimes10 = [];

        // The `from` clause works similar to a `foreach` statement.
        check from var i in nums
            // The `do` statement block is evaluated in each iteration.
            do {
                numsTimes10.push(i * 10);
            };

        // Print only the even numbers in the `nums` array.
        // Intermediate clauses such as `let` clause, `join` clause, `order by` clause,
        // `where clause`, and `limit` clause can also be used.
        check from var i in nums
            where i % 2 == 0
            do {
                _ = nums.remove(i);
            };
    }
}
