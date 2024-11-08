import ballerina/http;

client class MyClient {
    remote function myRemoteFn() {

    }
}

http:Client moduleCl = check new ("http://localhost:9090", {});
MyClient myModuleCl = new;
MyClient myExplicitModuleCl = new MyClient();
//TODO: Support union types: MyClient|http:Client unionCl = new MyClient();

public function main() returns error? {
    json moduleVal = check moduleCl->get("/hello");
    myModuleCl->myRemoteFn();

    http:Client localCl = check new ("http://localhost:8080", {});
    json localVal = check localCl->get("/hello");
    MyClient myLocalCl = new;
    myLocalCl->myRemoteFn();

    //TODO: Support clients in the object scope
    // var myObj = object {
    //     MyClient myObjCl = new;
    //     http:Client|http:ClientError objCl = new ("http://localhost:5005");
    // };
    // (myObj.myObjCl)->myRemoteFn();
    // json objVal = check (check myObj.objCl)->get("/hello");
}
