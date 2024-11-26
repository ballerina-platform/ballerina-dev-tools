import ballerina/data.jsondata;
import ballerina/http;

public function main() returns error? {
    string var1 = "John";

    Target var1 = transform({firstName: "John", lastName: "Doe", age: 30});
    Target var2 = transform({firstName: "John", age: 30});
    int var1 = fn(10);
    string var1 = jsondata:prettify({name: "John", age: 30});
}

function fn(int i) returns int {
    return 0;
}
