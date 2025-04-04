import ballerina/http;
import ballerinax/ai.agent;

final agent:OpenAiModel _telegramAgentModel = check new ("", "gpt-3.5-turbo-16k-0613");
final agent:Agent _telegramAgentAgent = check new (systemPrompt = {
    role: "",
    instructions: string ``
}, model = _telegramAgentModel, tools = []);

listener agent:Listener telegramAgentListener = new (listenOn = check http:getDefaultListener());

service /telegramAgent on telegramAgentListener {
    resource function post chat(@http:Payload agent:ChatReqMessage request) returns agent:ChatRespMessage|error {

        string stringResult = check _telegramAgentAgent->run(request.message);
        return {message: stringResult};
    }
}
