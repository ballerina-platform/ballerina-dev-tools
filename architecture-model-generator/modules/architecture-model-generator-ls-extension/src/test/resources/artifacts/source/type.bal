// Basic types
public type MyInt int;
public type MyString string;
public type MyFloat float;
public type MyBoolean boolean;
public type MyByte byte;
public type MyDecimal decimal;

// Union types
public type StringOrInt string|int;
public type Nullable string?; // string|()

// Record types
public type Person record {
    readonly string name;
    int age;
    string[] hobbies;
    boolean married?; // optional field
};

// Open record with rest field
public type OpenRecord record {|
    string id;
    int count;
    anydata...; // rest field can contain any data
|};

// Closed record without rest field
public type ClosedRecord record {|
    string id;
    int count;
|};

// Array types
public type IntArray int[];
public type StringArray string[];
public type Matrix int[][];

// Tuple types
public type Pair [string, int];
public type Triple [string, int, boolean];

// Map types
public type StringMap map<string>;
public type RecordMap map<Person>;

// Object types
public type Student object {
    public string name;
    public int age;
    
    public function getFullDetails() returns string;
};

// Function types
public type StringFunction function (string) returns string;
public type Calculator function (int, int) returns int;

// Error types
public type AppError distinct error;
public type DatabaseError distinct error<record {| string code; string details; |}>;

// Intersection types
public type ReadOnlyPerson readonly & Person;

// Table type
public type PersonTable table<Person>;
public type KeyedPersonTable table<Person> key(name);

// Stream type
public type PersonStream stream<Person>;

// XML types
public type XMLElement xml:Element;
public type XMLText xml<xml:Text>;

// Type inclusion
public type Employee record {|
    *Person;
    string department;
    float salary;
|};

// Constrained types
public type SmallInt int:Signed16;
public type PositiveInt int:Unsigned32;

// Enum type
public enum Color {
    RED,
    GREEN,
    BLUE,
    YELLOW,
    BLACK
}

// Direction enum type
public enum Direction {
    NORTH,
    EAST,
    SOUTH,
    WEST
}

// Service class definition
public service class PersonService {
    private final Person[] people = [];
    
    public function init() {
        // Initialize service
        self.people.push({name: "John", age: 30, hobbies: ["Reading", "Swimming"]});
    }
    
    resource function get people() returns Person[] {
        return self.people;
    }
    
    resource function get person/[string name]() returns Person|error {
        foreach Person p in self.people {
            if p.name == name {
                return p;
            }
        }
        return error("Person not found");
    }
    
    resource function post person(Person newPerson) returns Person|error {
        self.people.push(newPerson);
        return newPerson;
    }
}
