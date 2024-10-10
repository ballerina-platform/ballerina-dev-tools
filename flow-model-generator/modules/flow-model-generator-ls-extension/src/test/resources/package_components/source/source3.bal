import ballerina/io;

configurable int numberOfDancers = 5;
configurable string performanceName = "Swan Lake";
configurable float ticketPrice = 50.0;
configurable boolean hasIntermission = true;
configurable string[] danceMoves = ["pirouette", "grand jete", "arabesque"];
configurable int maxCapacity = ?;
configurable table<Performance> key(name) performances = table [
    {name: "Swan Lake", year: 1876, composer: "Tchaikovsky"},
    {name: "The Nutcracker", year: 1892, composer: "Tchaikovsky"},
    {name: "Giselle", year: 1841, composer: "Adolphe Adam"}
];
configurable map<string> dancerRoles = {
    "Odette": "Principal female",
    "Siegfried": "Principal male",
    "Rothbart": "Antagonist",
    "Odile": "Secondary female"
};

int value = 32;

function createDanceRoutine(string[] moves) returns string {
    string routine = "";
    foreach var move in moves {
        routine += move + " - ";
    }
    return routine.substring(0, routine.length() - 3);
}

type Performance record {|
    readonly string name;
    int year;
    string composer;
|};

type Dancer record {
    string name;
    int age;
    string specialty;
};

public function main() {
    // Create an array of dancers
    Dancer[] dancers = [];
    foreach int i in 1 ... numberOfDancers {
        dancers.push({
            name: "Dancer " + i.toString(),
            age: 20 + i,
            specialty: danceMoves[i % danceMoves.length()]
        });
    }

    // Calculate total revenue
    float totalRevenue = <float>numberOfDancers * ticketPrice;

    // Create the dance routine
    string routine = createDanceRoutine(danceMoves);

    // Print performance details
    io:println("Performance: ", performanceName);
    io:println("Number of Dancers: ", numberOfDancers);
    io:println("Ticket Price: $", ticketPrice);
    io:println("Has Intermission: ", hasIntermission);
    io:println("Dance Routine: ", routine);
    io:println("Total Revenue: $", totalRevenue);

    // Using the Elvis operator for maxCapacity
    int actualCapacity = maxCapacity ?: 100;
    io:println("Max Capacity: ", actualCapacity);

    // Print dancer details
    io:println("\nDancers:");
    foreach var dancer in dancers {
        io:println(dancer.name, " (Age: ", dancer.age, ", Specialty: ", dancer.specialty, ")");
    }

    // Using the table
    io:println("\nFamous Ballet Performances:");
    foreach var performance in performances {
        io:println(performance.name, " (", performance.year, ") by ", performance.composer);
    }

    // Using the map<string>
    io:println("\nDancer Roles:");
    foreach var [role, description] in dancerRoles.entries() {
        io:println(role, ": ", description);
    }
}
