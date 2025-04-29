import ballerina/http;
import ballerinax/ai;

service /p1 on new http:Listener(9091) {
    final ai:OpenAiProvider _AgentServiceModel;
    final ai:Agent _AgentServiceAgent;

    public function init() returns error? {
        self._AgentServiceModel = check new ("", ai:GPT_3_5_TURBO_0613);
        self._AgentServiceAgent = check new (systemPrompt = {role: "", instructions: string ``},
            model = self._AgentServiceModel,
            tools = []
        );
    }

    resource function post chat(@http:Payload ai:ChatReqMessage request) returns ai:ChatRespMessage|error {
        string stringResult = check self._AgentServiceAgent->run(request.message, request.sessionId);
        return {message: stringResult};
    }
}
