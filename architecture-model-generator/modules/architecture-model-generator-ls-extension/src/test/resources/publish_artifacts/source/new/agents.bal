import ballerinax/ai.agent;

final agent:OpenAiModel _telegramAgentModel = check new ("", "gpt-3.5-turbo-16k-0613");
final agent:Agent _telegramAgentAgent = check new (systemPrompt = {
    role: "",
    instructions: string ``
}, model = _telegramAgentModel, tools = []);
    