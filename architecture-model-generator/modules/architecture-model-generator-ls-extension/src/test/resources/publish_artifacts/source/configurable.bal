// Configurable string variable
configurable string apiUrl = "https://api.example.com";

// Configurable int variable
configurable int maxRetries = ?;

// Configurable boolean variable
configurable boolean enableLogging = ?;

// Configurable float variable
configurable float timeout = 30.5;

// Configurable table array
type User record {
    readonly string name;
    int age;
    string email;
};

configurable table<User> key(name) users = table [
    {name: "John", age: 30, email: "john@example.com"},
    {name: "Jane", age: 25, email: "jane@example.com"}
];

// Configurable json variable
configurable json serverConfig = {
    "host": "localhost",
    "port": 8080,
    "debug": true
};

// Configurable anydata variable
configurable anydata defaultSettings = ?;
