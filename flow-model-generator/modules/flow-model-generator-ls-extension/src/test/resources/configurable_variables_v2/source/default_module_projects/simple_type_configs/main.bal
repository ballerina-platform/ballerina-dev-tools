configurable int intVar = 42;
configurable float floatVar = 3.5;
configurable string stringVar = "foo";
configurable boolean booleanVar = true;
configurable string[] stringArrayVar = ["bar", "baz"];
configurable Student recordVar1 = {
    name: "John Doe",
    age: 20,
    grade: "A"
};
configurable Student recordVar2 = ?;

public type Student record {
    string name;
    int age;
    string grade;
};

public function main() {

}
