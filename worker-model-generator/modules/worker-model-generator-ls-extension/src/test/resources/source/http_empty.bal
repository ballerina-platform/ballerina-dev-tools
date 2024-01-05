import ballerina/http;

http:Client clientEp = check new ("http://localhost:9090/");


@display {
    label: "Flow",
    id: "1",
    name: "main/function"
}
public function main() returns http:ClientError? {

    @display {
        label: "Node",
        templateId: "HttpRequestNode",
        xCord: 100,
        yCord: 24
    }
    worker A returns error? {
        json res = check clientEp->/.get();
        res -> function;
    
    }

    json|error val = <- A;
}
