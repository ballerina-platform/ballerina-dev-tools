// Copyright (c) 2025 WSO2 LLC (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/lang.regexp;
import ballerinax/ai.agent;

isolated function getNumbers(string prompt) returns string[] {
    regexp:Span[] spans = re `-?\d+\.?\d*`.findAll(prompt);
    return spans.'map(span => span.substring());
}

isolated function getAnswer(string prompt) returns string {
    var result = re `.*(Answer is: .*)\n?`.findGroups(prompt);
    if result is () || result.length() <= 1 {
        return "Sorry! I don't know the answer";
    }
    var answer = result[1];
    return answer is () ? "Sorry! I don't know the answer" : answer.substring();
}

isolated function getDecimals(string[] numbers) returns decimal[] {
    decimal[] decimalVals = [];
    foreach var num in numbers {
        decimal|error decimalVal = decimal:fromString(num);
        decimalVals.push(decimalVal is decimal ? decimalVal : 0d);
    }
    return decimalVals;
}

isolated function getInt(string number) returns int {
    int|error intVal = int:fromString(number);
    return intVal is int ? intVal : 0;
}

type MockLlmToolCall record {|
    string action;
    json action_input;
|};

@agent:Tool
isolated function sum(decimal[] numbers) returns string {
    decimal total = 0;
    foreach decimal number in numbers {
        total += number;
    }
    return string `Answer is: ${total}`;
}

@agent:Tool
isolated function mutiply(int a, int b) returns string {
    return string `Answer is: ${a * b}`;
}


configurable string deploymentId = "gpt-4o";
configurable string apiVersion = "2023-08-01-preview";
configurable string serviceUrl = "https://bal-rnd.openai.azure.com/openai";
configurable string apiKey = ?;

agent:DefaultMessageWindowChatMemoryManager def = new ();
agent:AzureOpenAiModel myModel = check new ({auth: {apiKey}}, serviceUrl, deploymentId, apiVersion);

isolated function getChatAssistantMessage(string content) returns agent:ChatAssistantMessage {
    return {role: agent:ASSISTANT, content};
}

// final MockLlm model = new;
final agent:Agent agent = check new (model = myModel,
    systemPrompt = {role: "Math tutor", instructions: "Help the students with their questions."},
    tools = [set, sum, mutiply], agentType = agent:REACT_AGENT);

public function main() returns error? {
    string result = check agent->run("What is the sum of 1 and 2?");

}

# The `Client.get()` function can be used to send HTTP GET requests to HTTP endpoints.
# + path - Request path
# + headers - The entity headers
@agent:Tool
@display {
    iconPath: "/path/icon.png"
}
isolated function set(string key, string value) returns string|error? {
    string item = check redisCl->set(key, value);
    return item;
}
