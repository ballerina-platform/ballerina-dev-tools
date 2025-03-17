import ballerina/http;

final http:Client tool1Client = check new("http:localhost:8080/tool1");
final http:Client tool2Client = check new("http:localhost:8080/tool2");

final Agent agent1 = check new(tools = [tool1, tool2]);

AgentConfiguration config = {
    tools: [tool1, tool2]
};

final Agent agent2 = check new (config);


service /tool1Service on new http:Listener(8080) {

    resource function post chat(@http:Payload string query) returns string|error? {
        return agent1->run(query);
    }
}

service /tool2Service on new http:Listener(9090) {

    resource function post chat(@http:Payload string query) returns string|error? {
        return agent2->run(query);
    }
}


isolated function tool1() returns string|error? {
    return tool1Client->get("/get/result");
}

isolated function tool2() returns string|error? {
    return tool2Client->get("/get/result");
}

public isolated distinct client class Agent {
    public isolated function init(*AgentConfiguration config) returns error? {
    }

    isolated remote function run(string query) returns string|error {
        return query;
    }
}

# Represents a type alias for an isolated function, representing a function tool.
public type FunctionTool isolated function;

public type AgentConfiguration record {|
    FunctionTool[] tools = [];
|};
