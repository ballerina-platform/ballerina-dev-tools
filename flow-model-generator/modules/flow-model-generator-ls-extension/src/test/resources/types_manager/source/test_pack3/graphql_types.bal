// Following service class is expected to be generated
service class Person {
	private final string name;
	private final int age;
    private final boolean isAdult;
    private final Person[] children;
	function init(string name, int age, boolean isAdult, Person[] children) {
		self.name = name;
		self.age = age;
        self.children = children;
        self.isAdult = isAdult;
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

    resource function get children(int|string count) returns Person[] {
        return self.children;
    }

    resource function get child(record {|int age; string name;|} filter) returns Person {
        return self.children[0];
    }
}