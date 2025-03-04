import ballerina/sql;
import ballerinax/mysql;

final mysql:Client mysqlClient = check new ();

type Row record {|
    string id;
    int value;
|};

public function main() returns error? {
    stream<Row, sql:Error?> res1 = mysqlClient->query(``);
    stream<record {|string id; int val;|}, sql:Error?> res2 = mysqlClient->query(``);
}
