import ballerina/io;
import ballerina/lang.regexp;
import ballerinax/ai.agent;

configurable string apiKey = ?;
configurable string deploymentId = ?;
configurable string apiVersion = ?;
configurable string serviceUrl = ?;

final agent:Model model = check new agent:AzureOpenAiModel({auth: {apiKey}}, serviceUrl, deploymentId, apiVersion);
final agent:Agent agent = check new (
    systemPrompt = {
        role: "Telegram Assistant",
        instructions: "Assist the users with their requests, whether it's for information, " +
            "tasks, or troubleshooting. Provide clear, helpful responses in a friendly and professional manner."
    },
    model = model,
    tools = [sum, multiply],
    memory = new agent:MessageWindowChatMemory(10) // Available by default
);
