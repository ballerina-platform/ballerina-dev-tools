import ballerina/http;

final http:Client pineValleyEp = check new ("http://localhost:9091/pineValley/");
final http:Client grandOakEp = check new ("http://localhost:9092/grandOak/");

type PineValleyPayload record {
    string doctorType;
};

service / on new http:Listener(9090) {
    resource function get doctor/[string doctorType]() returns json|error? {

        @display {
            label: "Node",
            templateId: "CodeBlockNode",
            xCord: 301,
            yCord: 8
        }
        worker CodeBlockNode_0 returns error? {
        }
    }
}

function name() {

}