// Following service class is expected to be generated
service class Person {
    private final string name;
    private final int age;
    private final boolean isAdult;
    private final Person[] children;
    private final Person child;

    function init(string name, int age, boolean isAdult, Person[] children, Person child) {
        self.name = name;
        self.age = age;
        self.isAdult = isAdult;
        self.children = children;
        self.child = child;
    }

    resource function get name() returns string {
        return self.name;
    }

    resource function get age() returns int {
        return self.age;
    }

    resource function get isAdult() returns boolean {
        return self.isAdult;
    }

    resource function get children(string|int count) returns Person[] {
        return self.children;
    }

    resource function get child(record {|
                string name;
                int age;
            |} filter) returns Person {
        return self.child;
    }
}