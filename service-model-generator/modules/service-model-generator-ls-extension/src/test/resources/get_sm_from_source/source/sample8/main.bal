import ballerina/http;

listener http:Listener httpListener = new (port = 9090);

service /api on httpListener {

    resource function get response() returns http:Response {
        return new http:Response();
    }

    resource function get intResult() returns int {
        return 200;
    }

    resource function get httpOk() returns http:Ok {
        return http:OK;
    }

    resource function get httpOkWithType() returns HttpOk {
        return {body: 0};
    }

    resource function get anonReturn() returns record {|*http:Ok; int body;|} {
        return {body: 0};
    }

    resource function get allUnion() returns http:Response|int|http:Ok|HttpOk|record {|*http:Ok; int body;|} {
        return new http:Response();

    }

    resource function get noReturn() {
        return;
    }
}

type HttpOk record {|
    *http:Ok;
    int body;
|};

